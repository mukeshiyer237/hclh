package com.example.demo.service;

import com.example.demo.controller.dto.ChangeRoleRequest;
import com.example.demo.controller.dto.CreateAdminRequest;
import com.example.demo.controller.dto.UserResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.kafka.producer.KafkaProducerService;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository       userRepository;
    @Mock PasswordEncoder      passwordEncoder;
    @Mock KafkaProducerService kafkaProducerService;

    @InjectMocks UserService userService;

    private User alice;

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Spring would normally bind
        ReflectionTestUtils.setField(userService, "bootstrapSecret", "test-secret");
        ReflectionTestUtils.setField(userService, "usersTopic",       "users-topic");

        alice = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password("encoded")
                .role("USER")
                .build();
        ReflectionTestUtils.setField(alice, "id", 1L);
        ReflectionTestUtils.setField(alice, "createdAt", LocalDateTime.now());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsOnlyActiveUsers() {
        when(userRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(alice));

        List<UserResponse> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("alice");
        assertThat(result.get(0).deletedAt()).isNull();
    }

    @Test
    void findAll_emptyRepository_returnsEmptyList() {
        when(userRepository.findAllByDeletedAtIsNull()).thenReturn(List.of());
        assertThat(userService.findAll()).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existingActiveUser_returnsResponse() {
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(alice));

        UserResponse response = userService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void findById_missingUser_throwsResourceNotFoundException() {
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createAdmin ───────────────────────────────────────────────────────────

    @Test
    void createAdmin_happyPath_returnsAdminUserResponse() {
        CreateAdminRequest req = new CreateAdminRequest("admin", "admin@example.com", "Adm1n!", "test-secret");

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Adm1n!")).thenReturn("encoded");

        User saved = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password("encoded")
                .role("ADMIN")
                .build();
        ReflectionTestUtils.setField(saved, "id", 2L);
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse response = userService.createAdmin(req);

        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.username()).isEqualTo("admin");
        verify(kafkaProducerService).publish(anyString(), anyString(), any());
    }

    @Test
    void createAdmin_bootstrapDisabled_throwsIllegalStateException() {
        ReflectionTestUtils.setField(userService, "bootstrapSecret", "disabled");
        CreateAdminRequest req = new CreateAdminRequest("admin", "admin@example.com", "Adm1n!", "disabled");

        assertThatThrownBy(() -> userService.createAdmin(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createAdmin_wrongSecret_throwsIllegalArgumentException() {
        CreateAdminRequest req = new CreateAdminRequest("admin", "admin@example.com", "Adm1n!", "wrong-secret");

        assertThatThrownBy(() -> userService.createAdmin(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bootstrap secret");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createAdmin_duplicateUsername_throwsDuplicateResourceException() {
        CreateAdminRequest req = new CreateAdminRequest("admin", "admin@example.com", "Adm1n!", "test-secret");
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdmin(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("admin");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createAdmin_duplicateEmail_throwsDuplicateResourceException() {
        CreateAdminRequest req = new CreateAdminRequest("admin", "admin@example.com", "Adm1n!", "test-secret");
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdmin(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("admin@example.com");

        verify(userRepository, never()).save(any());
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_setsDeletedAtAndPublishesEvent() {
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(alice)).thenReturn(alice);

        userService.deleteUser(1L);

        assertThat(alice.getDeletedAt()).isNotNull();
        verify(userRepository).save(alice);
        verify(kafkaProducerService).publish(anyString(), anyString(), any());
    }

    @Test
    void deleteUser_missingUser_throwsResourceNotFoundException() {
        when(userRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(kafkaProducerService);
    }

    // ── changeRole ────────────────────────────────────────────────────────────

    @Test
    void changeRole_validRole_updatesAndReturnsResponse() {
        ChangeRoleRequest req = new ChangeRoleRequest("ADMIN");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(alice)).thenReturn(alice);

        UserResponse response = userService.changeRole(1L, req);

        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(alice.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void changeRole_roleNormalisedToUpperCase() {
        ChangeRoleRequest req = new ChangeRoleRequest("user");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(alice)).thenReturn(alice);

        userService.changeRole(1L, req);

        assertThat(alice.getRole()).isEqualTo("USER");
    }

    @Test
    void changeRole_invalidRole_throwsIllegalArgumentException() {
        // role validation happens before the DB lookup — no stub needed
        ChangeRoleRequest req = new ChangeRoleRequest("SUPERUSER");

        assertThatThrownBy(() -> userService.changeRole(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERUSER");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_missingUser_throwsResourceNotFoundException() {
        ChangeRoleRequest req = new ChangeRoleRequest("ADMIN");
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole(99L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}

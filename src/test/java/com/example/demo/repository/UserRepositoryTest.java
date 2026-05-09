package com.example.demo.repository;

import com.example.demo.AbstractIntegrationTest;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired UserRepository  userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(User.builder()
                .username("alice")
                .email("alice@example.com")
                .password(passwordEncoder.encode("Password1!"))
                .role("USER")
                .build());
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        Optional<User> found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getRole()).isEqualTo("USER");
    }

    @Test
    void findByUsername_unknownUser_returnsEmpty() {
        Optional<User> found = userRepository.findByUsername("nobody");
        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("ghost@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_existingUser_returnsTrue() {
        assertThat(userRepository.existsByUsername("alice")).isTrue();
    }

    @Test
    void existsByUsername_unknownUser_returnsFalse() {
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
    }

    @Test
    void save_persistsAndAssignsId() {
        User bob = userRepository.save(User.builder()
                .username("bob")
                .email("bob@example.com")
                .password(passwordEncoder.encode("Password2!"))
                .role("USER")
                .build());

        assertThat(bob.getId()).isNotNull();
        assertThat(userRepository.findById(bob.getId())).isPresent();
    }

    @Test
    void delete_removesUser() {
        userRepository.delete(alice);
        assertThat(userRepository.findByUsername("alice")).isEmpty();
    }

    // ── soft-delete query methods ──────────────────────────────────────────────

    @Test
    void findAllByDeletedAtIsNull_excludesSoftDeletedUsers() {
        // soft-delete alice
        alice.setDeletedAt(LocalDateTime.now());
        userRepository.save(alice);

        List<User> active = userRepository.findAllByDeletedAtIsNull();

        assertThat(active).extracting(User::getUsername).doesNotContain("alice");
    }

    @Test
    void findAllByDeletedAtIsNull_includesActiveUsers() {
        List<User> active = userRepository.findAllByDeletedAtIsNull();

        assertThat(active).extracting(User::getUsername).contains("alice");
    }

    @Test
    void findByIdAndDeletedAtIsNull_activeUser_returnsPresent() {
        Optional<User> found = userRepository.findByIdAndDeletedAtIsNull(alice.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByIdAndDeletedAtIsNull_softDeletedUser_returnsEmpty() {
        alice.setDeletedAt(LocalDateTime.now());
        userRepository.save(alice);

        Optional<User> found = userRepository.findByIdAndDeletedAtIsNull(alice.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void findByIdAndDeletedAtIsNull_unknownId_returnsEmpty() {
        Optional<User> found = userRepository.findByIdAndDeletedAtIsNull(999L);
        assertThat(found).isEmpty();
    }
}

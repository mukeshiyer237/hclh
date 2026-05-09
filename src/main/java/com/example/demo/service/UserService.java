package com.example.demo.service;

import com.example.demo.controller.dto.ChangeRoleRequest;
import com.example.demo.controller.dto.CreateAdminRequest;
import com.example.demo.controller.dto.UserResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.kafka.event.UserCreatedEvent;
import com.example.demo.kafka.event.UserDeletedEvent;
import com.example.demo.kafka.producer.KafkaProducerService;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("USER", "ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducerService kafkaProducerService;

    @Value("${admin.bootstrap-secret}")
    private String bootstrapSecret;

    @Value("${kafka.topics.users}")
    private String usersTopic;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    @Transactional
    public UserResponse createAdmin(CreateAdminRequest request) {
        if ("disabled".equals(bootstrapSecret)) {
            throw new IllegalStateException("Admin bootstrap is disabled on this environment");
        }
        if (!bootstrapSecret.equals(request.bootstrapSecret())) {
            throw new IllegalArgumentException("Invalid bootstrap secret");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("username", request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("email", request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role("ADMIN")
                .build();

        User saved = userRepository.save(user);

        kafkaProducerService.publish(usersTopic, saved.getId().toString(),
                new UserCreatedEvent(saved.getId(), saved.getUsername(),
                        saved.getEmail(), saved.getCreatedAt()));

        return toResponse(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        kafkaProducerService.publish(usersTopic, user.getId().toString(),
                new UserDeletedEvent(user.getId(), user.getUsername(), user.getDeletedAt()));
    }

    @Transactional
    public UserResponse changeRole(Long id, ChangeRoleRequest request) {
        String role = request.role().toUpperCase();

        if (!VALID_ROLES.contains(role)) {
            throw new IllegalArgumentException(
                    String.format("Invalid role: '%s'. Allowed values: %s", role, VALID_ROLES)
            );
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setRole(role);
        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }
}
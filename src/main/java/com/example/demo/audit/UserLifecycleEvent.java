package com.example.demo.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_lifecycle_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** USER_CREATED or USER_DELETED */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Raw JSON payload as received from Kafka */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** When the event actually occurred (from the event body) */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** When this row was written to the DB */
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onInsert() {
        this.recordedAt = LocalDateTime.now();
    }
}

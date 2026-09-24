package com.example.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs", indexes = {
        @Index(name = "idx_logs_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_logs_action", columnList = "action")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private LogEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** Cached display name at time of change. */
    @Column(name = "entity_name", nullable = false, length = 200)
    private String entityName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogAction action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
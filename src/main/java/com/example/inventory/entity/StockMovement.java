package com.example.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_movements_item", columnList = "item_id"),
        @Index(name = "idx_movements_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    /** Always positive. Direction is determined by {@link #type}. */
    @Column(nullable = false)
    private int quantity;

    /** Quantity of the item before this movement. */
    @Column(name = "previous_quantity", nullable = false)
    private int previousQuantity;

    /** Quantity of the item after this movement. */
    @Column(name = "new_quantity", nullable = false)
    private int newQuantity;

    @Column(length = 255)
    private String reason;

    @Column(length = 100)
    private String reference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
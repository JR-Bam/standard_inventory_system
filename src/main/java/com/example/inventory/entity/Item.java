package com.example.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "items", indexes = {
        @Index(name = "idx_items_sku", columnList = "sku", unique = true),
        @Index(name = "idx_items_active", columnList = "active"),
        @Index(name = "idx_items_category", columnList = "category_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public boolean isLowStock() {
        return active && quantity > 0 && quantity <= reorderLevel;
    }

    @Transient
    public boolean isOutOfStock() {
        return active && quantity == 0;
    }

    @Transient
    public java.math.BigDecimal getTotalValue() {
        if (unitPrice == null || quantity == 0) {
            return java.math.BigDecimal.ZERO;
        }
        return unitPrice.multiply(java.math.BigDecimal.valueOf(quantity));
    }

    @Transient
    public String getStockStatus() {
        if (!active) return "Inactive";
        if (quantity == 0) return "Out of stock";
        if (quantity <= reorderLevel) return "Low stock";
        return "In stock";
    }

    @Transient
    public String getStockBadgeClass() {
        if (!active) return "bg-secondary";
        if (quantity == 0) return "bg-dark";
        if (quantity <= reorderLevel) return "bg-danger";
        return "bg-success";
    }
}
package com.example.inventory.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ItemForm {

    private Long id;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must be 50 characters or fewer")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$",
            message = "SKU may only contain letters, numbers, hyphens, and underscores")
    private String sku;

    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Name must be 150 characters or fewer")
    private String name;

    @Size(max = 500, message = "Description must be 500 characters or fewer")
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    /**
     * Only used on create. On edit, the field is not rendered and
     * the value is ignored by the service — quantity changes go
     * through the movements module.
     */
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Unit price format is invalid")
    private BigDecimal unitPrice;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @Size(max = 100, message = "Location must be 100 characters or fewer")
    private String location;

    private boolean active = true;
}
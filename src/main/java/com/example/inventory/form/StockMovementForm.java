package com.example.inventory.form;

import com.example.inventory.entity.MovementType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StockMovementForm {

    @NotNull(message = "Movement type is required")
    private MovementType type;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 255, message = "Reason must be 255 characters or fewer")
    private String reason;

    @Size(max = 100, message = "Reference must be 100 characters or fewer")
    private String reference;
}
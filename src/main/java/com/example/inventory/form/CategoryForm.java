package com.example.inventory.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoryForm {

    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;

    @Size(max = 255, message = "Description must be 255 characters or fewer")
    private String description;

    private boolean active = true;
}
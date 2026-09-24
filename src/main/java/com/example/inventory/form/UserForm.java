package com.example.inventory.form;

import com.example.inventory.entity.Role;
import jakarta.validation.constraints.*;

public class UserForm {

    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "Username may only contain letters, numbers, dot, underscore, hyphen")
    private String username;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be 100 characters or fewer")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 100, message = "Email must be 100 characters or fewer")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    private boolean enabled = true;

    /** Only required on create. Blank on edit means "leave password unchanged". */
    @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
    private String password;

    /** Only set to true via the "Reset password" action. */
    private boolean forcePasswordChange;

    // ----- getters / setters -----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }
}
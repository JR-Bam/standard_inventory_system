package com.example.inventory.service;

import com.example.inventory.entity.Role;
import com.example.inventory.entity.User;
import com.example.inventory.form.ChangePasswordForm;
import com.example.inventory.form.UserForm;
import com.example.inventory.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InventoryLogService logService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       InventoryLogService logService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
    }

    // ---------- Read ----------

    @Transactional(readOnly = true)
    public Page<User> list(String search, Role role, Boolean enabled, Pageable pageable) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return userRepository.search(normalized, role, enabled, pageable);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<User> allActive() {
        return userRepository.findAllByEnabledTrueOrderByUsernameAsc();
    }

    // ---------- Create ----------

    @Transactional
    public User create(UserForm form, String actorUsername) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a user");
        }

        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().trim());
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        // New users must change their password on first login unless the admin
        // explicitly unchecked "Require password change".
        user.setMustChangePassword(true);
        // The line above is intentionally always true on create: new accounts
        // always start with mustChangePassword = true. Admins can clear it
        // later by editing the user.

        User saved = userRepository.save(user);
        logService.logUserCreated(user, actorUsername);
        return saved;
    }

    // ---------- Update ----------

    @Transactional
    public User update(Long id, UserForm form, String actorUsername) {
        User user = getById(id);

        if (!user.getUsername().equals(form.getUsername())
                && userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        boolean wasEnabled = user.isEnabled();
        Role previousRole = user.getRole();

        // Safety: cannot demote the last admin
        if (previousRole == Role.ADMIN
                && form.getRole() != Role.ADMIN
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Cannot demote the last active admin");
        }

        // Safety: cannot disable the last admin
        if (wasEnabled
                && !form.isEnabled()
                && previousRole == Role.ADMIN
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Cannot disable the last active admin");
        }

        user.setUsername(form.getUsername().trim());
        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().trim());
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());

        // Password only changes if a new one is provided
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
            user.setMustChangePassword(form.isForcePasswordChange());
        }

        User saved = userRepository.save(user);
        logService.logUserUpdated(user, actorUsername);
        return saved;
    }

    // ---------- Disable / Enable ----------

    @Transactional
    public void setEnabled(Long id, boolean enabled, String actorUsername) {
        User user = getById(id);

        if (!enabled && user.getRole() == Role.ADMIN
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Cannot disable the last active admin");
        }

        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        logService.logUserEnabled(saved, true, actorUsername);
    }

    // ---------- Change own password ----------

    @Transactional
    public void changeOwnPassword(String username, ChangePasswordForm form) {
        User user = getByUsername(username);

        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }

        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
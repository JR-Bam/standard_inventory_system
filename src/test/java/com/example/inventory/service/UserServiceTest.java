package com.example.inventory.service;

import com.example.inventory.entity.Role;
import com.example.inventory.entity.User;
import com.example.inventory.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private InventoryLogService logService;

    @InjectMocks private UserService userService;

    @Test
    void disable_rejectsLastActiveAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.setEnabled(1L, false, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last active admin");

        verify(userRepository, never()).save(any());
    }

    @Test
    void disable_allowsWhenOtherAdminsExist() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(2L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.setEnabled(1L, false, "admin");

        assertThat(admin.isEnabled()).isFalse();
    }

    @Test
    void update_rejectsDemotingLastAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

        var form = new com.example.inventory.form.UserForm();
        form.setId(1L);
        form.setUsername("admin");
        form.setRole(Role.STAFF);
        form.setEnabled(true);
        form.setFullName("Admin");
        form.setEmail("a@example.com");

        assertThatThrownBy(() -> userService.update(1L, form, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last active admin");
    }
}
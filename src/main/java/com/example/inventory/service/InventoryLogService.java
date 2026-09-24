package com.example.inventory.service;

import com.example.inventory.entity.*;
import com.example.inventory.repository.InventoryLogRepository;
import com.example.inventory.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryLogService {

    private final InventoryLogRepository logRepository;
    private final UserRepository userRepository;

    public InventoryLogService(InventoryLogRepository logRepository,
                               UserRepository userRepository) {
        this.logRepository = logRepository;
        this.userRepository = userRepository;
    }

    /**
     * Record a log entry. Called from within the same transaction as the
     * entity mutation it describes.
     */
    @Transactional
    public void log(LogEntityType entityType,
                    Long entityId,
                    String entityName,
                    LogAction action,
                    String message,
                    String username) {
        if (username == null) {
            // No authenticated user — likely a system action or test.
            // Skip logging rather than throwing, since this shouldn't break the caller.
            return;
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }
        InventoryLog entry = InventoryLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName == null ? "—" : truncate(entityName, 200))
                .action(action)
                .user(user)
                .message(truncate(message, 500))
                .build();
        logRepository.save(entry);
    }

    // Convenience helpers -------------------------------------------------

    public void logItemCreated(Item item, String username) {
        log(LogEntityType.ITEM, item.getId(), item.getName(),
                LogAction.CREATE,
                "Created item '" + item.getName() + "'", username);
    }

    public void logItemUpdated(Item item, String username) {
        log(LogEntityType.ITEM, item.getId(), item.getName(),
                LogAction.UPDATE,
                "Updated item '" + item.getName() + "'", username);
    }

    public void logItemDeleted(Item item, String username) {
        log(LogEntityType.ITEM, item.getId(), item.getName(),
                LogAction.DELETE,
                "Soft-deleted item '" + item.getName() + "'", username);
    }

    public void logItemRestored(Item item, String username) {
        log(LogEntityType.ITEM, item.getId(), item.getName(),
                LogAction.RESTORE,
                "Restored item '" + item.getName() + "'", username);
    }

    public void logCategoryCreated(Category c, String username) {
        log(LogEntityType.CATEGORY, c.getId(), c.getName(),
                LogAction.CREATE,
                "Created category '" + c.getName() + "'", username);
    }

    public void logCategoryUpdated(Category c, String username) {
        log(LogEntityType.CATEGORY, c.getId(), c.getName(),
                LogAction.UPDATE,
                "Updated category '" + c.getName() + "'", username);
    }

    public void logCategoryDeleted(Category c, String username) {
        log(LogEntityType.CATEGORY, c.getId(), c.getName(),
                LogAction.DELETE,
                "Soft-deleted category '" + c.getName() + "'", username);
    }

    // Read-side -----------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<InventoryLog> search(LogEntityType entityType,
                                     LogAction action,
                                     Long userId,
                                     LocalDateTime from,
                                     LocalDateTime to,
                                     String search,
                                     Pageable pageable) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return logRepository.search(entityType, action, userId, from, to, normalized, pageable);
    }

    @Transactional(readOnly = true)
    public List<InventoryLog> recent(int limit) {
        return logRepository.findRecent(Pageable.ofSize(limit));
    }

    @Transactional(readOnly = true)
    public List<Object[]> distinctUsers() {
        return logRepository.findDistinctUsers();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    public void logUserCreated(User user, String actorUsername) {
        log(LogEntityType.USER, user.getId(), user.getUsername(),
                LogAction.CREATE,
                "Created user '" + user.getUsername() + "'", actorUsername);
    }

    public void logUserUpdated(User user, String actorUsername) {
        log(LogEntityType.USER, user.getId(), user.getUsername(),
                LogAction.UPDATE,
                "Updated user '" + user.getUsername() + "'", actorUsername);
    }

    public void logUserEnabled(User user, boolean enabled, String actorUsername) {
        log(LogEntityType.USER, user.getId(), user.getUsername(),
                enabled ? LogAction.RESTORE : LogAction.DELETE,
                (enabled ? "Enabled" : "Disabled") + " user '" + user.getUsername() + "'",
                actorUsername);
    }
}
package com.example.inventory.repository;

import com.example.inventory.entity.InventoryLog;
import com.example.inventory.entity.LogAction;
import com.example.inventory.entity.LogEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    @Query("""
        SELECT l FROM InventoryLog l
        JOIN FETCH l.user
        WHERE (:entityType IS NULL OR l.entityType = :entityType)
          AND (:action IS NULL OR l.action = :action)
          AND (:userId IS NULL OR l.user.id = :userId)
          AND (:from IS NULL OR l.createdAt >= :from)
          AND (:to IS NULL OR l.createdAt <= :to)
          AND (:search IS NULL OR :search = ''
               OR LOWER(l.entityName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(l.message)    LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY l.createdAt DESC, l.id DESC
        """)
    Page<InventoryLog> search(@Param("entityType") LogEntityType entityType,
                              @Param("action") LogAction action,
                              @Param("userId") Long userId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("search") String search,
                              Pageable pageable);

    @Query("""
        SELECT l FROM InventoryLog l
        JOIN FETCH l.user
        ORDER BY l.createdAt DESC, l.id DESC
        """)
    List<InventoryLog> findRecent(Pageable pageable);

    @Query("""
        SELECT DISTINCT l.user.id, l.user.username
        FROM InventoryLog l
        ORDER BY l.user.username
        """)
    List<Object[]> findDistinctUsers();
}
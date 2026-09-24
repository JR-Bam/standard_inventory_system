package com.example.inventory.repository;

import com.example.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("""
        SELECT m FROM StockMovement m
        JOIN FETCH m.user
        WHERE m.item.id = :itemId
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    Page<StockMovement> findByItemIdWithUser(@Param("itemId") Long itemId, Pageable pageable);

    @Query("""
        SELECT m FROM StockMovement m
        JOIN FETCH m.item i
        JOIN FETCH m.user
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    List<StockMovement> findRecent(Pageable pageable);

    long countByItemId(Long itemId);

    @Query("""
        SELECT FUNCTION('DATE', m.createdAt), m.type, SUM(m.quantity)
        FROM StockMovement m
        WHERE m.createdAt >= :since
        GROUP BY FUNCTION('DATE', m.createdAt), m.type
        ORDER BY FUNCTION('DATE', m.createdAt)
    """)
    List<Object[]> dailyMovementsSince(@Param("since") LocalDateTime since);
}
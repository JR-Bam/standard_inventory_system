package com.example.inventory.repository;

import com.example.inventory.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    long countByActiveTrue();

    long countByCategoryIdAndActiveTrue(Long categoryId);

    @Query("""
        SELECT i FROM Item i
        JOIN FETCH i.category c
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(i.name)        LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(i.sku)         LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:categoryId IS NULL OR c.id = :categoryId)
          AND (:active IS NULL OR i.active = :active)
          AND (:lowStock IS NULL OR :lowStock = FALSE
               OR (i.quantity > 0 AND i.quantity <= i.reorderLevel))
          AND (:outOfStock IS NULL OR :outOfStock = FALSE
               OR i.quantity = 0)
        """)
    Page<Item> search(@Param("search") String search,
                      @Param("categoryId") Long categoryId,
                      @Param("active") Boolean active,
                      @Param("lowStock") Boolean lowStock,
                      @Param("outOfStock") Boolean outOfStock,
                      Pageable pageable);

    @Query("""
        SELECT i FROM Item i
        JOIN FETCH i.category
        WHERE i.active = TRUE AND i.quantity <= i.reorderLevel
        ORDER BY i.quantity ASC, i.name ASC
        """)
    List<Item> findLowStockItems();

    @Query("""
        SELECT i FROM Item i
        JOIN FETCH i.category
        WHERE i.active = TRUE AND i.quantity = 0
        ORDER BY i.name ASC
        """)
    List<Item> findOutOfStockItems();

    @Query("""
        SELECT i FROM Item i
        JOIN FETCH i.category
        LEFT JOIN FETCH i.createdBy
        WHERE i.id = :id
        """)
    Optional<Item> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT c.name, COUNT(i.id)
        FROM Category c
        LEFT JOIN Item i ON i.category = c AND i.active = TRUE
        WHERE c.active = TRUE
        GROUP BY c.id, c.name
        ORDER BY c.name
    """)
    List<Object[]> categoryItemCounts();

    @Query("""
    SELECT c.name, COALESCE(SUM(i.quantity * i.unitPrice), 0)
    FROM Category c
    LEFT JOIN Item i ON i.category = c AND i.active = TRUE
    WHERE c.active = TRUE
    GROUP BY c.id, c.name
    ORDER BY c.name
    """)
    List<Object[]> categoryItemValues();
}
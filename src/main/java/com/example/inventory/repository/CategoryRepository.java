package com.example.inventory.repository;

import com.example.inventory.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT c FROM Category c
        WHERE (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:active IS NULL OR c.active = :active)
        """)
    Page<Category> search(@Param("search") String search,
                          @Param("active") Boolean active,
                          Pageable pageable);

    List<Category> findAllByActiveTrueOrderByNameAsc();
}
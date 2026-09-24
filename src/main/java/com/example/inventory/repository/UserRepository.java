package com.example.inventory.repository;

import com.example.inventory.entity.Role;
import com.example.inventory.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("""
    SELECT u FROM User u
    WHERE (:search IS NULL
           OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))
      AND (:role IS NULL OR u.role = :role)
      AND (:enabled IS NULL OR u.enabled = :enabled)
    ORDER BY u.username ASC
    """)
    Page<User> search(@Param("search") String search,
                      @Param("role") Role role,
                      @Param("enabled") Boolean enabled,
                      Pageable pageable);

    List<User> findAllByEnabledTrueOrderByUsernameAsc();

    long countByRoleAndEnabledTrue(Role role);
}

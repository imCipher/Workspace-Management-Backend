package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username OR (u.email IS NOT NULL AND u.email = :email)")
    boolean existsByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByRole(Role role);

    Page<User> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(u.fullname) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
            OR (u.email IS NOT NULL AND LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
          )
        ORDER BY u.createdAt DESC
    """)
    Page<User> searchMembers(
            @Param("role") Role role,
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}

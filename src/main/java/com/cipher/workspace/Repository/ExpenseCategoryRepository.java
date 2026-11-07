package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.ExpenseCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategoryEntity, UUID> {

    List<ExpenseCategoryEntity> findAllByOrderByNameAsc();

    Optional<ExpenseCategoryEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}

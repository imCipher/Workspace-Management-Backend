package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.ServiceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceTypeEntity, UUID> {

    List<ServiceTypeEntity> findAllByOrderByNameAsc();

    Optional<ServiceTypeEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}

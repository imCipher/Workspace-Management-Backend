package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.PaymentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTypeRepository extends JpaRepository<PaymentTypeEntity, UUID> {

    List<PaymentTypeEntity> findAllByOrderByNameAsc();

    Optional<PaymentTypeEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}

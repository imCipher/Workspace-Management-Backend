package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.Customers;
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
public interface CustomerRepository extends JpaRepository<Customers, UUID> {

    Optional<Customers> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    Optional<Customers> findByFullnameIgnoreCase(String fullname);

    List<Customers> findByFullnameContainingIgnoreCase(String fullname);

    @Query("""
        SELECT c FROM Customers c
        WHERE :search IS NULL OR :search = ''
           OR LOWER(c.fullname) LIKE LOWER(CONCAT('%', :search, '%'))
           OR c.phoneNumber LIKE CONCAT('%', :search, '%')
    """)
    Page<Customers> searchByNameOrPhone(@Param("search") String search, Pageable pageable);

    @Query("""
        SELECT c FROM Customers c
        WHERE :search IS NULL OR :search = ''
           OR LOWER(c.fullname) LIKE LOWER(CONCAT('%', :search, '%'))
           OR c.phoneNumber LIKE CONCAT('%', :search, '%')
    """)
    List<Customers> searchByNameOrPhone(@Param("search") String search);

    Page<Customers> findByFullnameContainingIgnoreCaseOrPhoneNumberContaining(
            String fullname,
            String phoneNumber,
            Pageable pageable
    );

    Page<Customers> findAllByOrderByTotalVisitsDesc(Pageable pageable);

    List<Customers> findAllByOrderByTotalVisitsDesc();

    Page<Customers> findByTotalVisitsGreaterThanEqualOrderByTotalVisitsDesc(Integer threshold, Pageable pageable);

    List<Customers> findByTotalVisitsGreaterThanEqualOrderByTotalVisitsDesc(Integer threshold);

    long countByTotalVisitsGreaterThanEqual(Integer threshold);

    long countByTotalVisitsLessThan(Integer threshold);
}

package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.ExpenseCategory;
import com.cipher.workspace.Entity.Expenses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpensesRepository extends JpaRepository<Expenses, UUID> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expenses e")
    BigDecimal sumAllAmount();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expenses e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT e.category, COALESCE(SUM(e.amount), 0)
        FROM Expenses e
        WHERE e.expenseDate BETWEEN :startDate AND :endDate
        GROUP BY e.category
    """)
    List<Object[]> sumAmountByCategoryBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Expenses> findByExpenseDateBetweenOrderByExpenseDateAsc(LocalDate startDate, LocalDate endDate);

    List<Expenses> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate startDate, LocalDate endDate);

    long countByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

    List<Expenses> findByCategoryOrderByExpenseDateDesc(String category);

    Page<Expenses> findByCategoryOrderByExpenseDateDesc(String category, Pageable pageable);

    default List<Expenses> findByCategoryOrderByExpenseDateDesc(ExpenseCategory category) {
        return findByCategoryOrderByExpenseDateDesc(category != null ? category.name() : null);
    }

    default Page<Expenses> findByCategoryOrderByExpenseDateDesc(ExpenseCategory category, Pageable pageable) {
        return findByCategoryOrderByExpenseDateDesc(category != null ? category.name() : null, pageable);
    }

    boolean existsByAdminId(UUID adminId);

    List<Expenses> findByAdminIdOrderByExpenseDateDesc(UUID adminId);

    Page<Expenses> findByAdminIdOrderByExpenseDateDesc(UUID adminId, Pageable pageable);

    Page<Expenses> findAllByOrderByExpenseDateDesc(Pageable pageable);
}

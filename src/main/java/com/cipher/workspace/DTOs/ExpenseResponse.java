package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.ExpenseCategory;
import com.cipher.workspace.Entity.Expenses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID adminId,
        String adminName,
        String category,
        BigDecimal amount,
        LocalDate expenseDate,
        String description,
        LocalDateTime createdAt
) {
    public ExpenseResponse(UUID id, UUID adminId, String adminName, ExpenseCategory category,
                           BigDecimal amount, LocalDate expenseDate, String description, LocalDateTime createdAt) {
        this(id, adminId, adminName, category != null ? category.name() : null, amount, expenseDate, description, createdAt);
    }

    public static ExpenseResponse from(Expenses expense) {
        if (expense == null) {
            return null;
        }
        return new ExpenseResponse(
                expense.getId(),
                expense.getAdmin() != null ? expense.getAdmin().getId() : null,
                expense.getAdmin() != null ? expense.getAdmin().getFullname() : null,
                expense.getCategory(),
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getDescription(),
                expense.getCreatedAt()
        );
    }
}

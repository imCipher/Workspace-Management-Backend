package com.cipher.workspace.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "expenses",
        indexes = {
                @Index(name = "idx_expenses_admin_id", columnList = "admin_id"),
                @Index(name = "idx_expenses_expense_date", columnList = "expense_date"),
                @Index(name = "idx_expenses_category", columnList = "category")
        }
)
public class Expenses {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private User admin;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void setCategory(ExpenseCategory category) {
        this.category = category != null ? category.name() : null;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim().toUpperCase().replace(' ', '_') : null;
    }
}

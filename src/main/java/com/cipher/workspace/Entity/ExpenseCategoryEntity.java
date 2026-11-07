package com.cipher.workspace.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "expense_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_expense_categories_name", columnNames = {"name"})
        }
)
public class ExpenseCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ExpenseCategoryEntity(String name, String displayName, boolean isDefault) {
        this.name = name;
        this.displayName = displayName;
        this.isDefault = isDefault;
    }
}

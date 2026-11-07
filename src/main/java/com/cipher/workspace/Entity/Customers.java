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
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_customers_phone_number", columnNames = {"phone_number"})
        }
)
public class Customers {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fullname;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "total_visits", nullable = false)
    private Integer totalVisits = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

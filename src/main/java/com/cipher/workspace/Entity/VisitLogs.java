package com.cipher.workspace.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "visit_logs",
        indexes = {
                @Index(name = "idx_visit_logs_customer_id", columnList = "customer_id"),
                @Index(name = "idx_visit_logs_user_id", columnList = "user_id"),
                @Index(name = "idx_visit_logs_check_in_time", columnList = "check_in_time"),
                @Index(name = "idx_visit_logs_service_type", columnList = "service_type"),
                @Index(name = "idx_visit_logs_payment_type", columnList = "payment_type")
        }
)
public class VisitLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Customers customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_out_by_user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User checkedOutBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User updatedBy;

    @Column(name = "space_assigned")
    private String spaceAssigned;

    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(name = "payment_type", nullable = false, length = 100)
    private String paymentType;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Builder.Default
    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType != null ? serviceType.name() : null;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType != null ? serviceType.trim().toUpperCase().replace(' ', '_') : null;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType != null ? paymentType.name() : null;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType != null ? paymentType.trim().toUpperCase().replace(' ', '_') : null;
    }

    public Long getDurationInMinutes() {
        if (checkInTime == null || checkOutTime == null) {
            return null;
        }
        return Duration.between(checkInTime, checkOutTime).toMinutes();
    }
}

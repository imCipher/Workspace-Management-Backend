package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.PaymentType;
import com.cipher.workspace.Entity.ServiceType;
import com.cipher.workspace.Entity.VisitLogs;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String phoneNumber;
    private String spaceAssigned;
    private String serviceType;
    private String paymentType;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private BigDecimal amountPaid;
    private Long durationInMinutes;

    // Staff accountability fields
    private UUID staffId;
    private String staffName;
    private UUID checkedInById;
    private String checkedInByName;
    private UUID checkedOutById;
    private String checkedOutByName;
    private UUID updatedById;
    private String updatedByName;

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType != null ? serviceType.name() : null;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType != null ? paymentType.name() : null;
    }

    public static VisitResponse from(VisitLogs visitLog) {
        if (visitLog == null) {
            return null;
        }

        Long duration = null;
        if (visitLog.getCheckInTime() != null && visitLog.getCheckOutTime() != null) {
            duration = Duration.between(visitLog.getCheckInTime(), visitLog.getCheckOutTime()).toMinutes();
        }

        VisitResponseBuilder builder = VisitResponse.builder()
                .id(visitLog.getId())
                .customerId(visitLog.getCustomer() != null ? visitLog.getCustomer().getId() : null)
                .customerName(visitLog.getCustomer() != null ? visitLog.getCustomer().getFullname() : null)
                .phoneNumber(visitLog.getCustomer() != null ? visitLog.getCustomer().getPhoneNumber() : null)
                .spaceAssigned(visitLog.getSpaceAssigned())
                .serviceType(visitLog.getServiceType())
                .paymentType(visitLog.getPaymentType())
                .checkInTime(visitLog.getCheckInTime())
                .checkOutTime(visitLog.getCheckOutTime())
                .amountPaid(visitLog.getAmountPaid())
                .durationInMinutes(duration);

        if (visitLog.getUser() != null) {
            builder.staffId(visitLog.getUser().getId())
                    .staffName(visitLog.getUser().getFullname())
                    .checkedInById(visitLog.getUser().getId())
                    .checkedInByName(visitLog.getUser().getFullname());
        }

        if (visitLog.getCheckedOutBy() != null) {
            builder.checkedOutById(visitLog.getCheckedOutBy().getId())
                    .checkedOutByName(visitLog.getCheckedOutBy().getFullname());
        }

        if (visitLog.getUpdatedBy() != null) {
            builder.updatedById(visitLog.getUpdatedBy().getId())
                    .updatedByName(visitLog.getUpdatedBy().getFullname());
        }

        return builder.build();
    }
}

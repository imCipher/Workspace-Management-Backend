package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.PaymentType;
import com.cipher.workspace.Entity.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {

    private UUID customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String phoneNumber;

    @NotBlank(message = "Space assigned is required")
    private String spaceAssigned;

    @NotBlank(message = "Service type is required")
    private String serviceType;

    @NotBlank(message = "Payment type is required")
    private String paymentType;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount paid must be 0 or greater")
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType != null ? serviceType.name() : null;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType != null ? paymentType.name() : null;
    }
}

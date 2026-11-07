package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.Customers;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullname,
        String phoneNumber,
        Integer totalVisits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerResponse from(Customers customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getFullname(),
                customer.getPhoneNumber(),
                customer.getTotalVisits() != null ? customer.getTotalVisits() : 0,
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}

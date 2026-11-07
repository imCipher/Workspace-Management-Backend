package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.Customers;
import com.cipher.workspace.Entity.VisitLogs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerDetailResponse(
        UUID id,
        String fullname,
        String phoneNumber,
        Integer totalVisits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<VisitResponse> visits
) {
    public static CustomerDetailResponse of(Customers customer, List<VisitLogs> visitLogs) {
        if (customer == null) {
            return null;
        }
        List<VisitResponse> visitList = visitLogs != null
                ? visitLogs.stream().map(VisitResponse::from).toList()
                : List.of();

        return new CustomerDetailResponse(
                customer.getId(),
                customer.getFullname(),
                customer.getPhoneNumber(),
                customer.getTotalVisits() != null ? customer.getTotalVisits() : 0,
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                visitList
        );
    }
}

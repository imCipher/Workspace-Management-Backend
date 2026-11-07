package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.CustomerAnalyticsResponse;
import com.cipher.workspace.DTOs.CustomerVisitAnalyticsDto;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Entity.Customers;
import com.cipher.workspace.Entity.VisitLogs;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.CustomerRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAnalyticsServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VisitLogsRepository visitLogsRepository;

    private CustomerAnalyticsService customerAnalyticsService;

    @BeforeEach
    void setUp() {
        customerAnalyticsService = new CustomerAnalyticsService(customerRepository, visitLogsRepository, 5);
    }

    @Test
    void getCustomerAnalytics_tagsRegularsBasedOnMonthlyCheckIn() {
        UUID c1Id = UUID.randomUUID();
        Customers c1 = Customers.builder()
                .id(c1Id)
                .fullname("Frequent Visitor")
                .phoneNumber("08011111111")
                .totalVisits(8)
                .build();

        UUID c2Id = UUID.randomUUID();
        Customers c2 = Customers.builder()
                .id(c2Id)
                .fullname("Occasional Visitor")
                .phoneNumber("08022222222")
                .totalVisits(10)
                .build();

        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

        VisitLogs v1 = VisitLogs.builder()
                .customer(c1)
                .amountPaid(new BigDecimal("5000"))
                .checkInTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .serviceType("SHARED_WORKSPACE")
                .build();

        VisitLogs v2 = VisitLogs.builder()
                .customer(c1)
                .amountPaid(new BigDecimal("7000"))
                .checkInTime(LocalDateTime.of(2026, 8, 20, 10, 0))
                .serviceType("SHARED_WORKSPACE")
                .build();

        VisitLogs v3 = VisitLogs.builder()
                .customer(c2)
                .amountPaid(new BigDecimal("3000"))
                .checkInTime(LocalDateTime.of(2026, 8, 15, 14, 0))
                .serviceType("DEDICATED_DESK")
                .build();

        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of(v1, v2, v3));

        when(visitLogsRepository.findByCustomerIdOrderByCheckInTimeAsc(c1Id)).thenReturn(List.of(v1, v2));
        when(visitLogsRepository.findByCustomerIdOrderByCheckInTimeAsc(c2Id)).thenReturn(List.of(v3));

        CustomerAnalyticsResponse response = customerAnalyticsService.getCustomerAnalytics(2026, 8, 2, false, null);

        assertNotNull(response);
        assertEquals(2, response.totalCustomersCount());
        assertEquals(1, response.regularCustomersCount());
        assertEquals(1, response.occasionalCustomersCount());
        assertEquals(2, response.regularThreshold());
        assertEquals(50.0, response.regularPercentage());
        assertEquals(2026, response.year());
        assertEquals(8, response.month());
        assertEquals(2, response.customers().size());

        CustomerVisitAnalyticsDto dto1 = response.customers().get(0);
        assertEquals("Frequent Visitor", dto1.customerName());
        assertEquals(8, dto1.totalVisits());
        assertEquals(2, dto1.monthlyVisits());
        assertTrue(dto1.isRegular());
        assertEquals("REGULAR", dto1.customerTag());
        assertEquals(new BigDecimal("12000"), dto1.totalSpent());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), dto1.firstVisitDate());
        assertEquals("SHARED_WORKSPACE", dto1.preferredServiceType());
    }

    @Test
    void getRegularCustomers_returnsOnlyRegulars() {
        UUID c1Id = UUID.randomUUID();
        Customers c1 = Customers.builder().id(c1Id).fullname("Top Regular").totalVisits(15).build();

        UUID c2Id = UUID.randomUUID();
        Customers c2 = Customers.builder().id(c2Id).fullname("One-time Visitor").totalVisits(1).build();

        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

        VisitLogs v1 = VisitLogs.builder().customer(c1).checkInTime(LocalDateTime.of(2026, 8, 5, 9, 0)).build();
        VisitLogs v2 = VisitLogs.builder().customer(c1).checkInTime(LocalDateTime.of(2026, 8, 10, 9, 0)).build();

        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of(v1, v2));
        when(visitLogsRepository.findByCustomerIdOrderByCheckInTimeAsc(c1Id)).thenReturn(List.of(v1, v2));

        PagedResponse<CustomerVisitAnalyticsDto> response = customerAnalyticsService.getRegularCustomers(2026, 8, 2, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(1, response.content().size());
        assertEquals("Top Regular", response.content().get(0).customerName());
    }

    @Test
    void getCustomerDetails_existingCustomer_returnsDtoWithMonthlyVisits() {
        UUID customerId = UUID.randomUUID();
        Customers customer = Customers.builder()
                .id(customerId)
                .fullname("Alice Wonder")
                .phoneNumber("1234567890")
                .totalVisits(6)
                .build();

        VisitLogs v1 = VisitLogs.builder().customer(customer).checkInTime(LocalDateTime.of(2026, 8, 3, 10, 0)).build();
        VisitLogs v2 = VisitLogs.builder().customer(customer).checkInTime(LocalDateTime.of(2026, 8, 15, 10, 0)).build();
        VisitLogs v3 = VisitLogs.builder().customer(customer).checkInTime(LocalDateTime.of(2026, 7, 20, 10, 0)).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(visitLogsRepository.findByCustomerIdOrderByCheckInTimeAsc(customerId)).thenReturn(List.of(v3, v1, v2));

        CustomerVisitAnalyticsDto dto = customerAnalyticsService.getCustomerDetails(customerId, 2026, 8, 2);

        assertNotNull(dto);
        assertEquals("Alice Wonder", dto.customerName());
        assertEquals(6, dto.totalVisits());
        assertEquals(2, dto.monthlyVisits());
        assertTrue(dto.isRegular());
        assertEquals("REGULAR", dto.customerTag());
    }

    @Test
    void getCustomerDetails_notFound_throwsResourceNotFoundException() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> customerAnalyticsService.getCustomerDetails(customerId, 2026, 8, 5));
    }

    @Test
    void getCustomerAnalytics_invalidMonth_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> customerAnalyticsService.getCustomerAnalytics(2026, 13, 5, false, null));

        assertThrows(BadRequestException.class,
                () -> customerAnalyticsService.getCustomerAnalytics(2026, 0, 5, false, null));
    }
}

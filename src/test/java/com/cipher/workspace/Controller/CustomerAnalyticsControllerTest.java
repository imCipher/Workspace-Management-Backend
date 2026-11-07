package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.CustomerAnalyticsResponse;
import com.cipher.workspace.DTOs.CustomerVisitAnalyticsDto;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Services.CustomerAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerAnalyticsControllerTest {

    @Mock
    private CustomerAnalyticsService customerAnalyticsService;

    @InjectMocks
    private CustomerAnalyticsController customerAnalyticsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerAnalyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCustomerAnalytics_returns200() throws Exception {
        CustomerAnalyticsResponse response = new CustomerAnalyticsResponse(
                10L, 3L, 7L, 5, 30.0, 2026, 10, List.of()
        );

        when(customerAnalyticsService.getCustomerAnalytics(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/customers?year=2026&month=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomersCount").value(10))
                .andExpect(jsonPath("$.regularCustomersCount").value(3))
                .andExpect(jsonPath("$.regularPercentage").value(30.0));
    }

    @Test
    void getRegulars_returns200() throws Exception {
        CustomerVisitAnalyticsDto regular = new CustomerVisitAnalyticsDto(
                UUID.randomUUID(), "Alice", "08011112222", 12, 6, true, "REGULAR", new java.math.BigDecimal("15000.00"), java.time.LocalDateTime.now().minusMonths(2), java.time.LocalDateTime.now(), "DEDICATED_DESK"
        );
        PagedResponse<CustomerVisitAnalyticsDto> paged = PagedResponse.of(List.of(regular), 0, 10, 1);

        when(customerAnalyticsService.getRegularCustomers(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/analytics/customers/regulars?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerName").value("Alice"))
                .andExpect(jsonPath("$.content[0].isRegular").value(true));
    }

    @Test
    void getCustomerDetails_returns200() throws Exception {
        UUID customerId = UUID.randomUUID();
        CustomerVisitAnalyticsDto dto = new CustomerVisitAnalyticsDto(
                customerId, "Bob", "08022223333", 8, 4, false, "OCCASIONAL", new java.math.BigDecimal("8000.00"), java.time.LocalDateTime.now().minusMonths(1), java.time.LocalDateTime.now(), "SHARED_WORKSPACE"
        );

        when(customerAnalyticsService.getCustomerDetails(eq(customerId), any(), any(), any()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/analytics/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.customerName").value("Bob"))
                .andExpect(jsonPath("$.isRegular").value(false));
    }

    @Test
    void getCustomerDetails_notFound_returns404() throws Exception {
        UUID customerId = UUID.randomUUID();

        when(customerAnalyticsService.getCustomerDetails(eq(customerId), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: " + customerId));

        mockMvc.perform(get("/api/analytics/customers/" + customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

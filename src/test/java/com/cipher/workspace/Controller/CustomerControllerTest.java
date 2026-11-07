package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.CustomerDetailResponse;
import com.cipher.workspace.DTOs.CustomerResponse;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.DTOs.VisitResponse;
import com.cipher.workspace.Entity.PaymentType;
import com.cipher.workspace.Entity.ServiceType;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Services.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCustomers_returns200AndPagedResponse() throws Exception {
        UUID id = UUID.randomUUID();
        CustomerResponse customerResponse = new CustomerResponse(
                id,
                "Jane Doe",
                "08012345678",
                5,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now()
        );

        PagedResponse<CustomerResponse> pagedResponse = PagedResponse.of(List.of(customerResponse), 0, 10, 1);

        when(customerService.getCustomers(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].fullname").value("Jane Doe"))
                .andExpect(jsonPath("$.content[0].phoneNumber").value("08012345678"))
                .andExpect(jsonPath("$.content[0].totalVisits").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCustomerById_returns200AndCustomerDetailResponse() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        VisitResponse visit = VisitResponse.builder()
                .id(visitId)
                .customerName("Jane Doe")
                .spaceAssigned("Desk #1")
                .serviceType(ServiceType.SHARED_WORKSPACE.name())
                .paymentType(PaymentType.CASH.name())
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(LocalDateTime.now().minusHours(1))
                .amountPaid(new BigDecimal("1500.00"))
                .build();

        CustomerDetailResponse detailResponse = new CustomerDetailResponse(
                customerId,
                "Jane Doe",
                "08012345678",
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                List.of(visit)
        );

        when(customerService.getCustomerById(customerId)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.fullname").value("Jane Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("08012345678"))
                .andExpect(jsonPath("$.totalVisits").value(1))
                .andExpect(jsonPath("$.visits[0].id").value(visitId.toString()))
                .andExpect(jsonPath("$.visits[0].spaceAssigned").value("Desk #1"))
                .andExpect(jsonPath("$.visits[0].amountPaid").value(1500.00));
    }

    @Test
    void getCustomerById_notFound_returns404() throws Exception {
        UUID customerId = UUID.randomUUID();

        when(customerService.getCustomerById(customerId))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: " + customerId));

        mockMvc.perform(get("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found with id: " + customerId));
    }

    @Test
    void getCustomerByPhone_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        CustomerResponse response = new CustomerResponse(id, "John Doe", "08099998888", 2, LocalDateTime.now(), LocalDateTime.now());

        when(customerService.getCustomerByPhone("08099998888")).thenReturn(response);

        mockMvc.perform(get("/api/customers/phone/08099998888")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullname").value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("08099998888"));
    }
}

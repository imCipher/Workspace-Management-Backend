package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.PaymentType;
import com.cipher.workspace.Entity.ServiceType;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Services.VisitsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VisitsControllerTest {

    @Mock
    private VisitsService visitsService;

    @InjectMocks
    private VisitsController visitsController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(visitsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checkIn_returns201() throws Exception {
        CheckInRequest request = CheckInRequest.builder()
                .customerName("Alice")
                .phoneNumber("08011112222")
                .spaceAssigned("Desk 4")
                .serviceType(ServiceType.DEDICATED_DESK.name())
                .paymentType(PaymentType.CASH.name())
                .amountPaid(new BigDecimal("2000.00"))
                .build();

        VisitResponse response = VisitResponse.builder()
                .id(UUID.randomUUID())
                .customerName("Alice")
                .spaceAssigned("Desk 4")
                .serviceType(ServiceType.DEDICATED_DESK.name())
                .paymentType(PaymentType.CASH.name())
                .checkInTime(LocalDateTime.now())
                .amountPaid(new BigDecimal("2000.00"))
                .build();

        when(visitsService.checkIn(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/visits/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.spaceAssigned").value("Desk 4"))
                .andExpect(jsonPath("$.amountPaid").value(2000.00));
    }

    @Test
    void checkOut_returns200() throws Exception {
        UUID visitId = UUID.randomUUID();
        CheckOutRequest request = new CheckOutRequest(visitId);
        VisitResponse response = VisitResponse.builder()
                .id(visitId)
                .customerName("Alice")
                .spaceAssigned("Desk 4")
                .serviceType(ServiceType.DEDICATED_DESK.name())
                .paymentType(PaymentType.CASH.name())
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(LocalDateTime.now())
                .amountPaid(new BigDecimal("2000.00"))
                .build();

        when(visitsService.checkOut(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/visits/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visitId.toString()));
    }

    @Test
    void checkOut_alreadyCheckedOut_returns400() throws Exception {
        UUID visitId = UUID.randomUUID();
        CheckOutRequest request = new CheckOutRequest(visitId);

        when(visitsService.checkOut(any(), any()))
                .thenThrow(new BadRequestException("Visit already checked out"));

        mockMvc.perform(post("/api/visits/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Visit already checked out"));
    }

    @Test
    void updateTimeLog_returns200() throws Exception {
        UUID visitId = UUID.randomUUID();
        UpdateTimeLogRequest request = new UpdateTimeLogRequest(visitId, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(1));
        VisitResponse response = VisitResponse.builder()
                .id(visitId)
                .customerName("Alice")
                .spaceAssigned("Desk 4")
                .serviceType(ServiceType.DEDICATED_DESK.name())
                .paymentType(PaymentType.CASH.name())
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .amountPaid(new BigDecimal("2000.00"))
                .build();

        when(visitsService.updateTimeLog(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/visits/time-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visitId.toString()));
    }

    @Test
    void getAllVisits_returns200() throws Exception {
        VisitLogsPagedResponse response = VisitLogsPagedResponse.of(List.of(), 0, 10, 0, 0, 0, 0, 0);
        when(visitsService.getAllVisits(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/visits?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getActiveVisits_returns200() throws Exception {
        PagedResponse<VisitResponse> response = PagedResponse.of(List.of(), 0, 10, 0);
        when(visitsService.getActiveVisits(0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/visits/active?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getVisitMetrics_returns200() throws Exception {
        VisitMetricsResponse response = new VisitMetricsResponse(
                LocalDate.now(), LocalDate.now(), LocalDate.now(), 2026, 10, 15L, 3L, 12L, 4L, 2L
        );
        when(visitsService.getVisitMetrics(any(), any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/visits/metrics?year=2026&month=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(15))
                .andExpect(jsonPath("$.activeVisits").value(3));
    }

    @Test
    void getVisitMetadata_returns200() throws Exception {
        VisitMetadataResponse response = new VisitMetadataResponse(
                List.of(new EnumOptionResponse("DESK", "Desk", true)),
                List.of(new EnumOptionResponse("CASH", "Cash", true)),
                List.of(new EnumOptionResponse("ACTIVE", "Active", true))
        );
        when(visitsService.getVisitMetadata()).thenReturn(response);

        mockMvc.perform(get("/api/visits/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceTypes[0].value").value("DESK"))
                .andExpect(jsonPath("$.paymentTypes[0].value").value("CASH"));
    }
}

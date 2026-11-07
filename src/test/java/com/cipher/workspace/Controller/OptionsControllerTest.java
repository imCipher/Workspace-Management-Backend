package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.CreateOptionRequest;
import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.cipher.workspace.DTOs.VisitMetadataResponse;
import com.cipher.workspace.Entity.PaymentTypeEntity;
import com.cipher.workspace.Entity.ServiceTypeEntity;
import com.cipher.workspace.Exception.DuplicateResourceException;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Repository.PaymentTypeRepository;
import com.cipher.workspace.Repository.ServiceTypeRepository;
import com.cipher.workspace.Services.ExpenseCategoryService;
import com.cipher.workspace.Services.VisitsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OptionsControllerTest {

    @Mock
    private VisitsService visitsService;

    @Mock
    private ExpenseCategoryService expenseCategoryService;

    @Mock
    private ServiceTypeRepository serviceTypeRepository;

    @Mock
    private PaymentTypeRepository paymentTypeRepository;

    @InjectMocks
    private OptionsController optionsController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(optionsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getServiceTypes_returns200() throws Exception {
        when(visitsService.getServiceTypes()).thenReturn(List.of(new EnumOptionResponse("DESK", "Desk", true)));

        mockMvc.perform(get("/api/options/service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("DESK"));
    }

    @Test
    void getPaymentTypes_returns200() throws Exception {
        when(visitsService.getPaymentTypes()).thenReturn(List.of(new EnumOptionResponse("CASH", "Cash", true)));

        mockMvc.perform(get("/api/options/payment-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("CASH"));
    }

    @Test
    void getExpenseCategories_returns200() throws Exception {
        when(expenseCategoryService.getCategoryOptions()).thenReturn(List.of(new EnumOptionResponse("UTILITIES", "Utilities", true)));

        mockMvc.perform(get("/api/options/expense-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("UTILITIES"));
    }

    @Test
    void getMetadata_returns200() throws Exception {
        VisitMetadataResponse metadata = new VisitMetadataResponse(List.of(), List.of(), List.of());
        when(visitsService.getVisitMetadata()).thenReturn(metadata);

        mockMvc.perform(get("/api/options/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceTypes").isArray());
    }

    @Test
    void addServiceType_returns201() throws Exception {
        CreateOptionRequest request = new CreateOptionRequest("PODCAST_STUDIO", "Podcast Studio");
        ServiceTypeEntity entity = new ServiceTypeEntity("PODCAST_STUDIO", "Podcast Studio", false);

        when(serviceTypeRepository.existsByNameIgnoreCase("PODCAST_STUDIO")).thenReturn(false);
        when(serviceTypeRepository.save(any())).thenReturn(entity);

        mockMvc.perform(post("/api/options/service-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("PODCAST_STUDIO"))
                .andExpect(jsonPath("$.label").value("Podcast Studio"));
    }

    @Test
    void addPaymentType_duplicate_returns409() throws Exception {
        CreateOptionRequest request = new CreateOptionRequest("CASH", "Cash");

        when(paymentTypeRepository.existsByNameIgnoreCase("CASH")).thenReturn(true);

        mockMvc.perform(post("/api/options/payment-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

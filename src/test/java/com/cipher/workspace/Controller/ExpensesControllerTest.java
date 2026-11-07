package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Exception.DuplicateResourceException;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Services.ExpenseCategoryService;
import com.cipher.workspace.Services.ExpensesService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpensesControllerTest {

    @Mock
    private ExpensesService expensesService;

    @Mock
    private ExpenseCategoryService expenseCategoryService;

    @InjectMocks
    private ExpensesController expensesController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(expensesController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addExpense_returns201() throws Exception {
        ExpenseRequest request = new ExpenseRequest("UTILITIES", new BigDecimal("5000.00"), LocalDate.now(), "Electricity bill");
        ExpenseResponse response = new ExpenseResponse(
                UUID.randomUUID(), UUID.randomUUID(), "admin", "UTILITIES", new BigDecimal("5000.00"), LocalDate.now(), "Electricity bill", LocalDateTime.now()
        );

        when(expensesService.addExpense(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("UTILITIES"))
                .andExpect(jsonPath("$.amount").value(5000.00));
    }

    @Test
    void getAllExpenses_returns200() throws Exception {
        PagedResponse<ExpenseResponse> paged = PagedResponse.of(List.of(), 0, 10, 0);
        when(expensesService.getAllExpenses(anyInt(), anyInt(), anyString(), anyString())).thenReturn(paged);

        mockMvc.perform(get("/api/expenses?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getExpenseById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ExpenseResponse response = new ExpenseResponse(
                id, UUID.randomUUID(), "admin", "INTERNET", new BigDecimal("12000.00"), LocalDate.now(), "Fibre", LocalDateTime.now()
        );

        when(expensesService.getExpenseById(id)).thenReturn(response);

        mockMvc.perform(get("/api/expenses/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.category").value("INTERNET"));
    }

    @Test
    void deleteExpense_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(expensesService).deleteExpense(id);

        mockMvc.perform(delete("/api/expenses/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void getExpenseById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(expensesService.getExpenseById(id))
                .thenThrow(new ResourceNotFoundException("Expense not found"));

        mockMvc.perform(get("/api/expenses/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void addCategory_returns201() throws Exception {
        CreateExpenseCategoryRequest request = new CreateExpenseCategoryRequest();
        request.setName("SNACKS");
        request.setDisplayName("Snacks & Beverages");

        ExpenseCategoryResponse response = new ExpenseCategoryResponse(UUID.randomUUID(), "SNACKS", "Snacks & Beverages", false, LocalDateTime.now(), LocalDateTime.now());
        when(expenseCategoryService.addCategory(any())).thenReturn(response);

        mockMvc.perform(post("/api/expenses/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SNACKS"));
    }

    @Test
    void addCategory_duplicate_returns409() throws Exception {
        CreateExpenseCategoryRequest request = new CreateExpenseCategoryRequest();
        request.setName("UTILITIES");
        request.setDisplayName("Utilities");

        when(expenseCategoryService.addCategory(any()))
                .thenThrow(new DuplicateResourceException("Expense category 'UTILITIES' already exists"));

        mockMvc.perform(post("/api/expenses/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

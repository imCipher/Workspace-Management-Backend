package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.cipher.workspace.DTOs.ExpenseRequest;
import com.cipher.workspace.DTOs.ExpenseResponse;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Entity.ExpenseCategory;
import com.cipher.workspace.Entity.Expenses;
import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.ExpenseCategoryRepository;
import com.cipher.workspace.Repository.ExpensesRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpensesServiceTest {

    @Mock
    private ExpensesRepository expensesRepository;

    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;

    private ExpensesService expensesService;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        expensesService = new ExpensesService(expensesRepository, expenseCategoryRepository);

        testAdmin = new User();
        testAdmin.setId(UUID.randomUUID());
        testAdmin.setUsername("admin1");
        testAdmin.setFullname("Administrator");
        testAdmin.setRole(Role.ADMIN);

        var auth = new UsernamePasswordAuthenticationToken(testAdmin, null, testAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addExpense_success() {
        ExpenseRequest request = ExpenseRequest.builder()
                .category("UTILITIES")
                .amount(new BigDecimal("25000"))
                .expenseDate(LocalDate.now())
                .description("Electricity bill")
                .build();

        Expenses saved = Expenses.builder()
                .id(UUID.randomUUID())
                .admin(testAdmin)
                .category("UTILITIES")
                .amount(new BigDecimal("25000"))
                .expenseDate(request.getExpenseDate())
                .description("Electricity bill")
                .build();

        when(expensesRepository.save(any(Expenses.class))).thenReturn(saved);

        ExpenseResponse response = expensesService.addExpense(request);

        assertNotNull(response);
        assertEquals(testAdmin.getId(), response.adminId());
        assertEquals("Administrator", response.adminName());
        assertEquals("UTILITIES", response.category());
        assertEquals(new BigDecimal("25000"), response.amount());
        assertEquals("Electricity bill", response.description());
        verify(expensesRepository).save(any(Expenses.class));
    }

    @Test
    void addExpense_missingCategory_throwsBadRequest() {
        ExpenseRequest request = ExpenseRequest.builder()
                .category("")
                .amount(new BigDecimal("1000"))
                .expenseDate(LocalDate.now())
                .build();

        assertThrows(BadRequestException.class, () -> expensesService.addExpense(request));
    }

    @Test
    void getAllExpenses_returnsList() {
        Expenses exp1 = Expenses.builder()
                .id(UUID.randomUUID())
                .admin(testAdmin)
                .category("SUPPLIES")
                .amount(new BigDecimal("5000"))
                .expenseDate(LocalDate.now())
                .build();

        Expenses exp2 = Expenses.builder()
                .id(UUID.randomUUID())
                .admin(testAdmin)
                .category("MAINTENANCE")
                .amount(new BigDecimal("12000"))
                .expenseDate(LocalDate.now())
                .build();

        when(expensesRepository.findAll(any(Sort.class))).thenReturn(List.of(exp1, exp2));

        List<ExpenseResponse> list = expensesService.getAllExpenses();

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("SUPPLIES", list.get(0).category());
        assertEquals("MAINTENANCE", list.get(1).category());
    }

    @Test
    void getAllExpenses_paged_returnsPagedResponse() {
        Expenses exp1 = Expenses.builder()
                .id(UUID.randomUUID())
                .admin(testAdmin)
                .category("SUPPLIES")
                .amount(new BigDecimal("5000"))
                .expenseDate(LocalDate.now())
                .build();

        when(expensesRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exp1)));

        PagedResponse<ExpenseResponse> page = expensesService.getAllExpenses(0, 10, "expenseDate", "desc");

        assertNotNull(page);
        assertEquals(1, page.totalElements());
        assertEquals("SUPPLIES", page.content().get(0).category());
    }

    @Test
    void getExpenseById_existing_returnsExpense() {
        UUID id = UUID.randomUUID();
        Expenses exp = Expenses.builder()
                .id(id)
                .admin(testAdmin)
                .category("UTILITIES")
                .amount(new BigDecimal("4000"))
                .expenseDate(LocalDate.now())
                .build();

        when(expensesRepository.findById(id)).thenReturn(Optional.of(exp));

        ExpenseResponse response = expensesService.getExpenseById(id);
        assertNotNull(response);
        assertEquals(id, response.id());
    }

    @Test
    void getExpenseById_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(expensesRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expensesService.getExpenseById(id));
    }

    @Test
    void deleteExpense_success() {
        UUID id = UUID.randomUUID();
        Expenses exp = Expenses.builder().id(id).build();

        when(expensesRepository.findById(id)).thenReturn(Optional.of(exp));
        doNothing().when(expensesRepository).delete(exp);

        assertDoesNotThrow(() -> expensesService.deleteExpense(id));
        verify(expensesRepository).delete(exp);
    }

    @Test
    void getExpensesByCategory_returnsPaged() {
        Expenses exp = Expenses.builder()
                .id(UUID.randomUUID())
                .category("UTILITIES")
                .amount(new BigDecimal("1000"))
                .expenseDate(LocalDate.now())
                .build();

        when(expensesRepository.findByCategoryOrderByExpenseDateDesc(eq("UTILITIES"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exp)));

        PagedResponse<ExpenseResponse> response = expensesService.getExpensesByCategory("utilities", 0, 10);
        assertNotNull(response);
        assertEquals(1, response.totalElements());
    }

    @Test
    void getExpenseCategories_returnsCategories() {
        when(expenseCategoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        List<EnumOptionResponse> categories = expensesService.getExpenseCategories();
        assertNotNull(categories);
        assertEquals(ExpenseCategory.values().length, categories.size());
    }
}

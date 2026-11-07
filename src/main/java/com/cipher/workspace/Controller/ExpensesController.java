package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Services.ExpenseCategoryService;
import com.cipher.workspace.Services.ExpensesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Expenses Management", description = "Endpoints for logging operational expenses, managing expense categories, and tracking costs (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/expenses", "/expenses"})
@PreAuthorize("hasRole('ADMIN')")
public class ExpensesController {

    private final ExpensesService expensesService;
    private final ExpenseCategoryService expenseCategoryService;

    public ExpensesController(ExpensesService expensesService, ExpenseCategoryService expenseCategoryService) {
        this.expensesService = expensesService;
        this.expenseCategoryService = expenseCategoryService;
    }

    @Operation(summary = "Record new expense", description = "Creates a new workspace operating expense record.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Expense created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid expense payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(
            @Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal User adminUser
    ) {
        ExpenseResponse response = expensesService.addExpense(request, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all expenses", description = "Retrieves paginated workspace expenses sorted by date or amount.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of expenses returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ExpenseResponse>> getAllExpenses(
            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort property (expenseDate, amount, category, createdAt)")
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PagedResponse<ExpenseResponse> response = expensesService.getAllExpenses(page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get expense by ID", description = "Retrieves an expense entry by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense details returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Expense not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(
            @Parameter(description = "UUID of expense")
            @PathVariable UUID id
    ) {
        ExpenseResponse response = expensesService.getExpenseById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete expense", description = "Deletes an expense record by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Expense deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Expense not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @Parameter(description = "UUID of expense")
            @PathVariable UUID id
    ) {
        expensesService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get expense categories", description = "Retrieves all expense categories.")
    @GetMapping("/categories")
    public ResponseEntity<List<ExpenseCategoryResponse>> getCategories() {
        return ResponseEntity.ok(expenseCategoryService.getCategories());
    }

    @Operation(summary = "Get expense category dropdown options", description = "Retrieves expense categories formatted as key-value enum options for UI dropdowns.")
    @GetMapping("/categories/options")
    public ResponseEntity<List<EnumOptionResponse>> getCategoryOptions() {
        return ResponseEntity.ok(expenseCategoryService.getCategoryOptions());
    }

    @Operation(summary = "Add expense category", description = "Creates a new custom expense category.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Expense category created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid category data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Category with this name already exists")
    })
    @PostMapping("/categories")
    public ResponseEntity<ExpenseCategoryResponse> addCategory(
            @Valid @RequestBody CreateExpenseCategoryRequest request
    ) {
        ExpenseCategoryResponse response = expenseCategoryService.addCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update expense category", description = "Updates an existing expense category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Category with this name already exists")
    })
    @PutMapping("/categories/{id}")
    public ResponseEntity<ExpenseCategoryResponse> updateCategory(
            @Parameter(description = "Category UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseCategoryRequest request
    ) {
        ExpenseCategoryResponse response = expenseCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }
}

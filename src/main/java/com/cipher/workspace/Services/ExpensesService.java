package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.cipher.workspace.DTOs.ExpenseRequest;
import com.cipher.workspace.DTOs.ExpenseResponse;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Entity.ExpenseCategory;
import com.cipher.workspace.Entity.Expenses;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.ExpenseCategoryRepository;
import com.cipher.workspace.Repository.ExpensesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ExpensesService {

    private final ExpensesRepository expensesRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Autowired
    public ExpensesService(
            ExpensesRepository expensesRepository,
            @Autowired(required = false) ExpenseCategoryRepository expenseCategoryRepository
    ) {
        this.expensesRepository = expensesRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
    }

    public ExpensesService(ExpensesRepository expensesRepository) {
        this(expensesRepository, null);
    }

    @Transactional
    public ExpenseResponse addExpense(ExpenseRequest request, User admin) {
        if (admin == null) {
            admin = getCurrentUser();
        }
        if (admin == null) {
            throw new BadRequestException("Authenticated user required to record an expense");
        }

        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new BadRequestException("Category is required");
        }
        if (request.getAmount() == null) {
            throw new BadRequestException("Amount is required");
        }
        if (request.getExpenseDate() == null) {
            throw new BadRequestException("Expense date is required");
        }

        String normalizedCategory = request.getCategory().trim().toUpperCase().replace(' ', '_');

        Expenses expense = Expenses.builder()
                .admin(admin)
                .category(normalizedCategory)
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .description(request.getDescription())
                .build();

        Expenses savedExpense = expensesRepository.save(expense);
        return ExpenseResponse.from(savedExpense);
    }

    public ExpenseResponse addExpense(ExpenseRequest request) {
        return addExpense(request, getCurrentUser());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ExpenseResponse> getAllExpenses(int page, int size, String sortBy, String sortDir) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));

        String property;
        if ("amount".equalsIgnoreCase(sortBy)) {
            property = "amount";
        } else if ("category".equalsIgnoreCase(sortBy)) {
            property = "category";
        } else if ("createdAt".equalsIgnoreCase(sortBy)) {
            property = "createdAt";
        } else {
            property = "expenseDate";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, property));

        Page<Expenses> expensesPage = expensesRepository.findAll(pageable);
        return PagedResponse.from(expensesPage.map(ExpenseResponse::from));
    }

    public PagedResponse<ExpenseResponse> getAllExpenses(int page, int size) {
        return getAllExpenses(page, size, "expenseDate", "desc");
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        List<Expenses> expenses = expensesRepository.findAll(Sort.by(Sort.Direction.DESC, "expenseDate"));
        return expenses.stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID id) {
        Expenses expense = expensesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void deleteExpense(UUID id) {
        Expenses expense = expensesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));
        expensesRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ExpenseResponse> getExpensesByCategory(String category, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        String normalizedCategory = category != null ? category.trim().toUpperCase().replace(' ', '_') : "";
        Page<Expenses> expensesPage = expensesRepository.findByCategoryOrderByExpenseDateDesc(normalizedCategory, pageable);
        return PagedResponse.from(expensesPage.map(ExpenseResponse::from));
    }

    @Transactional(readOnly = true)
    public List<EnumOptionResponse> getExpenseCategories() {
        if (expenseCategoryRepository != null) {
            var entities = expenseCategoryRepository.findAllByOrderByNameAsc();
            if (entities != null && !entities.isEmpty()) {
                return entities.stream()
                        .map(e -> new EnumOptionResponse(e.getName(), e.getDisplayName(), e.isDefault()))
                        .toList();
            }
        }
        return Arrays.stream(ExpenseCategory.values())
                .map(ec -> new EnumOptionResponse(ec.name(), ec.getDisplayName(), true))
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}

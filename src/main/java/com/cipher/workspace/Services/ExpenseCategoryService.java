package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.CreateExpenseCategoryRequest;
import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.cipher.workspace.DTOs.ExpenseCategoryResponse;
import com.cipher.workspace.DTOs.UpdateExpenseCategoryRequest;
import com.cipher.workspace.Entity.ExpenseCategory;
import com.cipher.workspace.Entity.ExpenseCategoryEntity;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.DuplicateResourceException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.ExpenseCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    public ExpenseCategoryService(ExpenseCategoryRepository expenseCategoryRepository) {
        this.expenseCategoryRepository = expenseCategoryRepository;
    }

    @Transactional
    public List<ExpenseCategoryResponse> getCategories() {
        ensureDefaultCategories();
        List<ExpenseCategoryEntity> entities = expenseCategoryRepository.findAllByOrderByNameAsc();
        return entities.stream()
                .map(ExpenseCategoryResponse::from)
                .toList();
    }

    @Transactional
    public List<EnumOptionResponse> getCategoryOptions() {
        return getCategories().stream()
                .map(cat -> new EnumOptionResponse(cat.name(), cat.displayName(), cat.isDefault()))
                .toList();
    }

    @Transactional
    public ExpenseCategoryResponse addCategory(CreateExpenseCategoryRequest request) {
        ensureDefaultCategories();

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new BadRequestException("Display name is required");
        }

        String normalizedName = normalizeCategoryName(request.getName());
        String trimmedDisplayName = request.getDisplayName().trim();

        if (expenseCategoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException("Expense category '" + normalizedName + "' already exists");
        }

        ExpenseCategoryEntity category = new ExpenseCategoryEntity(
                normalizedName,
                trimmedDisplayName,
                false
        );

        ExpenseCategoryEntity saved = expenseCategoryRepository.save(category);
        return ExpenseCategoryResponse.from(saved);
    }

    @Transactional
    public ExpenseCategoryResponse updateCategory(UUID id, UpdateExpenseCategoryRequest request) {
        ExpenseCategoryEntity category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newNormalizedName = normalizeCategoryName(request.getName());
            if (!newNormalizedName.equalsIgnoreCase(category.getName())) {
                if (expenseCategoryRepository.existsByNameIgnoreCase(newNormalizedName)) {
                    throw new DuplicateResourceException("Expense category '" + newNormalizedName + "' already exists");
                }
                category.setName(newNormalizedName);
            }
        }

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            category.setDisplayName(request.getDisplayName().trim());
        }

        ExpenseCategoryEntity updated = expenseCategoryRepository.save(category);
        return ExpenseCategoryResponse.from(updated);
    }

    @Transactional
    public void ensureDefaultCategories() {
        if (expenseCategoryRepository.count() == 0) {
            List<ExpenseCategoryEntity> defaults = new ArrayList<>();
            for (ExpenseCategory ec : ExpenseCategory.values()) {
                defaults.add(new ExpenseCategoryEntity(ec.name(), ec.getDisplayName(), true));
            }
            expenseCategoryRepository.saveAll(defaults);
        }
    }

    private String normalizeCategoryName(String raw) {
        return raw.trim().toUpperCase().replace(' ', '_');
    }
}

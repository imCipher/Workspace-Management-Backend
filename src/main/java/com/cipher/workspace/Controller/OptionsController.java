package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.CreateOptionRequest;
import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.cipher.workspace.DTOs.UpdateOptionRequest;
import com.cipher.workspace.DTOs.VisitMetadataResponse;
import com.cipher.workspace.Entity.PaymentTypeEntity;
import com.cipher.workspace.Entity.ServiceTypeEntity;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.DuplicateResourceException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.PaymentTypeRepository;
import com.cipher.workspace.Repository.ServiceTypeRepository;
import com.cipher.workspace.Services.ExpenseCategoryService;
import com.cipher.workspace.Services.VisitsService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Workspace Options & Metadata", description = "Endpoints for retrieving dynamic service types, payment types, and expense category options")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/options", "/options"})
public class OptionsController {

    private final VisitsService visitsService;
    private final ExpenseCategoryService expenseCategoryService;
    private final ServiceTypeRepository serviceTypeRepository;
    private final PaymentTypeRepository paymentTypeRepository;

    public OptionsController(
            VisitsService visitsService,
            ExpenseCategoryService expenseCategoryService,
            ServiceTypeRepository serviceTypeRepository,
            PaymentTypeRepository paymentTypeRepository
    ) {
        this.visitsService = visitsService;
        this.expenseCategoryService = expenseCategoryService;
        this.serviceTypeRepository = serviceTypeRepository;
        this.paymentTypeRepository = paymentTypeRepository;
    }

    @Operation(summary = "Get workspace service types", description = "Retrieves active workspace service types (Desk, Meeting room, etc.).")
    @GetMapping("/service-types")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<List<EnumOptionResponse>> getServiceTypes() {
        return ResponseEntity.ok(visitsService.getServiceTypes());
    }

    @Operation(summary = "Get workspace payment types", description = "Retrieves active payment types (Cash, Card, Transfer, etc.).")
    @GetMapping("/payment-types")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<List<EnumOptionResponse>> getPaymentTypes() {
        return ResponseEntity.ok(visitsService.getPaymentTypes());
    }

    @Operation(summary = "Get expense categories", description = "Retrieves expense categories formatted as options.")
    @GetMapping("/expense-categories")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<List<EnumOptionResponse>> getExpenseCategories() {
        return ResponseEntity.ok(expenseCategoryService.getCategoryOptions());
    }

    @Operation(summary = "Get combined workspace metadata", description = "Retrieves service types, payment types, and visit statuses in a single call.")
    @GetMapping("/metadata")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    public ResponseEntity<VisitMetadataResponse> getMetadata() {
        return ResponseEntity.ok(visitsService.getVisitMetadata());
    }

    @Operation(summary = "Add dynamic service type (Admin only)", description = "Registers a new custom service type for desk check-ins.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service type added successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Service type already exists")
    })
    @PostMapping("/service-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnumOptionResponse> addServiceType(@Valid @RequestBody CreateOptionRequest request) {
        String normalizedName = normalizeOptionName(request.getName());
        if (serviceTypeRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException("Service type '" + normalizedName + "' already exists");
        }
        ServiceTypeEntity entity = new ServiceTypeEntity(normalizedName, request.getDisplayName().trim(), false);
        ServiceTypeEntity saved = serviceTypeRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EnumOptionResponse(saved.getName(), saved.getDisplayName(), saved.isDefault()));
    }

    @Operation(summary = "Update service type (Admin only)", description = "Updates display name or identifier of an existing service type.")
    @PutMapping("/service-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnumOptionResponse> updateServiceType(
            @Parameter(description = "UUID of service type") @PathVariable UUID id,
            @Valid @RequestBody UpdateOptionRequest request
    ) {
        ServiceTypeEntity entity = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = normalizeOptionName(request.getName());
            if (!newName.equalsIgnoreCase(entity.getName()) && serviceTypeRepository.existsByNameIgnoreCase(newName)) {
                throw new DuplicateResourceException("Service type '" + newName + "' already exists");
            }
            entity.setName(newName);
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            entity.setDisplayName(request.getDisplayName().trim());
        }

        ServiceTypeEntity updated = serviceTypeRepository.save(entity);
        return ResponseEntity.ok(new EnumOptionResponse(updated.getName(), updated.getDisplayName(), updated.isDefault()));
    }

    @Operation(summary = "Add dynamic payment type (Admin only)", description = "Registers a new custom payment method.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment type added successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Payment type already exists")
    })
    @PostMapping("/payment-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnumOptionResponse> addPaymentType(@Valid @RequestBody CreateOptionRequest request) {
        String normalizedName = normalizeOptionName(request.getName());
        if (paymentTypeRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException("Payment type '" + normalizedName + "' already exists");
        }
        PaymentTypeEntity entity = new PaymentTypeEntity(normalizedName, request.getDisplayName().trim(), false);
        PaymentTypeEntity saved = paymentTypeRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EnumOptionResponse(saved.getName(), saved.getDisplayName(), saved.isDefault()));
    }

    @Operation(summary = "Update payment type (Admin only)", description = "Updates display name or identifier of an existing payment method.")
    @PutMapping("/payment-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnumOptionResponse> updatePaymentType(
            @Parameter(description = "UUID of payment type") @PathVariable UUID id,
            @Valid @RequestBody UpdateOptionRequest request
    ) {
        PaymentTypeEntity entity = paymentTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment type not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = normalizeOptionName(request.getName());
            if (!newName.equalsIgnoreCase(entity.getName()) && paymentTypeRepository.existsByNameIgnoreCase(newName)) {
                throw new DuplicateResourceException("Payment type '" + newName + "' already exists");
            }
            entity.setName(newName);
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            entity.setDisplayName(request.getDisplayName().trim());
        }

        PaymentTypeEntity updated = paymentTypeRepository.save(entity);
        return ResponseEntity.ok(new EnumOptionResponse(updated.getName(), updated.getDisplayName(), updated.isDefault()));
    }

    private String normalizeOptionName(String raw) {
        return raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
    }
}

package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.CustomerDetailResponse;
import com.cipher.workspace.DTOs.CustomerResponse;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customer Management", description = "Endpoints for viewing customer profiles and detailed visit histories")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/customers", "/customers"})
@PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Get paginated customers", description = "Retrieves paginated customers with optional name or phone search and dynamic sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of customers returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<CustomerResponse>> getCustomers(
            @Parameter(description = "Search term across customer name or phone number")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size limit")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (createdAt, fullname, phoneNumber, totalVisits)")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PagedResponse<CustomerResponse> response = customerService.getCustomers(search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get customer details by ID", description = "Retrieves full profile and visit history for a specific customer by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer details returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailResponse> getCustomerById(
            @Parameter(description = "UUID of customer")
            @PathVariable UUID id
    ) {
        CustomerDetailResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get customer by phone number", description = "Retrieves customer profile by exact phone number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer returned"),
            @ApiResponse(responseCode = "400", description = "Phone number is required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<CustomerResponse> getCustomerByPhone(
            @Parameter(description = "Phone number of customer")
            @PathVariable String phoneNumber
    ) {
        CustomerResponse response = customerService.getCustomerByPhone(phoneNumber);
        return ResponseEntity.ok(response);
    }
}

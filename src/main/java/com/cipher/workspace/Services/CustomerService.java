package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.CustomerDetailResponse;
import com.cipher.workspace.DTOs.CustomerResponse;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Entity.Customers;
import com.cipher.workspace.Entity.VisitLogs;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.DuplicateResourceException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.CustomerRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    public static final int DEFAULT_REGULAR_THRESHOLD = 5;

    private final CustomerRepository customerRepository;
    private final VisitLogsRepository visitLogsRepository;

    public CustomerService(CustomerRepository customerRepository, VisitLogsRepository visitLogsRepository) {
        this.customerRepository = customerRepository;
        this.visitLogsRepository = visitLogsRepository;
    }

    /**
     * Retrieve paginated customers with optional name/phone substring search and dynamic sorting.
     */
    public PagedResponse<CustomerResponse> getCustomers(String search, int page, int size, String sortBy, String sortDir) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));

        String property;
        if ("name".equalsIgnoreCase(sortBy) || "fullname".equalsIgnoreCase(sortBy)) {
            property = "fullname";
        } else if ("phone".equalsIgnoreCase(sortBy) || "phoneNumber".equalsIgnoreCase(sortBy)) {
            property = "phoneNumber";
        } else if ("visits".equalsIgnoreCase(sortBy) || "totalVisits".equalsIgnoreCase(sortBy)) {
            property = "totalVisits";
        } else if ("updatedAt".equalsIgnoreCase(sortBy)) {
            property = "updatedAt";
        } else {
            property = "createdAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, property));

        Page<Customers> customerPage;
        if (search != null && !search.trim().isBlank()) {
            String trimmedSearch = search.trim();
            customerPage = customerRepository.searchByNameOrPhone(trimmedSearch, pageable);
        } else {
            customerPage = customerRepository.findAll(pageable);
        }

        return PagedResponse.from(customerPage.map(CustomerResponse::from));
    }

    public PagedResponse<CustomerResponse> getCustomers(int page, int size) {
        return getCustomers(null, page, size, "createdAt", "desc");
    }

    public List<CustomerResponse> getCustomers() {
        return customerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    /**
     * Retrieve customer by UUID along with their comprehensive visit history.
     */
    public CustomerDetailResponse getCustomerById(UUID id) {
        Customers customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        List<VisitLogs> visits = visitLogsRepository.findByCustomerIdOrderByCheckInTimeDesc(id);

        return CustomerDetailResponse.of(customer, visits);
    }

    /**
     * Retrieve customer by exact phone number.
     */
    public CustomerResponse getCustomerByPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new BadRequestException("Phone number is required");
        }
        Customers customer = customerRepository.findByPhoneNumber(phoneNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone: " + phoneNumber));
        return CustomerResponse.from(customer);
    }

    /**
     * Create or lookup customer profile.
     */
    @Transactional
    public Customers findOrCreateCustomer(String fullname, String phoneNumber) {
        String name = fullname != null ? fullname.trim() : null;
        String phone = phoneNumber != null ? phoneNumber.trim() : null;

        if (name == null || name.isBlank()) {
            throw new BadRequestException("Customer name is required");
        }

        Customers customer = null;
        if (phone != null && !phone.isBlank()) {
            customer = customerRepository.findByPhoneNumber(phone).orElse(null);
        }

        if (customer == null) {
            customer = customerRepository.findByFullnameIgnoreCase(name).orElse(null);
        }

        if (customer == null) {
            customer = Customers.builder()
                    .fullname(name)
                    .phoneNumber(phone)
                    .totalVisits(0)
                    .build();
            return customerRepository.save(customer);
        } else {
            if ((customer.getPhoneNumber() == null || customer.getPhoneNumber().isBlank()) && phone != null && !phone.isBlank()) {
                customer.setPhoneNumber(phone);
                return customerRepository.save(customer);
            }
            return customer;
        }
    }

    /**
     * Update customer profile details with uniqueness constraint validation on phone number.
     */
    @Transactional
    public CustomerResponse updateCustomer(UUID id, String fullname, String phoneNumber) {
        Customers customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        if (fullname != null && !fullname.isBlank()) {
            customer.setFullname(fullname.trim());
        }

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            String trimmedPhone = phoneNumber.trim();
            if (customerRepository.existsByPhoneNumberAndIdNot(trimmedPhone, id)) {
                throw new DuplicateResourceException("Phone number already registered to another customer: " + trimmedPhone);
            }
            customer.setPhoneNumber(trimmedPhone);
        }

        Customers saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }

    /**
     * Classify customer retention status based on check-ins in the target month and regular threshold.
     */
    public String classifyCustomer(Customers customer, YearMonth targetMonth, int threshold) {
        if (customer == null) {
            return "UNKNOWN";
        }
        int totalVisits = customer.getTotalVisits() != null ? customer.getTotalVisits() : 0;
        int monthlyVisits = getMonthlyVisits(customer, targetMonth);

        if (monthlyVisits >= threshold) {
            return "REGULAR";
        } else if (totalVisits == 1) {
            return "NEW";
        } else if (totalVisits == 0) {
            return "NO_VISITS";
        } else {
            return "OCCASIONAL";
        }
    }

    public String classifyCustomer(Customers customer, int threshold) {
        return classifyCustomer(customer, YearMonth.now(), threshold);
    }

    public String classifyCustomer(Customers customer) {
        return classifyCustomer(customer, YearMonth.now(), DEFAULT_REGULAR_THRESHOLD);
    }

    public boolean isRegularCustomer(Customers customer, YearMonth targetMonth, int threshold) {
        return getMonthlyVisits(customer, targetMonth) >= threshold;
    }

    public boolean isRegularCustomer(Customers customer, int threshold) {
        return isRegularCustomer(customer, YearMonth.now(), threshold);
    }

    public boolean isRegularCustomer(Customers customer) {
        return isRegularCustomer(customer, YearMonth.now(), DEFAULT_REGULAR_THRESHOLD);
    }

    public int getMonthlyVisits(Customers customer, YearMonth targetMonth) {
        if (customer == null || customer.getId() == null) {
            return 0;
        }
        YearMonth month = targetMonth != null ? targetMonth : YearMonth.now();
        LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = month.atEndOfMonth().atTime(LocalTime.MAX);
        return (int) visitLogsRepository.countByCustomerIdAndCheckInTimeBetween(customer.getId(), startOfMonth, endOfMonth);
    }
}

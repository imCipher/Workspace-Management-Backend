package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.CustomerDetailResponse;
import com.cipher.workspace.DTOs.CustomerResponse;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Entity.Customers;
import com.cipher.workspace.Entity.PaymentType;
import com.cipher.workspace.Entity.ServiceType;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.VisitLogs;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.DuplicateResourceException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.CustomerRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VisitLogsRepository visitLogsRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, visitLogsRepository);
    }

    @Test
    void getCustomers_withoutSearch_returnsPagedCustomers() {
        Customers c1 = Customers.builder()
                .id(UUID.randomUUID())
                .fullname("John Doe")
                .phoneNumber("08011112222")
                .totalVisits(5)
                .build();

        Customers c2 = Customers.builder()
                .id(UUID.randomUUID())
                .fullname("Jane Smith")
                .phoneNumber("08033334444")
                .totalVisits(3)
                .build();

        Page<Customers> page = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 2);

        when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<CustomerResponse> result = customerService.getCustomers(null, 0, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(2, result.content().size());
        assertEquals("John Doe", result.content().get(0).fullname());
        assertEquals("Jane Smith", result.content().get(1).fullname());
        assertEquals(2, result.totalElements());
        verify(customerRepository).findAll(any(Pageable.class));
        verify(customerRepository, never()).searchByNameOrPhone(any(), any());
    }

    @Test
    void getCustomers_withSearch_callsSearchByNameOrPhone() {
        Customers c1 = Customers.builder()
                .id(UUID.randomUUID())
                .fullname("John Doe")
                .phoneNumber("08011112222")
                .totalVisits(5)
                .build();

        Page<Customers> page = new PageImpl<>(List.of(c1), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);

        when(customerRepository.searchByNameOrPhone(eq("John"), any(Pageable.class))).thenReturn(page);

        PagedResponse<CustomerResponse> result = customerService.getCustomers("  John  ", 0, 10, "name", "asc");

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("John Doe", result.content().get(0).fullname());
        verify(customerRepository).searchByNameOrPhone(eq("John"), any(Pageable.class));
        verify(customerRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getCustomers_allList_returnsCustomerList() {
        Customers c1 = Customers.builder().id(UUID.randomUUID()).fullname("Alice").totalVisits(1).build();
        when(customerRepository.findAll(any(Sort.class))).thenReturn(List.of(c1));

        List<CustomerResponse> list = customerService.getCustomers();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Alice", list.get(0).fullname());
    }

    @Test
    void getCustomerById_existingCustomer_returnsCustomerDetailWithVisits() {
        UUID customerId = UUID.randomUUID();
        Customers customer = Customers.builder()
                .id(customerId)
                .fullname("Alice Johnson")
                .phoneNumber("08055556666")
                .totalVisits(2)
                .build();

        User worker = User.builder().id(UUID.randomUUID()).fullname("Worker").build();

        VisitLogs v1 = VisitLogs.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .user(worker)
                .checkInTime(LocalDateTime.now().minusDays(1))
                .checkOutTime(LocalDateTime.now().minusDays(1).plusHours(2))
                .amountPaid(new BigDecimal("20.00"))
                .spaceAssigned("Desk 1")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("CREDIT_CARD")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(visitLogsRepository.findByCustomerIdOrderByCheckInTimeDesc(customerId)).thenReturn(List.of(v1));

        CustomerDetailResponse detail = customerService.getCustomerById(customerId);

        assertNotNull(detail);
        assertEquals(customerId, detail.id());
        assertEquals("Alice Johnson", detail.fullname());
        assertEquals("08055556666", detail.phoneNumber());
        assertEquals(2, detail.totalVisits());
        assertEquals(1, detail.visits().size());
        assertEquals("Alice Johnson", detail.visits().get(0).getCustomerName());
        assertEquals("SHARED_WORKSPACE", detail.visits().get(0).getServiceType());
    }

    @Test
    void getCustomerById_notFound_throwsResourceNotFoundException() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerById(customerId));
    }

    @Test
    void getCustomerByPhone_existing_returnsCustomer() {
        Customers customer = Customers.builder()
                .id(UUID.randomUUID())
                .fullname("Bob")
                .phoneNumber("08099998888")
                .totalVisits(4)
                .build();

        when(customerRepository.findByPhoneNumber("08099998888")).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerByPhone("08099998888");
        assertNotNull(response);
        assertEquals("Bob", response.fullname());
        assertEquals("08099998888", response.phoneNumber());
    }

    @Test
    void getCustomerByPhone_blank_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> customerService.getCustomerByPhone("   "));
    }

    @Test
    void getCustomerByPhone_notFound_throwsResourceNotFound() {
        when(customerRepository.findByPhoneNumber("123")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerByPhone("123"));
    }

    @Test
    void findOrCreateCustomer_newCustomer_createsAndReturns() {
        when(customerRepository.findByPhoneNumber("08012345678")).thenReturn(Optional.empty());
        when(customerRepository.findByFullnameIgnoreCase("New User")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customers result = customerService.findOrCreateCustomer("New User", "08012345678");
        assertNotNull(result);
        assertEquals("New User", result.getFullname());
        assertEquals("08012345678", result.getPhoneNumber());
        assertEquals(0, result.getTotalVisits());
    }

    @Test
    void findOrCreateCustomer_existingByPhone_returnsExisting() {
        Customers existing = Customers.builder().id(UUID.randomUUID()).fullname("Old Name").phoneNumber("08012345678").build();
        when(customerRepository.findByPhoneNumber("08012345678")).thenReturn(Optional.of(existing));

        Customers result = customerService.findOrCreateCustomer("Any Name", "08012345678");
        assertEquals(existing, result);
    }

    @Test
    void updateCustomer_success_updatesNameAndPhone() {
        UUID customerId = UUID.randomUUID();
        Customers customer = Customers.builder().id(customerId).fullname("Old").phoneNumber("111").build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByPhoneNumberAndIdNot("222", customerId)).thenReturn(false);
        when(customerRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse updated = customerService.updateCustomer(customerId, "New", "222");
        assertNotNull(updated);
        assertEquals("New", updated.fullname());
        assertEquals("222", updated.phoneNumber());
    }

    @Test
    void updateCustomer_duplicatePhone_throwsDuplicateResourceException() {
        UUID customerId = UUID.randomUUID();
        Customers customer = Customers.builder().id(customerId).fullname("Old").phoneNumber("111").build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByPhoneNumberAndIdNot("222", customerId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.updateCustomer(customerId, "New", "222"));
    }

    @Test
    void classifyCustomer_andIsRegular_checksCorrectlyBasedOnMonth() {
        UUID noVisitsId = UUID.randomUUID();
        Customers noVisits = Customers.builder().id(noVisitsId).totalVisits(0).build();

        UUID newCustId = UUID.randomUUID();
        Customers newCust = Customers.builder().id(newCustId).totalVisits(1).build();

        UUID occasionalId = UUID.randomUUID();
        Customers occasional = Customers.builder().id(occasionalId).totalVisits(10).build(); // 10 lifetime visits, but only 2 this month

        UUID regularId = UUID.randomUUID();
        Customers regular = Customers.builder().id(regularId).totalVisits(5).build(); // 5 visits this month

        when(visitLogsRepository.countByCustomerIdAndCheckInTimeBetween(eq(noVisitsId), any(), any())).thenReturn(0L);
        when(visitLogsRepository.countByCustomerIdAndCheckInTimeBetween(eq(newCustId), any(), any())).thenReturn(1L);
        when(visitLogsRepository.countByCustomerIdAndCheckInTimeBetween(eq(occasionalId), any(), any())).thenReturn(2L);
        when(visitLogsRepository.countByCustomerIdAndCheckInTimeBetween(eq(regularId), any(), any())).thenReturn(5L);

        assertEquals("NO_VISITS", customerService.classifyCustomer(noVisits, 5));
        assertEquals("NEW", customerService.classifyCustomer(newCust, 5));
        assertEquals("OCCASIONAL", customerService.classifyCustomer(occasional, 5));
        assertEquals("REGULAR", customerService.classifyCustomer(regular, 5));

        assertFalse(customerService.isRegularCustomer(occasional, 5));
        assertTrue(customerService.isRegularCustomer(regular, 5));
    }
}

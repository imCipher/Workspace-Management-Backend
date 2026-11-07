package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.*;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.CustomerRepository;
import com.cipher.workspace.Repository.PaymentTypeRepository;
import com.cipher.workspace.Repository.ServiceTypeRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitsServiceTest {

    @Mock
    private VisitLogsRepository visitLogsRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ServiceTypeRepository serviceTypeRepository;

    @Mock
    private PaymentTypeRepository paymentTypeRepository;

    private VisitsService visitsService;
    private User testWorker;

    @BeforeEach
    void setUp() {
        visitsService = new VisitsService(visitLogsRepository, customerRepository, serviceTypeRepository, paymentTypeRepository);

        testWorker = new User();
        testWorker.setId(UUID.randomUUID());
        testWorker.setUsername("worker1");
        testWorker.setFullname("Worker One");
        testWorker.setRole(Role.WORKER);

        var auth = new UsernamePasswordAuthenticationToken(testWorker, null, testWorker.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkIn_withNewCustomer_provisionsCustomerAndReturnsStaffDetails() {
        CheckInRequest request = CheckInRequest.builder()
                .customerName("New Customer")
                .phoneNumber("1234567890")
                .spaceAssigned("Desk 4")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("CASH")
                .amountPaid(new BigDecimal("5000"))
                .build();

        when(customerRepository.findByPhoneNumber("1234567890")).thenReturn(Optional.empty());
        when(customerRepository.findByFullnameIgnoreCase("New Customer")).thenReturn(Optional.empty());

        Customers newCustomer = new Customers();
        newCustomer.setId(UUID.randomUUID());
        newCustomer.setFullname("New Customer");
        newCustomer.setPhoneNumber("1234567890");
        newCustomer.setTotalVisits(0);

        when(customerRepository.save(any(Customers.class))).thenReturn(newCustomer);

        VisitLogs savedLog = VisitLogs.builder()
                .id(UUID.randomUUID())
                .customer(newCustomer)
                .user(testWorker)
                .spaceAssigned("Desk 4")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("CASH")
                .checkInTime(LocalDateTime.now())
                .amountPaid(new BigDecimal("5000"))
                .build();

        when(visitLogsRepository.save(any(VisitLogs.class))).thenReturn(savedLog);

        VisitResponse response = visitsService.checkIn(request);

        assertNotNull(response);
        assertEquals("New Customer", response.getCustomerName());
        assertEquals("Desk 4", response.getSpaceAssigned());
        assertEquals("SHARED_WORKSPACE", response.getServiceType());
        assertEquals("CASH", response.getPaymentType());
        assertEquals(new BigDecimal("5000"), response.getAmountPaid());
        assertEquals(testWorker.getId(), response.getStaffId());
        assertEquals("Worker One", response.getStaffName());
        assertEquals(testWorker.getId(), response.getCheckedInById());
        assertEquals("Worker One", response.getCheckedInByName());
        verify(customerRepository, atLeastOnce()).save(any(Customers.class));
        verify(visitLogsRepository).save(any(VisitLogs.class));
    }

    @Test
    void checkIn_withExistingCustomerByPhone_reusesCustomerAndIncrementsVisits() {
        CheckInRequest request = CheckInRequest.builder()
                .customerName("Jane Doe")
                .phoneNumber("0987654321")
                .spaceAssigned("Office 1")
                .serviceType("PRIVATE_OFFICE")
                .paymentType("CASH")
                .amountPaid(new BigDecimal("15000"))
                .build();

        Customers existingCustomer = new Customers();
        existingCustomer.setId(UUID.randomUUID());
        existingCustomer.setFullname("Jane Doe");
        existingCustomer.setPhoneNumber("0987654321");
        existingCustomer.setTotalVisits(2);

        when(customerRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(existingCustomer));

        VisitLogs savedLog = VisitLogs.builder()
                .id(UUID.randomUUID())
                .customer(existingCustomer)
                .user(testWorker)
                .spaceAssigned("Office 1")
                .serviceType("PRIVATE_OFFICE")
                .paymentType("CASH")
                .checkInTime(LocalDateTime.now())
                .amountPaid(new BigDecimal("15000"))
                .build();

        when(visitLogsRepository.save(any(VisitLogs.class))).thenReturn(savedLog);

        VisitResponse response = visitsService.checkIn(request);

        assertNotNull(response);
        assertEquals("Jane Doe", response.getCustomerName());
        assertEquals(3, existingCustomer.getTotalVisits());
    }

    @Test
    void checkIn_withExistingCustomerId_reusesCustomer() {
        UUID customerId = UUID.randomUUID();
        CheckInRequest request = CheckInRequest.builder()
                .customerId(customerId)
                .customerName("Alice")
                .spaceAssigned("Desk 1")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("ONLINE_TRANSFER")
                .amountPaid(new BigDecimal("2000"))
                .build();

        Customers existingCustomer = new Customers();
        existingCustomer.setId(customerId);
        existingCustomer.setFullname("Alice");
        existingCustomer.setTotalVisits(5);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));

        VisitLogs savedLog = VisitLogs.builder()
                .id(UUID.randomUUID())
                .customer(existingCustomer)
                .user(testWorker)
                .spaceAssigned("Desk 1")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("ONLINE_TRANSFER")
                .checkInTime(LocalDateTime.now())
                .amountPaid(new BigDecimal("2000"))
                .build();

        when(visitLogsRepository.save(any(VisitLogs.class))).thenReturn(savedLog);

        VisitResponse response = visitsService.checkIn(request);

        assertNotNull(response);
        assertEquals("Alice", response.getCustomerName());
        assertEquals(6, existingCustomer.getTotalVisits());
    }

    @Test
    void checkIn_missingNameAndCustomerId_throwsBadRequest() {
        CheckInRequest request = CheckInRequest.builder()
                .customerName("")
                .spaceAssigned("Desk 1")
                .build();

        assertThrows(BadRequestException.class, () -> visitsService.checkIn(request));
    }

    @Test
    void checkOut_capturesStaffAndSetsCheckedOutBy() {
        UUID visitLogId = UUID.randomUUID();
        CheckOutRequest request = new CheckOutRequest(visitLogId);

        Customers customer = new Customers();
        customer.setFullname("Jane Doe");

        User checkInWorker = new User();
        checkInWorker.setId(UUID.randomUUID());
        checkInWorker.setFullname("CheckIn Worker");

        VisitLogs visitLog = VisitLogs.builder()
                .id(visitLogId)
                .customer(customer)
                .user(checkInWorker)
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(null)
                .spaceAssigned("Desk 1")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("CREDIT_CARD")
                .amountPaid(new BigDecimal("3000"))
                .build();

        when(visitLogsRepository.findById(visitLogId)).thenReturn(Optional.of(visitLog));
        when(visitLogsRepository.save(any(VisitLogs.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitResponse response = visitsService.checkOut(request);

        assertNotNull(response);
        assertNotNull(response.getCheckOutTime());
        assertEquals(testWorker.getId(), response.getCheckedOutById());
        assertEquals("Worker One", response.getCheckedOutByName());
        assertEquals(checkInWorker.getId(), response.getCheckedInById());
        assertEquals("CheckIn Worker", response.getCheckedInByName());
        verify(visitLogsRepository).save(visitLog);
    }

    @Test
    void checkOut_alreadyCheckedOut_throwsBadRequest() {
        UUID visitLogId = UUID.randomUUID();
        CheckOutRequest request = new CheckOutRequest(visitLogId);

        VisitLogs visitLog = VisitLogs.builder()
                .id(visitLogId)
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(LocalDateTime.now().minusHours(1))
                .build();

        when(visitLogsRepository.findById(visitLogId)).thenReturn(Optional.of(visitLog));

        assertThrows(BadRequestException.class, () -> visitsService.checkOut(request));
    }

    @Test
    void checkOut_visitNotFound_throwsResourceNotFound() {
        UUID visitLogId = UUID.randomUUID();
        CheckOutRequest request = new CheckOutRequest(visitLogId);

        when(visitLogsRepository.findById(visitLogId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> visitsService.checkOut(request));
    }

    @Test
    void updateTimeLog_capturesStaffAndUpdatesTimestamps() {
        UUID visitLogId = UUID.randomUUID();
        UpdateTimeLogRequest request = new UpdateTimeLogRequest(
                visitLogId,
                LocalDateTime.now().minusHours(4),
                LocalDateTime.now().minusHours(1)
        );

        Customers customer = new Customers();
        customer.setFullname("Jane Doe");

        VisitLogs visitLog = VisitLogs.builder()
                .id(visitLogId)
                .customer(customer)
                .user(testWorker)
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(null)
                .spaceAssigned("Desk 1")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("CREDIT_CARD")
                .amountPaid(new BigDecimal("3000"))
                .build();

        when(visitLogsRepository.findById(visitLogId)).thenReturn(Optional.of(visitLog));
        when(visitLogsRepository.save(any(VisitLogs.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitResponse response = visitsService.updateTimeLog(request);

        assertNotNull(response);
        assertEquals(request.getCheckInTime(), response.getCheckInTime());
        assertEquals(request.getCheckOutTime(), response.getCheckOutTime());
        assertEquals(testWorker.getId(), response.getUpdatedById());
        assertEquals("Worker One", response.getUpdatedByName());
        assertEquals(180L, response.getDurationInMinutes());
    }

    @Test
    void updateTimeLog_checkOutBeforeCheckIn_throwsBadRequest() {
        UUID visitLogId = UUID.randomUUID();
        UpdateTimeLogRequest request = new UpdateTimeLogRequest(
                visitLogId,
                null,
                LocalDateTime.now().minusHours(5)
        );

        VisitLogs visitLog = VisitLogs.builder()
                .id(visitLogId)
                .checkInTime(LocalDateTime.now().minusHours(2))
                .build();

        when(visitLogsRepository.findById(visitLogId)).thenReturn(Optional.of(visitLog));

        assertThrows(BadRequestException.class, () -> visitsService.updateTimeLog(request));
    }

    @Test
    void getActiveVisits_returnsActiveVisitors() {
        VisitLogs log = VisitLogs.builder()
                .id(UUID.randomUUID())
                .checkInTime(LocalDateTime.now().minusHours(1))
                .checkOutTime(null)
                .spaceAssigned("Desk 5")
                .serviceType("SHARED_WORKSPACE")
                .paymentType("CASH")
                .amountPaid(BigDecimal.TEN)
                .build();

        when(visitLogsRepository.findByCheckOutTimeIsNullOrderByCheckInTimeDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(visitLogsRepository.findByCheckOutTimeIsNullOrderByCheckInTimeDesc())
                .thenReturn(List.of(log));

        PagedResponse<VisitResponse> paged = visitsService.getActiveVisits(0, 10);
        assertNotNull(paged);
        assertEquals(1, paged.totalElements());

        List<VisitResponse> list = visitsService.getActiveVisits();
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    void getAllVisits_withFiltersAndPagination_returnsVisitLogsPagedResponse() {
        Customers customer = new Customers();
        customer.setId(UUID.randomUUID());
        customer.setFullname("Filtered Customer");
        customer.setTotalVisits(2);

        VisitLogs log = VisitLogs.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .user(testWorker)
                .checkInTime(LocalDateTime.now().minusHours(1))
                .serviceType("SHARED_WORKSPACE")
                .paymentType("CASH")
                .amountPaid(new BigDecimal("5000"))
                .build();

        when(visitLogsRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of(log));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        VisitLogsPagedResponse response = visitsService.getAllVisits(
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                null,
                null,
                null,
                "ACTIVE",
                null,
                0,
                10
        );

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalVisits());
        assertEquals(1, response.returningCustomers());
        assertEquals(1, response.content().size());
        assertEquals("Filtered Customer", response.content().get(0).getCustomerName());
    }

    @Test
    void getVisitMetrics_computesMetrics() {
        Customers customer = new Customers();
        customer.setId(UUID.randomUUID());
        customer.setFullname("Test Customer");
        customer.setTotalVisits(3);

        VisitLogs logActive = VisitLogs.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .user(testWorker)
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(null)
                .build();

        VisitLogs logCompleted = VisitLogs.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .user(testWorker)
                .checkInTime(LocalDateTime.now().minusHours(4))
                .checkOutTime(LocalDateTime.now().minusHours(1))
                .build();

        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of(logActive, logCompleted));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        VisitMetricsResponse response = visitsService.getVisitMetrics();

        assertNotNull(response);
        assertEquals(LocalDate.now(), response.date());
        assertEquals(2, response.totalVisits());
        assertEquals(1, response.activeVisits());
        assertEquals(1, response.completedVisits());
        assertEquals(1, response.returningCustomers());
    }

    @Test
    void getTodayMetrics_returnsMapWithDeskTotals() {
        when(visitLogsRepository.countByCheckOutTimeIsNull()).thenReturn(5L);
        when(visitLogsRepository.countByCheckInTimeBetween(any(), any())).thenReturn(14L);
        when(visitLogsRepository.sumAmountPaidBetween(any(), any())).thenReturn(new BigDecimal("75000"));

        Map<String, Object> metrics = visitsService.getTodayMetrics();

        assertNotNull(metrics);
        assertEquals(LocalDate.now(), metrics.get("date"));
        assertEquals(5L, metrics.get("activeVisitors"));
        assertEquals(14L, metrics.get("todayCheckIns"));
        assertEquals(new BigDecimal("75000"), metrics.get("todayRevenue"));
    }

    @Test
    void getOptionsAndMetadata_returnsConfiguredOptions() {
        when(serviceTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(paymentTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        List<EnumOptionResponse> paymentTypes = visitsService.getPaymentTypes();
        assertNotNull(paymentTypes);
        assertEquals(PaymentType.values().length, paymentTypes.size());

        List<EnumOptionResponse> serviceTypes = visitsService.getServiceTypes();
        assertNotNull(serviceTypes);
        assertEquals(ServiceType.values().length, serviceTypes.size());

        List<EnumOptionResponse> visitStatuses = visitsService.getVisitStatuses();
        assertNotNull(visitStatuses);
        assertEquals(VisitStatus.values().length, visitStatuses.size());

        VisitMetadataResponse metadata = visitsService.getVisitMetadata();
        assertNotNull(metadata);
        assertEquals(ServiceType.values().length, metadata.serviceTypes().size());
        assertEquals(PaymentType.values().length, metadata.paymentTypes().size());
        assertEquals(VisitStatus.values().length, metadata.statuses().size());
    }
}

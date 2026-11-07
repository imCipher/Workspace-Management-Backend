package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.*;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VisitsService {

    private final VisitLogsRepository visitLogsRepository;
    private final CustomerRepository customerRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final PaymentTypeRepository paymentTypeRepository;

    @Autowired
    public VisitsService(
            VisitLogsRepository visitLogsRepository,
            CustomerRepository customerRepository,
            @Autowired(required = false) ServiceTypeRepository serviceTypeRepository,
            @Autowired(required = false) PaymentTypeRepository paymentTypeRepository
    ) {
        this.visitLogsRepository = visitLogsRepository;
        this.customerRepository = customerRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.paymentTypeRepository = paymentTypeRepository;
    }

    public VisitsService(
            VisitLogsRepository visitLogsRepository,
            CustomerRepository customerRepository
    ) {
        this(visitLogsRepository, customerRepository, null, null);
    }

    /**
     * Check in a visitor.
     * If customer does not exist, provisions a new Customers profile.
     * Increments customer totalVisits and records check-in timestamp on server clock.
     */
    @Transactional
    public VisitResponse checkIn(CheckInRequest request, User currentUser) {
        if (currentUser == null) {
            currentUser = getCurrentUser();
        }

        String name = request.getCustomerName() != null ? request.getCustomerName().trim() : null;
        String phone = request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null;

        if ((name == null || name.isBlank()) && request.getCustomerId() == null) {
            throw new BadRequestException("Customer name is required");
        }

        Customers customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        }

        if (customer == null && phone != null && !phone.isBlank()) {
            customer = customerRepository.findByPhoneNumber(phone).orElse(null);
        }

        if (customer == null && name != null && !name.isBlank()) {
            customer = customerRepository.findByFullnameIgnoreCase(name).orElse(null);
        }

        if (customer == null) {
            customer = new Customers();
            customer.setFullname(name);
            customer.setPhoneNumber(phone);
            customer.setTotalVisits(0);
            customer = customerRepository.save(customer);
        } else {
            if ((customer.getPhoneNumber() == null || customer.getPhoneNumber().isBlank()) && phone != null && !phone.isBlank()) {
                customer.setPhoneNumber(phone);
                customer = customerRepository.save(customer);
            }
        }

        VisitLogs visitLog = new VisitLogs();
        visitLog.setCustomer(customer);
        visitLog.setUser(currentUser);
        visitLog.setSpaceAssigned(request.getSpaceAssigned());
        visitLog.setServiceType(request.getServiceType());
        visitLog.setPaymentType(request.getPaymentType());
        visitLog.setCheckInTime(LocalDateTime.now());
        visitLog.setAmountPaid(request.getAmountPaid() != null ? request.getAmountPaid() : BigDecimal.ZERO);

        customer.setTotalVisits((customer.getTotalVisits() != null ? customer.getTotalVisits() : 0) + 1);
        customerRepository.save(customer);

        VisitLogs savedVisit = visitLogsRepository.save(visitLog);
        return mapToResponse(savedVisit);
    }

    public VisitResponse checkIn(CheckInRequest request) {
        return checkIn(request, getCurrentUser());
    }

    /**
     * Check out a visitor.
     * Records departure timestamp, assigns checkedOutBy staff audit, and computes duration in minutes.
     */
    @Transactional
    public VisitResponse checkOut(CheckOutRequest request, User currentUser) {
        if (currentUser == null) {
            currentUser = getCurrentUser();
        }

        VisitLogs visitLog = visitLogsRepository.findById(request.getVisitLogId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit log not found"));

        if (visitLog.getCheckOutTime() != null) {
            throw new BadRequestException("Visit already checked out");
        }

        visitLog.setCheckedOutBy(currentUser);
        visitLog.setCheckOutTime(LocalDateTime.now());
        VisitLogs updatedVisit = visitLogsRepository.save(visitLog);
        return mapToResponse(updatedVisit);
    }

    public VisitResponse checkOut(CheckOutRequest request) {
        return checkOut(request, getCurrentUser());
    }

    /**
     * Update time log (Supervised correction by staff/admin).
     * Recalculates duration and attributes audit to updatedBy staff member.
     */
    @Transactional
    public VisitResponse updateTimeLog(UpdateTimeLogRequest request, User currentUser) {
        if (currentUser == null) {
            currentUser = getCurrentUser();
        }

        VisitLogs visitLog = visitLogsRepository.findById(request.getVisitLogId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit log not found"));

        if (request.getCheckInTime() != null) {
            visitLog.setCheckInTime(request.getCheckInTime());
        }

        if (request.getCheckOutTime() != null) {
            if (visitLog.getCheckInTime() != null && request.getCheckOutTime().isBefore(visitLog.getCheckInTime())) {
                throw new BadRequestException("Check-out time cannot be before check-in time");
            }
            visitLog.setCheckOutTime(request.getCheckOutTime());
            if (visitLog.getCheckedOutBy() == null) {
                visitLog.setCheckedOutBy(currentUser);
            }
        }

        visitLog.setUpdatedBy(currentUser);
        VisitLogs updatedVisit = visitLogsRepository.save(visitLog);
        return mapToResponse(updatedVisit);
    }

    public VisitResponse updateTimeLog(UpdateTimeLogRequest request) {
        return updateTimeLog(request, getCurrentUser());
    }

    public VisitResponse updateTimeLog(TimeLogUpdateRequest request) {
        return updateTimeLog(new UpdateTimeLogRequest(request.getVisitLogId(), request.getCheckInTime(), request.getCheckOutTime()));
    }

    /**
     * Get paginated active in-space visitors (checkOutTime is null).
     */
    @Transactional(readOnly = true)
    public PagedResponse<VisitResponse> getActiveVisits(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<VisitLogs> pageResult = visitLogsRepository.findByCheckOutTimeIsNullOrderByCheckInTimeDesc(pageable);
        return PagedResponse.from(pageResult.map(this::mapToResponse));
    }

    /**
     * Get list of active in-space visitors (checkOutTime is null).
     */
    @Transactional(readOnly = true)
    public List<VisitResponse> getActiveVisits() {
        List<VisitLogs> visits = visitLogsRepository.findByCheckOutTimeIsNullOrderByCheckInTimeDesc();
        return visits.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Filterable visit history with period metrics.
     */
    @Transactional(readOnly = true)
    public VisitLogsPagedResponse getAllVisits(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate date,
            Integer year,
            Integer month,
            String status,
            UUID customerId,
            String search,
            String serviceType,
            String paymentType,
            int page,
            int size
    ) {
        LocalDate resolvedStartDate = null;
        LocalDate resolvedEndDate = null;

        if (startDate != null || endDate != null) {
            resolvedStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
            resolvedEndDate = endDate != null ? endDate : LocalDate.now();
            if (resolvedStartDate.isAfter(resolvedEndDate)) {
                throw new BadRequestException("Start date cannot be after end date");
            }
        } else if (date != null) {
            resolvedStartDate = date;
            resolvedEndDate = date;
        } else if (year != null || month != null) {
            int resolvedYear = year != null ? year : LocalDate.now().getYear();
            int resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();
            if (resolvedMonth < 1 || resolvedMonth > 12) {
                throw new BadRequestException("Month must be between 1 and 12");
            }
            YearMonth ym = YearMonth.of(resolvedYear, resolvedMonth);
            resolvedStartDate = ym.atDay(1);
            resolvedEndDate = ym.atEndOfMonth();
        }

        LocalDateTime startDateTime = resolvedStartDate != null ? resolvedStartDate.atStartOfDay() : null;
        LocalDateTime endDateTime = resolvedEndDate != null ? resolvedEndDate.atTime(LocalTime.MAX) : null;
        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, VisitLogsSpecification.sortByCheckInTimeDesc());

        Specification<VisitLogs> spec = VisitLogsSpecification.filterVisits(
                startDateTime,
                endDateTime,
                customerId,
                normalizedStatus,
                search,
                serviceType,
                paymentType
        );

        Page<VisitLogs> pageResult = visitLogsRepository.findAll(spec, pageable);

        // Compute metrics for the active filter window or default to current month
        LocalDateTime metricsStart = startDateTime != null
                ? startDateTime
                : YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime metricsEnd = endDateTime != null
                ? endDateTime
                : YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

        VisitMetrics metrics = computeMetrics(metricsStart, metricsEnd);

        return VisitLogsPagedResponse.from(
                pageResult.map(this::mapToResponse),
                metrics.totalVisits(),
                metrics.activeVisits(),
                metrics.completedVisits(),
                metrics.returningCustomers()
        );
    }

    public VisitLogsPagedResponse getAllVisits(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate date,
            Integer year,
            Integer month,
            String status,
            UUID customerId,
            int page,
            int size
    ) {
        return getAllVisits(startDate, endDate, date, year, month, status, customerId, null, null, null, page, size);
    }

    public VisitLogsPagedResponse getAllVisits(int page, int size) {
        return getAllVisits(null, null, null, null, null, null, null, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> getAllVisits() {
        List<VisitLogs> visits = visitLogsRepository.findAllByOrderByCheckInTimeDesc();
        return visits.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * High-level operational visit metrics and KPI breakdown.
     */
    @Transactional(readOnly = true)
    public VisitMetricsResponse getVisitMetrics(LocalDate date, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        LocalDate resolvedStartDate;
        LocalDate resolvedEndDate;
        LocalDate reportDate = null;
        int resolvedYear;
        int resolvedMonth;

        if (startDate != null || endDate != null) {
            resolvedStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
            resolvedEndDate = endDate != null ? endDate : LocalDate.now();
            if (resolvedStartDate.isAfter(resolvedEndDate)) {
                throw new BadRequestException("Start date cannot be after end date");
            }
            reportDate = resolvedStartDate.equals(resolvedEndDate) ? resolvedStartDate : null;
            resolvedYear = resolvedStartDate.getYear();
            resolvedMonth = resolvedStartDate.getMonthValue();
        } else if (date != null) {
            resolvedStartDate = date;
            resolvedEndDate = date;
            reportDate = date;
            resolvedYear = date.getYear();
            resolvedMonth = date.getMonthValue();
        } else if (year != null || month != null) {
            resolvedYear = year != null ? year : LocalDate.now().getYear();
            resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();
            if (resolvedMonth < 1 || resolvedMonth > 12) {
                throw new BadRequestException("Month must be between 1 and 12");
            }
            YearMonth ym = YearMonth.of(resolvedYear, resolvedMonth);
            resolvedStartDate = ym.atDay(1);
            resolvedEndDate = ym.atEndOfMonth();
        } else {
            LocalDate today = LocalDate.now();
            resolvedStartDate = today;
            resolvedEndDate = today;
            reportDate = today;
            resolvedYear = today.getYear();
            resolvedMonth = today.getMonthValue();
        }

        LocalDateTime startDateTime = resolvedStartDate.atStartOfDay();
        LocalDateTime endDateTime = resolvedEndDate.atTime(LocalTime.MAX);
        VisitMetrics periodMetrics = computeMetrics(startDateTime, endDateTime);

        // Returning and new customers for that month
        YearMonth targetMonth = YearMonth.of(resolvedYear, resolvedMonth);
        LocalDateTime startOfMonth = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetMonth.atEndOfMonth().atTime(LocalTime.MAX);
        VisitMetrics monthMetrics = computeMetrics(startOfMonth, endOfMonth);

        return new VisitMetricsResponse(
                reportDate,
                resolvedStartDate,
                resolvedEndDate,
                resolvedYear,
                resolvedMonth,
                periodMetrics.totalVisits(),
                periodMetrics.activeVisits(),
                periodMetrics.completedVisits(),
                monthMetrics.returningCustomers(),
                monthMetrics.newCustomers()
        );
    }

    public VisitMetricsResponse getVisitMetrics() {
        return getVisitMetrics(null, null, null, null, null);
    }

    /**
     * Today's desk operations summary (in-space count, today check-ins, today revenue).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayMetrics() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        long activeVisitors = visitLogsRepository.countByCheckOutTimeIsNull();
        long todayCheckIns = visitLogsRepository.countByCheckInTimeBetween(start, end);
        BigDecimal todayRevenue = visitLogsRepository.sumAmountPaidBetween(start, end);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("date", today);
        metrics.put("activeVisitors", activeVisitors);
        metrics.put("todayCheckIns", todayCheckIns);
        metrics.put("todayRevenue", todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
        return metrics;
    }

    public List<EnumOptionResponse> getServiceTypes() {
        if (serviceTypeRepository != null) {
            List<ServiceTypeEntity> entities = serviceTypeRepository.findAllByOrderByNameAsc();
            if (entities != null && !entities.isEmpty()) {
                return entities.stream()
                        .map(st -> new EnumOptionResponse(st.getName(), st.getDisplayName(), st.isDefault()))
                        .toList();
            }
        }
        return Arrays.stream(ServiceType.values())
                .map(st -> new EnumOptionResponse(st.name(), st.getDisplayName(), true))
                .toList();
    }

    public List<EnumOptionResponse> getPaymentTypes() {
        if (paymentTypeRepository != null) {
            List<PaymentTypeEntity> entities = paymentTypeRepository.findAllByOrderByNameAsc();
            if (entities != null && !entities.isEmpty()) {
                return entities.stream()
                        .map(pt -> new EnumOptionResponse(pt.getName(), pt.getDisplayName(), pt.isDefault()))
                        .toList();
            }
        }
        return Arrays.stream(PaymentType.values())
                .map(pt -> new EnumOptionResponse(pt.name(), pt.getDisplayName(), true))
                .toList();
    }

    public List<EnumOptionResponse> getVisitStatuses() {
        return Arrays.stream(VisitStatus.values())
                .map(vs -> new EnumOptionResponse(vs.name(), vs.getDisplayName(), true))
                .toList();
    }

    public VisitMetadataResponse getVisitMetadata() {
        return new VisitMetadataResponse(
                getServiceTypes(),
                getPaymentTypes(),
                getVisitStatuses()
        );
    }

    private VisitMetrics computeMetrics(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<VisitLogs> visits = visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(startDateTime, endDateTime);

        long totalVisits = visits.size();
        long activeVisits = visits.stream().filter(v -> v.getCheckOutTime() == null).count();
        long completedVisits = visits.stream().filter(v -> v.getCheckOutTime() != null).count();

        Map<UUID, Long> visitsByCustomerInPeriod = visits.stream()
                .filter(v -> v.getCustomer() != null && v.getCustomer().getId() != null)
                .collect(Collectors.groupingBy(v -> v.getCustomer().getId(), Collectors.counting()));

        long returningCustomers = 0;
        long newCustomers = 0;

        for (Map.Entry<UUID, Long> entry : visitsByCustomerInPeriod.entrySet()) {
            UUID custId = entry.getKey();
            long periodCount = entry.getValue();

            Optional<Customers> custOpt = customerRepository.findById(custId);
            boolean isReturning = false;
            if (custOpt.isPresent()) {
                Customers c = custOpt.get();
                Integer totalLifetimeVisits = c.getTotalVisits();
                if (totalLifetimeVisits != null && totalLifetimeVisits > 1) {
                    isReturning = true;
                } else if (periodCount > 1) {
                    isReturning = true;
                }
            } else if (periodCount > 1) {
                isReturning = true;
            }

            if (isReturning) {
                returningCustomers++;
            } else {
                newCustomers++;
            }
        }

        return new VisitMetrics(totalVisits, activeVisits, completedVisits, returningCustomers, newCustomers);
    }

    private record VisitMetrics(
            long totalVisits,
            long activeVisits,
            long completedVisits,
            long returningCustomers,
            long newCustomers
    ) {}

    private VisitResponse mapToResponse(VisitLogs visitLog) {
        return VisitResponse.from(visitLog);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}

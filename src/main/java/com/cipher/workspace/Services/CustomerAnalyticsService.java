package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.CustomerAnalyticsResponse;
import com.cipher.workspace.DTOs.CustomerVisitAnalyticsDto;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Entity.Customers;
import com.cipher.workspace.Entity.VisitLogs;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.CustomerRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.springframework.beans.factory.annotation.Value;
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
public class CustomerAnalyticsService {

    private final CustomerRepository customerRepository;
    private final VisitLogsRepository visitLogsRepository;
    private final int defaultThreshold;

    public CustomerAnalyticsService(
            CustomerRepository customerRepository,
            VisitLogsRepository visitLogsRepository,
            @Value("${app.analytics.regular-threshold:5}") int defaultThreshold
    ) {
        this.customerRepository = customerRepository;
        this.visitLogsRepository = visitLogsRepository;
        this.defaultThreshold = defaultThreshold;
    }

    @Transactional(readOnly = true)
    public CustomerAnalyticsResponse getCustomerAnalytics(
            Integer year,
            Integer month,
            Integer threshold,
            Boolean onlyRegulars,
            String search
    ) {
        return getCustomerAnalytics(year, month, threshold, onlyRegulars, search, null, null, null);
    }

    @Transactional(readOnly = true)
    public CustomerAnalyticsResponse getCustomerAnalytics(
            Integer year,
            Integer month,
            Integer threshold,
            Boolean onlyRegulars,
            String search,
            Integer page,
            Integer size
    ) {
        return getCustomerAnalytics(year, month, threshold, onlyRegulars, search, null, page, size);
    }

    @Transactional(readOnly = true)
    public CustomerAnalyticsResponse getCustomerAnalytics(
            Integer year,
            Integer month,
            Integer threshold,
            Boolean onlyRegulars,
            String search,
            String name,
            Integer page,
            Integer size
    ) {
        YearMonth targetPeriod = resolveYearMonth(year, month);
        int resolvedYear = targetPeriod.getYear();
        int resolvedMonth = targetPeriod.getMonthValue();
        int effectiveThreshold = (threshold != null && threshold > 0) ? threshold : defaultThreshold;

        LocalDateTime startOfMonth = targetPeriod.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetPeriod.atEndOfMonth().atTime(LocalTime.MAX);

        List<VisitLogs> monthVisits = visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(startOfMonth, endOfMonth);

        Map<UUID, Long> monthlyCounts = monthVisits.stream()
                .filter(v -> v.getCustomer() != null && v.getCustomer().getId() != null)
                .collect(Collectors.groupingBy(v -> v.getCustomer().getId(), Collectors.counting()));

        List<Customers> allCustomers = customerRepository.findAll();
        long totalCustomersCount = allCustomers.size();
        long regularCustomersCount = allCustomers.stream()
                .filter(c -> monthlyCounts.getOrDefault(c.getId(), 0L) >= effectiveThreshold)
                .count();
        long occasionalCustomersCount = totalCustomersCount - regularCustomersCount;
        double regularPercentage = totalCustomersCount > 0
                ? Math.round(((double) regularCustomersCount / totalCustomersCount) * 10000.0) / 100.0
                : 0.0;

        List<Customers> targetCustomers;
        if (name != null && !name.isBlank()) {
            targetCustomers = customerRepository.findByFullnameContainingIgnoreCase(name.trim());
        } else if (search != null && !search.isBlank()) {
            targetCustomers = customerRepository.searchByNameOrPhone(search.trim());
        } else {
            targetCustomers = allCustomers;
        }

        List<CustomerVisitAnalyticsDto> customerDtos = targetCustomers.stream()
                .map(customer -> buildCustomerDto(customer, monthlyCounts.getOrDefault(customer.getId(), 0L).intValue(), effectiveThreshold))
                .filter(dto -> onlyRegulars == null || !onlyRegulars || dto.isRegular())
                .sorted(Comparator.comparingInt(CustomerVisitAnalyticsDto::monthlyVisits).reversed()
                        .thenComparing(Comparator.comparingInt(CustomerVisitAnalyticsDto::totalVisits).reversed()))
                .collect(Collectors.toList());

        if (page != null && size != null && size > 0) {
            int pageNumber = Math.max(0, page);
            int pageSize = Math.min(100, Math.max(1, size));
            int fromIndex = Math.min(pageNumber * pageSize, customerDtos.size());
            int toIndex = Math.min(fromIndex + pageSize, customerDtos.size());
            customerDtos = customerDtos.subList(fromIndex, toIndex);
        }

        return new CustomerAnalyticsResponse(
                totalCustomersCount,
                regularCustomersCount,
                occasionalCustomersCount,
                effectiveThreshold,
                regularPercentage,
                resolvedYear,
                resolvedMonth,
                customerDtos
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerVisitAnalyticsDto> getRegularCustomers(Integer year, Integer month, Integer threshold, int page, int size) {
        List<CustomerVisitAnalyticsDto> regulars = getRegularCustomers(year, month, threshold);
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        int fromIndex = Math.min(pageNumber * pageSize, regulars.size());
        int toIndex = Math.min(fromIndex + pageSize, regulars.size());
        List<CustomerVisitAnalyticsDto> pageContent = regulars.subList(fromIndex, toIndex);
        return PagedResponse.of(pageContent, pageNumber, pageSize, regulars.size());
    }

    @Transactional(readOnly = true)
    public List<CustomerVisitAnalyticsDto> getRegularCustomers(Integer year, Integer month, Integer threshold) {
        YearMonth targetPeriod = resolveYearMonth(year, month);
        int effectiveThreshold = (threshold != null && threshold > 0) ? threshold : defaultThreshold;

        LocalDateTime startOfMonth = targetPeriod.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetPeriod.atEndOfMonth().atTime(LocalTime.MAX);

        List<VisitLogs> monthVisits = visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(startOfMonth, endOfMonth);

        Map<UUID, Long> monthlyCounts = monthVisits.stream()
                .filter(v -> v.getCustomer() != null && v.getCustomer().getId() != null)
                .collect(Collectors.groupingBy(v -> v.getCustomer().getId(), Collectors.counting()));

        List<Customers> allCustomers = customerRepository.findAll();

        return allCustomers.stream()
                .filter(customer -> monthlyCounts.getOrDefault(customer.getId(), 0L) >= effectiveThreshold)
                .map(customer -> buildCustomerDto(customer, monthlyCounts.getOrDefault(customer.getId(), 0L).intValue(), effectiveThreshold))
                .sorted(Comparator.comparingInt(CustomerVisitAnalyticsDto::monthlyVisits).reversed()
                        .thenComparing(Comparator.comparingInt(CustomerVisitAnalyticsDto::totalVisits).reversed()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerVisitAnalyticsDto getCustomerDetails(UUID customerId, Integer year, Integer month, Integer threshold) {
        YearMonth targetPeriod = resolveYearMonth(year, month);
        int effectiveThreshold = (threshold != null && threshold > 0) ? threshold : defaultThreshold;

        Customers customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        LocalDateTime startOfMonth = targetPeriod.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetPeriod.atEndOfMonth().atTime(LocalTime.MAX);

        List<VisitLogs> visits = visitLogsRepository.findByCustomerIdOrderByCheckInTimeAsc(customer.getId());

        int monthlyVisits = (int) visits.stream()
                .filter(v -> v.getCheckInTime() != null
                        && !v.getCheckInTime().isBefore(startOfMonth)
                        && !v.getCheckInTime().isAfter(endOfMonth))
                .count();

        return buildCustomerDto(customer, monthlyVisits, effectiveThreshold, visits);
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (month != null && (month < 1 || month > 12)) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        int resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();
        return YearMonth.of(resolvedYear, resolvedMonth);
    }

    private CustomerVisitAnalyticsDto buildCustomerDto(Customers customer, int monthlyVisits, int threshold) {
        List<VisitLogs> visits = visitLogsRepository.findByCustomerIdOrderByCheckInTimeAsc(customer.getId());
        return buildCustomerDto(customer, monthlyVisits, threshold, visits);
    }

    private CustomerVisitAnalyticsDto buildCustomerDto(Customers customer, int monthlyVisits, int threshold, List<VisitLogs> visits) {
        int totalVisits = customer.getTotalVisits() != null ? customer.getTotalVisits() : 0;
        boolean isRegular = monthlyVisits >= threshold;

        String customerTag;
        if (isRegular) {
            customerTag = "REGULAR";
        } else if (totalVisits == 1) {
            customerTag = "NEW";
        } else if (totalVisits == 0) {
            customerTag = "NO_VISITS";
        } else {
            customerTag = "OCCASIONAL";
        }

        BigDecimal totalSpent = BigDecimal.ZERO;
        LocalDateTime firstVisit = null;
        LocalDateTime lastVisit = null;
        Map<String, Long> serviceCounts = new HashMap<>();

        if (visits != null && !visits.isEmpty()) {
            firstVisit = visits.get(0).getCheckInTime();
            lastVisit = visits.get(visits.size() - 1).getCheckInTime();

            for (VisitLogs visit : visits) {
                if (visit.getAmountPaid() != null) {
                    totalSpent = totalSpent.add(visit.getAmountPaid());
                }
                if (visit.getServiceType() != null && !visit.getServiceType().isBlank()) {
                    String service = visit.getServiceType().trim().toUpperCase().replace(' ', '_');
                    serviceCounts.put(service, serviceCounts.getOrDefault(service, 0L) + 1L);
                }
            }
        }

        String preferredServiceType = serviceCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new CustomerVisitAnalyticsDto(
                customer.getId(),
                customer.getFullname(),
                customer.getPhoneNumber(),
                totalVisits,
                monthlyVisits,
                isRegular,
                customerTag,
                totalSpent,
                firstVisit,
                lastVisit,
                preferredServiceType
        );
    }
}

package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.FinancialReportResponse;
import com.cipher.workspace.Entity.Expenses;
import com.cipher.workspace.Entity.FinancialStatus;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.VisitLogs;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Repository.ExpensesRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FinancialReportService {

    private final VisitLogsRepository visitLogsRepository;
    private final ExpensesRepository expensesRepository;

    public FinancialReportService(VisitLogsRepository visitLogsRepository, ExpensesRepository expensesRepository) {
        this.visitLogsRepository = visitLogsRepository;
        this.expensesRepository = expensesRepository;
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse generateReport(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate resolvedEndDate = endDate != null ? endDate : LocalDate.now();

        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        LocalDateTime startDateTime = resolvedStartDate.atStartOfDay();
        LocalDateTime endDateTime = resolvedEndDate.atTime(LocalTime.MAX);

        User currentStaff = getCurrentUser();

        List<VisitLogs> visits = visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(startDateTime, endDateTime);
        List<Expenses> expenses = expensesRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(resolvedStartDate, resolvedEndDate);

        BigDecimal totalRevenue = visits.stream()
                .map(v -> v.getAmountPaid() != null ? v.getAmountPaid() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenses.stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfitOrLoss = totalRevenue.subtract(totalExpenses);

        FinancialStatus status;
        if (netProfitOrLoss.compareTo(BigDecimal.ZERO) > 0) {
            status = FinancialStatus.PROFIT;
        } else if (netProfitOrLoss.compareTo(BigDecimal.ZERO) < 0) {
            status = FinancialStatus.LOSS;
        } else {
            status = FinancialStatus.BREAK_EVEN;
        }

        Map<String, BigDecimal> revenueByServiceType = new LinkedHashMap<>();
        for (VisitLogs visit : visits) {
            String service = visit.getServiceType() != null ? visit.getServiceType() : "OTHER";
            BigDecimal current = revenueByServiceType.getOrDefault(service, BigDecimal.ZERO);
            BigDecimal amount = visit.getAmountPaid() != null ? visit.getAmountPaid() : BigDecimal.ZERO;
            revenueByServiceType.put(service, current.add(amount));
        }

        Map<String, BigDecimal> expensesByCategory = new LinkedHashMap<>();
        for (Expenses expense : expenses) {
            String category = expense.getCategory() != null ? expense.getCategory() : "OTHER";
            BigDecimal current = expensesByCategory.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal amount = expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO;
            expensesByCategory.put(category, current.add(amount));
        }

        UUID staffId = currentStaff != null ? currentStaff.getId() : null;
        String staffName = currentStaff != null ? currentStaff.getFullname() : null;

        return new FinancialReportResponse(
                resolvedStartDate,
                resolvedEndDate,
                totalRevenue,
                totalExpenses,
                netProfitOrLoss,
                status,
                visits.size(),
                expenses.size(),
                revenueByServiceType,
                expensesByCategory,
                LocalDateTime.now(),
                staffId,
                staffName
        );
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse generateMonthlyReport(Integer year, Integer month) {
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        int resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        YearMonth ym = YearMonth.of(resolvedYear, resolvedMonth);
        return generateReport(ym.atDay(1), ym.atEndOfMonth());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}

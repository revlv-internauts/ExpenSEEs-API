package com.example.expensetracker.services.stats;

import com.example.expensetracker.dto.GraphDTO;
import com.example.expensetracker.dto.StatsDTO;
import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.entity.Income;
import com.example.expensetracker.repository.ExpenseRepository;
import com.example.expensetracker.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public GraphDTO getChartData(int days, LocalDate startDate, LocalDate endDate) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        if (startDate == null || endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(days);
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        GraphDTO graphDTO = new GraphDTO();
        graphDTO.setExpenseList(expenseRepository.findByDateBetween(startDate, endDate));
        graphDTO.setIncomeList(incomeRepository.findByDateBetween(startDate, endDate));
        return graphDTO;
    }

    @Override
    public StatsDTO getStats() {
        StatsDTO statsDTO = new StatsDTO();

        // Handle null sums
        Double totalIncome = incomeRepository.sumAllAmounts();
        Double totalExpense = expenseRepository.sumAllAmounts();
        statsDTO.setIncome(totalIncome != null ? totalIncome : 0.0);
        statsDTO.setExpense(totalExpense != null ? totalExpense : 0.0);
        statsDTO.setBalance(statsDTO.getIncome() - statsDTO.getExpense());

        // Handle latest records
        Optional<Income> optionalIncome = incomeRepository.findFirstByOrderByDateDesc();
        Optional<Expense> optionalExpense = expenseRepository.findFirstByOrderByDateDesc();
        optionalIncome.ifPresent(statsDTO::setLatestIncome);
        optionalExpense.ifPresent(statsDTO::setLatestExpense);

        // Handle min/max calculations
        List<Income> incomeList = incomeRepository.findAll();
        List<Expense> expenseList = expenseRepository.findAll();

        OptionalDouble minIncome = incomeList.stream().mapToDouble(Income::getAmount).min();
        OptionalDouble maxIncome = incomeList.stream().mapToDouble(Income::getAmount).max();
        OptionalDouble minExpense = expenseList.stream().mapToDouble(Expense::getAmount).min();
        OptionalDouble maxExpense = expenseList.stream().mapToDouble(Expense::getAmount).max();

        statsDTO.setMinIncome(minIncome.isPresent() ? minIncome.getAsDouble() : null);
        statsDTO.setMaxIncome(maxIncome.isPresent() ? maxIncome.getAsDouble() : null);
        statsDTO.setMinExpense(minExpense.isPresent() ? minExpense.getAsDouble() : null);
        statsDTO.setMaxExpense(maxExpense.isPresent() ? maxExpense.getAsDouble() : null);

        return statsDTO;
    }
}
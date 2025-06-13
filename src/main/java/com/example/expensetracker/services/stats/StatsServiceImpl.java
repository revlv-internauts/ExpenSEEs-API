package com.example.expensetracker.services.stats;

import com.example.expensetracker.dto.GraphDTO;
import com.example.expensetracker.dto.StatsDTO;
import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.entity.Income;
import com.example.expensetracker.entity.User;
import com.example.expensetracker.repository.ExpenseRepository;
import com.example.expensetracker.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    public GraphDTO getChartData(User user) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(27);

        GraphDTO graphDTO = new GraphDTO();
        graphDTO.setExpenseList(expenseRepository.findByDateBetweenAndUser(startDate, endDate, user));
        graphDTO.setIncomeList(incomeRepository.findByDateBetweenAndUser(startDate, endDate, user));

        return graphDTO;
    }

    public StatsDTO getStats(User user) {
        Double totalIncome = incomeRepository.sumAllAmountsByUser(user);
        Double totalExpense = expenseRepository.sumAllAmountsByUser(user);

        Optional<Income> optionalIncome = incomeRepository.findFirstByUserOrderByDateDesc(user);
        Optional<Expense> optionalExpense = expenseRepository.findFirstByUserOrderByDateDesc(user);

        StatsDTO statsDTO = new StatsDTO();
        statsDTO.setExpense(totalExpense != null ? totalExpense : 0.0);
        statsDTO.setIncome(totalIncome != null ? totalIncome : 0.0);

        optionalIncome.ifPresent(income -> statsDTO.setLatestIncome(income));
        optionalExpense.ifPresent(expense -> statsDTO.setLatestExpense(expense));

        statsDTO.setBalance((totalIncome != null ? totalIncome : 0.0) - (totalExpense != null ? totalExpense : 0.0));

        List<Income> incomeList = incomeRepository.findByUser(user);
        List<Expense> expenseList = expenseRepository.findByUser(user);

        OptionalDouble minIncome = incomeList.stream().mapToDouble(Income::getAmount).min();
        OptionalDouble maxIncome = incomeList.stream().mapToDouble(Income::getAmount).max();

        OptionalDouble minExpense = expenseList.stream().mapToDouble(Expense::getAmount).min();
        OptionalDouble maxExpense = expenseList.stream().mapToDouble(Expense::getAmount).max();

        statsDTO.setMaxExpense(maxExpense.isPresent() ? maxExpense.getAsDouble() : null);
        statsDTO.setMinExpense(minExpense.isPresent() ? minExpense.getAsDouble() : null);
        statsDTO.setMaxIncome(maxIncome.isPresent() ? maxIncome.getAsDouble() : null);
        statsDTO.setMinIncome(minIncome.isPresent() ? minIncome.getAsDouble() : null);

        return statsDTO;
    }
}
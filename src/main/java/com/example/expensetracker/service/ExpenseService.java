package com.example.expensetracker.service;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.dto.ExpenseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    public Expense addExpense(ExpenseDto expenseDto) {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User not found: " + username);

        Double amount = expenseDto.calculateTotal();
        if (amount != null && amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        Expense expense = new Expense();
        expense.setCategory(expenseDto.getCategory());
        expense.setAmount(amount);
        expense.setRemarks(expenseDto.getRemarks());
        expense.setDateOfTransaction(expenseDto.getDateOfTransaction() != null ? expenseDto.getDateOfTransaction() : LocalDate.now());
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setUser(user);
        return expenseRepository.save(expense);
    }

    public List<Expense> getAllExpenses() {
        User user = userRepository.findByUsername(getCurrentUsername());
        return expenseRepository.findAllByUser(user);
    }

    public double getTotalExpenseAmount() {
        User user = userRepository.findByUsername(getCurrentUsername());
        return expenseRepository.findAllByUser(user)
                .stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    @Transactional
    public Expense updateExpense(Long expenseId, ExpenseDto expenseDto) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        Double amount = expenseDto.calculateTotal();
        if (amount != null && amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        expense.setCategory(expenseDto.getCategory());
        expense.setAmount(amount);
        expense.setRemarks(expenseDto.getRemarks());
        expense.setDateOfTransaction(expenseDto.getDateOfTransaction() != null ? expenseDto.getDateOfTransaction() : LocalDate.now());
        expense.setUpdatedAt(LocalDateTime.now());
        return expenseRepository.save(expense);
    }

    public Map<String, Double> getExpenseDistributionByCategory() {
        User user = userRepository.findByUsername(getCurrentUsername());
        Map<String, Double> distribution = new HashMap<>();
        expenseRepository.findAllByUser(user).forEach(expense ->
                distribution.merge(expense.getCategory(), expense.getAmount(), Double::sum));
        return distribution;
    }

    public List<Expense> getRecentTransactions(int limit) {
        User user = userRepository.findByUsername(getCurrentUsername());
        return expenseRepository.findAllByUser(user).stream()
                .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
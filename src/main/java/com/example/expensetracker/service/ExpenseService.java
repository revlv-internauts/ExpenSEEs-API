package com.example.expensetracker.service;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.LiquidationExpenseItem;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.Repository.LiquidationExpenseItemRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.exception.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    private LiquidationExpenseItemRepository liquidationExpenseItemRepository;

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

    public List<Expense> getAllExpenses(String sortBy, String sortOrder) {
        User user = userRepository.findByUsername(getCurrentUsername());
        String sortField;
        switch (sortBy.toLowerCase()) {
            case "date":
                sortField = "createdAt";
                break;
            case "amount":
                sortField = "amount";
                break;
            case "category":
                sortField = "category";
                break;
            case "user":
                sortField = "user.username";
                break;
            default:
                sortField = "createdAt"; // Default sort
        }

        Sort sort = Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        return expenseRepository.findAllByUser(user, sort);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Expense> getAllExpensesForAdmin(String sortBy, String sortOrder) {
        String sortField;
        switch (sortBy.toLowerCase()) {
            case "date":
                sortField = "createdAt";
                break;
            case "amount":
                sortField = "amount";
                break;
            case "category":
                sortField = "category";
                break;
            case "user":
                sortField = "user.username";
                break;
            default:
                sortField = "createdAt"; // Default sort
        }

        Sort sort = Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        return expenseRepository.findAll(sort);
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
        checkExpenseOwnership(expense);

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

    public Expense getExpenseById(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + expenseId));
        String currentUsername = getCurrentUsername();
        User currentUser = userRepository.findByUsername(currentUsername);
        if (!expense.getUser().getUserId().equals(currentUser.getUserId()) &&
                !SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedAccessException("Unauthorized access to expense");
        }
        return expense;
    }

    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        checkExpenseOwnership(expense);
        expenseRepository.deleteById(expenseId);
    }

    public void deleteExpenses(List<Long> expenseIds) {
        for (Long expenseId : expenseIds) {
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + expenseId));
            checkExpenseOwnership(expense);
        }
        expenseRepository.deleteAllById(expenseIds);
    }

    public boolean isExpenseAddedToLiquidation(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + expenseId));
        // Check if expense is referenced in any LiquidationExpenseItem by matching amount and category
        List<LiquidationExpenseItem> liquidationItems = liquidationExpenseItemRepository.findAll();
        return liquidationItems.stream().anyMatch(item ->
                item.getAmount().equals(expense.getAmount()) &&
                        item.getCategory().equals(expense.getCategory()) &&
                        item.getBudget().getUser().getUserId().equals(expense.getUser().getUserId())
        );
    }

    private void checkExpenseOwnership(Expense expense) {
        String username = getCurrentUsername();
        if (!expense.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("Unauthorized access to expense");
        }
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }
}

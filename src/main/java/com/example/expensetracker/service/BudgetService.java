package com.example.expensetracker.service;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.ExpenseItem;
import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.dto.BudgetDto;
import com.example.expensetracker.Repository.BudgetRepository;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.exception.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    public SubmittedBudget createBudget(SubmittedBudget budget) {
        if (budget == null || budget.getName() == null || budget.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Budget name cannot be empty");
        }
        if (budget.getBudgetDate() == null) {
            throw new IllegalArgumentException("Budget date is required");
        }
        if (budget.getExpenses() == null || budget.getExpenses().isEmpty()) {
            throw new IllegalArgumentException("Budget must include at least one expense item");
        }

        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        budget.setUser(user);
        double total = budget.getExpenses().stream()
                .mapToDouble(item -> {
                    if (item.getQuantity() <= 0 || item.getAmountPerUnit() <= 0) {
                        throw new IllegalArgumentException("Expense item quantity and amount per unit must be positive");
                    }
                    return item.getQuantity() * item.getAmountPerUnit();
                })
                .sum();
        budget.setTotal(total);
        budget.setStatus(SubmittedBudget.Status.PENDING);
        budget.setExpenses(budget.getExpenses().stream()
                .peek(item -> item.setBudget(budget))
                .toList());
        return budgetRepository.save(budget);
    }

    public List<BudgetDto> getAllBudgets(String sortBy, String sortOrder) {
        User user = userRepository.findByUsername(getCurrentUsername());
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        String sortField;
        switch (sortBy.toLowerCase()) {
            case "date":
                sortField = "createdAt";
                break;
            case "username":
                sortField = "user.username";
                break;
            case "name":
                sortField = "name";
                break;
            case "amount":
                sortField = "total";
                break;
            case "status":
                sortField = "status";
                break;
            default:
                sortField = "createdAt";
        }

        Sort sort = Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        List<SubmittedBudget> budgets = budgetRepository.findAll(sort);

        if (!isAdmin) {
            budgets = budgets.stream()
                    .filter(budget -> budget.getUser().getUserId().equals(user.getUserId()))
                    .toList();
        }

        return budgets.stream().map(budget -> {
            BudgetDto dto = new BudgetDto();
            dto.setBudgetId(budget.getBudgetId());
            dto.setName(budget.getName());
            dto.setBudgetDate(budget.getBudgetDate());
            dto.setTotal(budget.getTotal());
            dto.setStatus(budget.getStatus());
            dto.setRemarks(budget.getRemarks());
            dto.setCreatedAt(budget.getCreatedAt());
            dto.setUsername(budget.getUser() != null ? budget.getUser().getUsername() : null);
            dto.setUserId(budget.getUser() != null ? budget.getUser().getUserId() : null);
            dto.setLiquidationIds(budget.getLiquidations() != null
                    ? budget.getLiquidations().stream()
                    .map(liquidation -> liquidation.getLiquidationId())
                    .collect(Collectors.toList())
                    : List.of());
            return dto;
        }).collect(Collectors.toList());
    }

    public ResponseEntity<SubmittedBudget> getBudgetById(Long budgetId) {
        if (budgetId == null) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        User currentUser = userRepository.findByUsername(getCurrentUsername());
        if (currentUser == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        if (budgetRepository.findById(budgetId).isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        SubmittedBudget budget = budgetRepository.findById(budgetId).get();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !budget.getUser().getUserId().equals(currentUser.getUserId())) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(budget, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<String> updateBudgetStatus(Long budgetId, SubmittedBudget.Status status, String remarks) {
        if (budgetId == null) {
            return new ResponseEntity<>("Budget ID is required", HttpStatus.BAD_REQUEST);
        }
        User currentUser = userRepository.findByUsername(getCurrentUsername());
        if (currentUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return new ResponseEntity<>("Only admins can update budget status", HttpStatus.FORBIDDEN);
        }
        if (budgetRepository.findById(budgetId).isEmpty()) {
            return new ResponseEntity<>("Budget not found", HttpStatus.NOT_FOUND);
        }
        SubmittedBudget budget = budgetRepository.findById(budgetId).get();
        budget.setStatus(status);
        budget.setRemarks(remarks);
        budgetRepository.save(budget);
        return new ResponseEntity<>("Status updated successfully", HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<String> associateExpenseWithBudget(Long budgetId, Long expenseId) {
        if (budgetId == null || expenseId == null) {
            return new ResponseEntity<>("Budget ID and Expense ID are required", HttpStatus.BAD_REQUEST);
        }
        User currentUser = userRepository.findByUsername(getCurrentUsername());
        if (currentUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (budgetRepository.findById(budgetId).isEmpty()) {
            return new ResponseEntity<>("Budget not found", HttpStatus.NOT_FOUND);
        }
        SubmittedBudget budget = budgetRepository.findById(budgetId).get();
        if (!budget.getUser().getUserId().equals(currentUser.getUserId())) {
            return new ResponseEntity<>("Unauthorized access to budget", HttpStatus.FORBIDDEN);
        }
        if (expenseRepository.findById(expenseId).isEmpty()) {
            return new ResponseEntity<>("Expense not found", HttpStatus.NOT_FOUND);
        }
        Expense expense = expenseRepository.findById(expenseId).get();
        if (!expense.getUser().getUserId().equals(currentUser.getUserId())) {
            return new ResponseEntity<>("Unauthorized access to expense", HttpStatus.FORBIDDEN);
        }
        ExpenseItem item = new ExpenseItem();
        item.setCategory(expense.getCategory());
        item.setAmountPerUnit(expense.getAmount());
        item.setQuantity(1);
        item.setBudget(budget);
        budget.getExpenses().add(item);
        budget.setTotal(budget.getTotal() + expense.getAmount());
        budgetRepository.save(budget);
        return new ResponseEntity<>("Expense associated with budget successfully", HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<String> deleteBudget(Long budgetId) {
        if (budgetId == null) {
            return new ResponseEntity<>("Budget ID is required", HttpStatus.BAD_REQUEST);
        }
        User currentUser = userRepository.findByUsername(getCurrentUsername());
        if (currentUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (budgetRepository.findById(budgetId).isEmpty()) {
            return new ResponseEntity<>("Budget not found", HttpStatus.NOT_FOUND);
        }
        SubmittedBudget budget = budgetRepository.findById(budgetId).get();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return new ResponseEntity<>("Only admins can delete budgets", HttpStatus.FORBIDDEN);
        }
        budgetRepository.delete(budget);
        return new ResponseEntity<>("Budget deleted successfully", HttpStatus.OK);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
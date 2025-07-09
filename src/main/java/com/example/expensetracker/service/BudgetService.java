package com.example.expensetracker.service;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.ExpenseItem;
import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.BudgetRepository;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.exception.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.List;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    public SubmittedBudget createBudget(SubmittedBudget budget) {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User not found: " + username);

        budget.setUser(user);
        budget.setTotal(budget.getExpenses().stream()
                .mapToDouble(item -> item.getQuantity() * item.getAmountPerUnit())
                .sum());
        budget.setStatus(SubmittedBudget.Status.PENDING);
        budget.setExpenses(budget.getExpenses().stream()
                .peek(item -> item.setBudget(budget))
                .toList());
        return budgetRepository.save(budget);
    }

    public List<SubmittedBudget> getAllBudgets() {
        User user = userRepository.findByUsername(getCurrentUsername());
        if (user == null) throw new UsernameNotFoundException("User not found");
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return budgetRepository.findAll();
        }
        return budgetRepository.findAll().stream()
                .filter(budget -> budget.getUser().getUserId().equals(user.getUserId()))
                .toList();
    }

    @Transactional
    public ResponseEntity<String> updateBudgetStatus(Long budgetId, SubmittedBudget.Status status) {
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
        // Check if the budget status is already APPROVED or DENIED
        if (budget.getStatus() == SubmittedBudget.Status.APPROVED || budget.getStatus() == SubmittedBudget.Status.DENIED) {
            return new ResponseEntity<>("Budget status is final and cannot be changed", HttpStatus.FORBIDDEN);
        }
        // Only allow update if status is PENDING
        if (budget.getStatus() == SubmittedBudget.Status.PENDING) {
            budget.setStatus(status);
            budgetRepository.save(budget);
            return new ResponseEntity<>("Status updated successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid status transition", HttpStatus.BAD_REQUEST);
    }

    @Transactional
    public ResponseEntity<String> associateExpenseWithBudget(Long budgetId, Long expenseId) {
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

    public ResponseEntity<SubmittedBudget> getBudgetById(Long budgetId) {
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

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
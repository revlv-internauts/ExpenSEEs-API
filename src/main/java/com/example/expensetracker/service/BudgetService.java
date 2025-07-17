package com.example.expensetracker.service;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.ExpenseItem;
import com.example.expensetracker.Entity.Liquidation;
import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.BudgetRepository;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.Repository.LiquidationRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.exception.UnauthorizedAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LiquidationRepository liquidationRepository;

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
        budget.getExpenses().forEach(item -> item.setBudget(budget));
        return budgetRepository.save(budget);
    }

    public List<SubmittedBudget> getAllBudgets(String sortBy, String sortOrder) {
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
        return budgets;
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
            logger.error("Budget ID is null");
            return new ResponseEntity<>("Budget ID is required", HttpStatus.BAD_REQUEST);
        }
        User currentUser = userRepository.findByUsername(getCurrentUsername());
        if (currentUser == null) {
            logger.error("User not found for username: {}", getCurrentUsername());
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (budgetRepository.findById(budgetId).isEmpty()) {
            logger.warn("Budget not found for ID: {}", budgetId);
            return new ResponseEntity<>("Budget not found", HttpStatus.NOT_FOUND);
        }

        logger.info("Deleting budget with ID: {}", budgetId);
        // Delete associated liquidations
        List<Liquidation> liquidations = liquidationRepository.findAll().stream()
                .filter(l -> l.getSubmittedBudget() != null && l.getSubmittedBudget().getBudgetId().equals(budgetId))
                .toList();
        if (!liquidations.isEmpty()) {
            logger.info("Deleting {} associated liquidation records for budget ID: {}", liquidations.size(), budgetId);
            liquidationRepository.deleteAll(liquidations);
        }

        budgetRepository.deleteById(budgetId);
        logger.info("Budget deleted successfully: {}", budgetId);
        return new ResponseEntity<>("Budget and associated liquidations deleted successfully", HttpStatus.OK);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
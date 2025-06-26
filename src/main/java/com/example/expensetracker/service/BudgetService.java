package com.example.expensetracker.service;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.ExpenseItem;
import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.BudgetRepository;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        return budgetRepository.findAll().stream()
                .filter(budget -> budget.getUser().getUserId().equals(user.getUserId()))
                .toList();
    }

    @Transactional
    public void updateBudgetStatus(Long budgetId, SubmittedBudget.Status status) {
        SubmittedBudget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        budget.setStatus(status);
        budgetRepository.save(budget);
    }

    @Transactional
    public void associateExpenseWithBudget(Long budgetId, Long expenseId) {
        SubmittedBudget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        ExpenseItem item = new ExpenseItem();
        item.setCategory(expense.getCategory());
        item.setAmountPerUnit(expense.getAmount());
        item.setQuantity(1);
        item.setBudget(budget);
        budget.getExpenses().add(item);
        budget.setTotal(budget.getTotal() + expense.getAmount());
        budgetRepository.save(budget);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
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
import java.time.LocalDateTime;
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
        User myUser = userRepository.findByUsername(username);
        if (myUser == null) throw new UsernameNotFoundException("User not found with username: " + username);

        budget.setUser(myUser);
        budget.setTotal(budget.getExpenses().stream().mapToDouble(item -> item.getQuantity() * item.getAmountPerUnit()).sum());
        budget.setStatus(SubmittedBudget.Status.PENDING);
        budget.setExpenses(budget.getExpenses().stream().peek(item -> item.setBudget(budget)).toList());
        return budgetRepository.save(budget);
    }

    public List<SubmittedBudget> getAllBudgets() {
        User myUser = userRepository.findByUsername(getCurrentUsername());
        return budgetRepository.findAll().stream()
                .filter(budget -> budget.getUser().getId().equals(myUser.getId()))
                .toList();
    }

    @Transactional
    public void updateBudgetStatus(Long id, SubmittedBudget.Status status) {
        SubmittedBudget budget = budgetRepository.findById(id).orElseThrow();
        budget.setStatus(status);
        budgetRepository.save(budget);
    }

    @Transactional
    public void associateExpenseWithBudget(Long budgetId, Long expenseId) {
        SubmittedBudget budget = budgetRepository.findById(budgetId).orElseThrow();
        Expense expense = expenseRepository.findById(expenseId).orElseThrow();
        // Logic to associate expense with a budget item (simplified)
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
        return null;
    }
}
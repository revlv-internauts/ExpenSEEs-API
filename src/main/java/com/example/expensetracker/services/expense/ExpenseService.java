package com.example.expensetracker.services.expense;

import com.example.expensetracker.dto.ExpenseDTO;
import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.entity.User;

import java.util.List;

public interface ExpenseService {
    Expense postExpense(ExpenseDTO expenseDTO, User user);
    List<Expense> getAllExpenses(User user);
    Expense getExpenseById(Long id, User user);
    Expense updateExpense(Long id, ExpenseDTO expenseDTO, User user);
    void deleteExpense(Long id, User user);
}
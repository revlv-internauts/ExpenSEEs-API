package com.example.expensetracker.services.expense;

import com.example.expensetracker.dto.ExpenseDTO;
import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.entity.User;
import com.example.expensetracker.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    public Expense postExpense(ExpenseDTO expenseDTO, User user) {
        Expense expense = new Expense();
        expense.setUser(user);
        return saveOrUpdateExpense(expense, expenseDTO);
    }

    private Expense saveOrUpdateExpense(Expense expense, ExpenseDTO expenseDTO) {
        expense.setTitle(expenseDTO.getTitle());
        expense.setDate(expenseDTO.getDate());
        expense.setAmount(expenseDTO.getAmount());
        expense.setCategory(expenseDTO.getCategory());
        expense.setDescription(expenseDTO.getDescription());
        return expenseRepository.save(expense);
    }

    public Expense updateExpense(Long id, ExpenseDTO expenseDTO, User user) {
        Optional<Expense> optionalExpense = expenseRepository.findById(id);
        if (optionalExpense.isPresent() && optionalExpense.get().getUser().getId().equals(user.getId())) {
            return saveOrUpdateExpense(optionalExpense.get(), expenseDTO);
        } else {
            throw new EntityNotFoundException("Expense not found or access denied for id " + id);
        }
    }

    public List<Expense> getAllExpenses(User user) {
        return expenseRepository.findByUser(user).stream()
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .collect(Collectors.toList());
    }

    public Expense getExpenseById(Long id, User user) {
        Optional<Expense> optionalExpense = expenseRepository.findById(id);
        if (optionalExpense.isPresent() && optionalExpense.get().getUser().getId().equals(user.getId())) {
            return optionalExpense.get();
        } else {
            throw new EntityNotFoundException("Expense not found or access denied for id " + id);
        }
    }

    public void deleteExpense(Long id, User user) {
        Optional<Expense> optionalExpense = expenseRepository.findById(id);
        if (optionalExpense.isPresent() && optionalExpense.get().getUser().getId().equals(user.getId())) {
            expenseRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException("Expense not found or access denied for id " + id);
        }
    }
}
package com.example.expensetracker.Repository;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByUser(User user);
    List<Expense> findAllByUser(User user, Sort sort);
}
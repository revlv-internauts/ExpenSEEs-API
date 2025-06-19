package com.example.expensetracker.Repository;

import java.util.List;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    public List<Expense> findAllByUser(User user);
}
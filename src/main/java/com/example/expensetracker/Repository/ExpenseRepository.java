package com.example.expensetracker.Repository;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByUser(User user);
}

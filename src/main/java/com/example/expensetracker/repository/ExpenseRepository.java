package com.example.expensetracker.repository;

import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByDateBetweenAndUser(LocalDate startDate, LocalDate endDate, User user);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user")
    Double sumAllAmountsByUser(User user);

    Optional<Expense> findFirstByUserOrderByDateDesc(User user);

    List<Expense> findByUser(User user);
}
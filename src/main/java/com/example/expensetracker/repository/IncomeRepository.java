package com.example.expensetracker.repository;

import com.example.expensetracker.entity.Income;
import com.example.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByDateBetweenAndUser(LocalDate startDate, LocalDate endDate, User user);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user = :user")
    Double sumAllAmountsByUser(User user);

    Optional<Income> findFirstByUserOrderByDateDesc(User user);

    List<Income> findByUser(User user);
}
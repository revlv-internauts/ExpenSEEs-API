package com.example.expensetracker.Repository;

import com.example.expensetracker.Entity.SubmittedBudget;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<SubmittedBudget, Long> {
    @Query("SELECT b FROM SubmittedBudget b JOIN FETCH b.user LEFT JOIN FETCH b.liquidations")
    List<SubmittedBudget> findAllWithUserAndLiquidations(Sort sort);
}
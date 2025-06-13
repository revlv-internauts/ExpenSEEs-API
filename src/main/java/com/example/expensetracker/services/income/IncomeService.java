package com.example.expensetracker.services.income;

import com.example.expensetracker.dto.IncomeDTO;
import com.example.expensetracker.entity.Income;
import com.example.expensetracker.entity.User;

import java.util.List;

public interface IncomeService {
    Income postIncome(IncomeDTO incomeDTO, User user);
    List<Income> getAllIncomes(User user);
    Income getIncomeById(Long id, User user);
    Income updateIncome(Long id, IncomeDTO incomeDTO, User user);
    void deleteIncome(Long id, User user);
}
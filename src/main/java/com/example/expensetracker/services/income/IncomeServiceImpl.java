package com.example.expensetracker.services.income;

import com.example.expensetracker.dto.IncomeDTO;
import com.example.expensetracker.entity.Income;
import com.example.expensetracker.entity.User;
import com.example.expensetracker.repository.IncomeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;

    public Income postIncome(IncomeDTO incomeDTO, User user) {
        Income income = new Income();
        income.setUser(user);
        return saveOrUpdateIncome(income, incomeDTO);
    }

    private Income saveOrUpdateIncome(Income income, IncomeDTO incomeDTO) {
        income.setTitle(incomeDTO.getTitle());
        income.setDate(incomeDTO.getDate());
        income.setAmount(incomeDTO.getAmount());
        income.setCategory(incomeDTO.getCategory());
        income.setDescription(incomeDTO.getDescription());
        return incomeRepository.save(income);
    }

    public Income updateIncome(Long id, IncomeDTO incomeDTO, User user) {
        Optional<Income> optionalIncome = incomeRepository.findById(id);
        if (optionalIncome.isPresent() && optionalIncome.get().getUser().getId().equals(user.getId())) {
            return saveOrUpdateIncome(optionalIncome.get(), incomeDTO);
        } else {
            throw new EntityNotFoundException("Income not found or access denied for id " + id);
        }
    }

    public List<Income> getAllIncomes(User user) {
        return incomeRepository.findByUser(user).stream()
                .sorted(Comparator.comparing(Income::getDate).reversed())
                .collect(Collectors.toList());
    }

    public Income getIncomeById(Long id, User user) {
        Optional<Income> optionalIncome = incomeRepository.findById(id);
        if (optionalIncome.isPresent() && optionalIncome.get().getUser().getId().equals(user.getId())) {
            return optionalIncome.get();
        } else {
            throw new EntityNotFoundException("Income not found or access denied for id " + id);
        }
    }

    public void deleteIncome(Long id, User user) {
        Optional<Income> optionalIncome = incomeRepository.findById(id);
        if (optionalIncome.isPresent() && optionalIncome.get().getUser().getId().equals(user.getId())) {
            incomeRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException("Income not found or access denied for id " + id);
        }
    }
}
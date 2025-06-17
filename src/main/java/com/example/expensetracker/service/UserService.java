package com.example.expensetracker.service;


import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public ResponseEntity<Object> registerUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return new ResponseEntity<>("User already exists", HttpStatus.CONFLICT);
        }
        try {
            userRepository.save(user);
            return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error while registering user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//	public Double getTotalExpenseAmount() {
//		String username = getCurrentUserName();
//		User myUser = userRepository.findByUsername(username);
//		return myUser.getExpenses().stream().mapToDouble(Expense::getAmount).sum();
//	}

//	private String getCurrentUserName() {
//		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//		if (principal instanceof UserDetails) {
//			return ((UserDetails) principal).getUsername();
//		}
//		throw new IllegalStateException("No authenticated user found");
//	}

}
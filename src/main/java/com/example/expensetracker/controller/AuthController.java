package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.config.JwtUtil;
import com.example.expensetracker.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private LoginService loginService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/auth/sign-in")
    public ResponseEntity<Object> signIn(@RequestBody Map<String, String> credentials) {
        String usernameOrEmail = credentials.get("usernameOrEmail");
        String password = credentials.get("password");

        if (usernameOrEmail == null || usernameOrEmail.isEmpty() || password == null || password.isEmpty()) {
            return new ResponseEntity<>("Username or email and password are required", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElse(null);
        if (user == null || !encoder.matches(password, user.getPassword())) {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.BAD_REQUEST);
        }

        UserDetails userDetails = loginService.loadUserByUsername(user.getUsername());
        String jwt = jwtUtil.generateToken(userDetails);
        Map<String, Object> response = new HashMap<>();
        response.put("jwt", jwt);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}

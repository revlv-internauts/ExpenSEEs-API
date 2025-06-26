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
        String accessToken = jwtUtil.generateToken(userDetails); // Short-lived access token
        String refreshToken = jwtUtil.generateRefreshToken(userDetails); // Long-lived refresh token
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("refresh_token", refreshToken);
        response.put("user_id", user.getUserId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<Object> refreshToken(@RequestBody Map<String, String> tokenRequest) {
        String refreshToken = tokenRequest.get("refresh_token");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return new ResponseEntity<>("Refresh token is required", HttpStatus.BAD_REQUEST);
        }

        if (jwtUtil.validateRefreshToken(refreshToken)) {
            String username = jwtUtil.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username);
            if (user != null) {
                UserDetails userDetails = loginService.loadUserByUsername(username);
                String newAccessToken = jwtUtil.generateToken(userDetails);
                Map<String, Object> response = new HashMap<>();
                response.put("access_token", newAccessToken);
                response.put("refresh_token", refreshToken); // Reuse refresh token or generate new one if rotated
                response.put("user_id", user.getUserId());
                response.put("status", "success");
                return ResponseEntity.ok(response);
            }
        }
        return new ResponseEntity<>("Invalid refresh token", HttpStatus.UNAUTHORIZED);
    }
}

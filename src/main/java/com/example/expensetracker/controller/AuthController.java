package com.example.expensetracker.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.config.JwtUtil;
import com.example.expensetracker.service.LoginService;
import com.example.expensetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private LoginService loginService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Object> signUp(@RequestBody User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        return userService.registerUser(user);
    }

    @PostMapping("/signin")
    public ResponseEntity<Object> signIn(@RequestBody Map<String, String> credentials) {
        System.out.println("Attempting sign-in with credentials: " + credentials);
        String usernameOrEmail = credentials.get("username");
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (password == null || password.isEmpty()) {
            System.out.println("Password is missing or empty");
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        User myUser = null;
        if (usernameOrEmail != null && !usernameOrEmail.isEmpty()) {
            myUser = userRepository.findByUsername(usernameOrEmail);
        } else if (email != null && !email.isEmpty()) {
            myUser = userRepository.findByEmail(email).orElse(null);
        } else {
            System.out.println("Neither username nor email provided");
            return new ResponseEntity<>("Username or email is required", HttpStatus.UNAUTHORIZED);
        }

        if (myUser == null) {
            System.out.println("User not found for: " + (usernameOrEmail != null ? usernameOrEmail : email));
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        if (!encoder.matches(password, myUser.getPassword())) {
            System.out.println("Password mismatch for: " + (usernameOrEmail != null ? usernameOrEmail : email));
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = loginService.loadUserByUsername(myUser.getUsername());
        String jwt = jwtUtil.generateToken(userDetails);
        Map<String, Object> response = new HashMap<>();
        response.put("jwt", jwt);
        response.put("status", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    public ResponseEntity<Object> getUser(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        user.setPassword(null); // Security: exclude password
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{username}")
    public ResponseEntity<Object> updateUser(@PathVariable String username, @RequestBody User userDetails) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        if (userDetails.getEmail() != null) {
            existingUser.setEmail(userDetails.getEmail());
        }
        if (userDetails.getPassword() != null) {
            existingUser.setPassword(encoder.encode(userDetails.getPassword()));
        }
        userRepository.save(existingUser);
        return ResponseEntity.ok("User updated successfully");
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Object> deleteUser(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        userRepository.delete(user);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Object> requestPasswordReset(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetExpiresAt(java.time.LocalDateTime.now().plusHours(1));

        userRepository.save(user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset requested. Please contact admin with token: " + resetToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> resetPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String resetToken = request.get("resetToken");
        String newPassword = request.get("newPassword");

        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        if (user.getResetToken() == null || !user.getResetToken().equals(resetToken) ||
                user.getResetExpiresAt() == null || user.getResetExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return new ResponseEntity<>("Invalid or expired reset token", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(encoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetExpiresAt(null);
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully");
    }
}
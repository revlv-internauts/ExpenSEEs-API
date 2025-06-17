package com.example.expensetracker.controller;

import java.util.HashMap;
import java.util.Map;

import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.config.JwtUtil;
import com.example.expensetracker.service.LoginService;
import com.example.expensetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Sign up a new user
    @PostMapping("/signup")
    public ResponseEntity<Object> signUp(@RequestBody User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        return userService.registerUser(user);
    }

    // Sign in and generate JWT
    @PostMapping("/signin")
    public ResponseEntity<Object> signIn(@RequestBody User user) {
        User myUser = userRepository.findByUsername(user.getUsername());
        try {
            if (myUser == null || !encoder.matches(user.getPassword(), myUser.getPassword())) {
                return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
            }

            UserDetails userDetails = loginService.loadUserByUsername(user.getUsername());
            String jwt = jwtUtil.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("jwt", jwt);
            response.put("status", HttpStatus.OK.value());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return new ResponseEntity<>("Authentication failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get user details by username (requires JWT)
    @GetMapping("/{username}")
    public ResponseEntity<Object> getUser(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        // Remove password from response for security
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // Update user details (requires JWT)
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

    // Delete user (requires JWT)
    @DeleteMapping("/{username}")
    public ResponseEntity<Object> deleteUser(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        userRepository.delete(user);
        return ResponseEntity.ok("User deleted successfully");
    }
}
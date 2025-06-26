package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.ForgotPassword;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.ForgotPasswordRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.dto.MailBody;
import com.example.expensetracker.service.EmailService;
import com.example.expensetracker.util.ChangePassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/forgotPassword")
@CrossOrigin(origins = "*")
public class ForgotPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ForgotPasswordRepository forgotPasswordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/verifyMail/{email}")
    @Transactional
    public ResponseEntity<String> verifyEmail(@PathVariable String email) {
        try {
            // Validate and retrieve user
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid email: " + email));

            // Clean up existing forgot password records for the user
            forgotPasswordRepository.deleteByUser(user);

            // Generate OTP and create mail body
            int otp = otpGenerator();
            MailBody mailBody = MailBody.builder()
                    .to(email)
                    .text("Your OTP for password reset is: " + otp + ". Valid for 20 minutes.")
                    .subject("Password Reset OTP")
                    .build();

            ForgotPassword fp = ForgotPassword.builder()
                    .otp(otp)
                    .expirationTime(new Date(System.currentTimeMillis() + 20 * 60 * 1000))
                    .user(user)
                    .build();

            // Attempt to send email
            emailService.sendSimpleMessage(mailBody);
            forgotPasswordRepository.save(fp);
            logger.info("OTP sent successfully to email: {}", email);
            return ResponseEntity.ok("OTP sent to your email for verification!");
        } catch (UsernameNotFoundException e) {
            logger.error("Invalid email address: {}", email, e);
            return new ResponseEntity<>("Invalid email address", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Failed to send OTP for email: {}. Error: {}", email, e.getMessage(), e);
            forgotPasswordRepository.deleteByUser(userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid email: " + email)));
            return new ResponseEntity<>("Failed to send OTP. Please check your email settings or try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verifyOtp/{otp}/{email}")
    public ResponseEntity<Map<String, String>> verifyOtp(@PathVariable Integer otp, @PathVariable String email) {
        Map<String, String> response = new HashMap<>();
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        response.put("status", "error");
                        response.put("message", "Invalid email: " + email);
                        return new UsernameNotFoundException("Invalid email");
                    });

            ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                    .orElseThrow(() -> {
                        response.put("status", "error");
                        response.put("message", "Invalid or expired OTP for email: " + email);
                        return new RuntimeException();
                    });

            if (fp.getExpirationTime().before(new Date())) {
                forgotPasswordRepository.deleteById(fp.getForgotPasswordId());
                response.put("status", "error");
                response.put("message", "OTP has expired. Please request a new one.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            response.put("status", "success");
            response.put("message", "OTP verified successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error verifying OTP for email: {}. Error: {}", email, e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "An error occurred while verifying OTP. Please try again.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/changePassword/{email}")
    @Transactional
    public ResponseEntity<String> changePassword(@RequestBody ChangePassword changePassword,
                                                 @PathVariable String email,
                                                 @RequestParam("otp") Integer otp) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid email: " + email));

            ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                    .orElseThrow(() -> new RuntimeException("Invalid OTP for email: " + email));

            if (fp.getExpirationTime().before(new Date())) {
                forgotPasswordRepository.deleteById(fp.getForgotPasswordId());
                return new ResponseEntity<>("OTP has expired. Please request a new one.", HttpStatus.BAD_REQUEST);
            }

            if (!changePassword.password().equals(changePassword.repeatPassword())) {
                return new ResponseEntity<>("Passwords do not match.", HttpStatus.BAD_REQUEST);
            }

            String encodedPassword = passwordEncoder.encode(changePassword.password());
            userRepository.updatePassword(email, encodedPassword);
            forgotPasswordRepository.deleteById(fp.getForgotPasswordId());

            logger.info("Password changed successfully for email: {}", email);
            return ResponseEntity.ok("Password changed successfully!");
        } catch (Exception e) {
            logger.error("Error changing password for email: {}. Error: {}", email, e.getMessage(), e);
            return new ResponseEntity<>("An error occurred while changing password. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}

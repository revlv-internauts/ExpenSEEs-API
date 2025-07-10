package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserWithDetails(@PathVariable Long userId) {
        Map<String, String> response = new HashMap<>();
        ResponseEntity<User> result = userService.getUserWithDetails(userId);
        if (result.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(result.getBody());
        } else {
            response.put("error", "User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (file.isEmpty()) {
                response.put("error", "Image file is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("error", "Only image files are allowed");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                response.put("error", "Invalid image file");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String uploadDir = "Uploads/profile-pictures/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            Files.write(filePath, baos.toByteArray());

            user.setProfilePicturePath(uploadDir + fileName);
            userRepository.save(user);

            response.put("message", "Profile picture uploaded successfully");
            response.put("path", user.getProfilePicturePath());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("error", "Failed to upload profile picture: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{userId}/profile-picture")
    public ResponseEntity<?> getProfilePicture(@PathVariable Long userId) {
        Map<String, String> response = new HashMap<>();
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String imagePath = user.getProfilePicturePath();
            if (imagePath == null || imagePath.isEmpty()) {
                imagePath = "Uploads/profile-pictures/default-profile.jpg"; // Default image
            }

            File file = new File(imagePath);
            if (!file.exists() || !file.isFile()) {
                response.put("error", "Profile picture not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            response.put("error", "Failed to retrieve profile picture: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
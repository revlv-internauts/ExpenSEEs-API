package com.example.expensetracker;

import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ExpensetrackerApplication {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(ExpensetrackerApplication.class, args);
	}

	@Bean
	public CommandLineRunner initAdminUser() {
		return args -> {
			if (userRepository.findByUsername("admin") == null) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setEmail("admin@gmail.com");
				admin.setPassword(passwordEncoder.encode("admin"));
				userRepository.save(admin);
				System.out.println("Admin user created: username=admin, email=admin@gmail.com, password=admin");
			}
		};
	}
}

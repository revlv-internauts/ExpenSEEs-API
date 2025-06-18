package com.example.expensetracker.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.expensetracker.Entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    java.util.Optional<User> findByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.password = ?2 WHERE u.email = ?1")
    void updatePassword(String email, String encodedPassword);
}
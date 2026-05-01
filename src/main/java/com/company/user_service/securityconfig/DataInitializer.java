package com.company.user_service.securityconfig;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.company.user_service.entity.User;
import com.company.user_service.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initAdmin() {

        String adminEmail = "admin@company.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of("ADMIN"));
         admin.setVerified(true);
            userRepository.save(admin);

            System.out.println("🔥 Admin created successfully!");
        } else {
            System.out.println("✅ Admin already exists");
        }
    }
}
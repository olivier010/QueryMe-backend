package com.year2.queryme.service;

import com.year2.queryme.model.Admin;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.AdminRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.model.enums.UserTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Admin registerAdmin(String email, String password, String fullName) {
        // 1. Create User
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserTypes.ADMIN)
                .name(fullName)
                .build();
        userRepository.save(user);

        // 2. Create Admin linked to User
        Admin admin = Admin.builder()
                .fullName(fullName)
                .user(user)
                .build();

        return adminRepository.save(admin);
    }

    @Transactional
    public Admin updateProfile(Long adminId, Map<String, String> data) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (data.containsKey("fullName")) {
            admin.setFullName(data.get("fullName"));
            User user = admin.getUser();
            if (user != null) {
                user.setName(data.get("fullName"));
                userRepository.save(user);
            }
        }
        if (data.containsKey("password")) {
            User user = admin.getUser();
            if (user != null) {
                user.setPasswordHash(passwordEncoder.encode(data.get("password")));
                userRepository.save(user);
            }
        }

        return adminRepository.save(admin);
    }
}

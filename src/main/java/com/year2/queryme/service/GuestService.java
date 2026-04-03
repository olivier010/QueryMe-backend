package com.year2.queryme.service;

import com.year2.queryme.model.Guest;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.GuestRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.model.enums.UserTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class GuestService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Guest registerGuest(String email, String password, String fullName) {
        // 1. Create User
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserTypes.GUEST)
                .name(fullName)
                .build();
        userRepository.save(user);

        // 2. Create Guest linked to User
        Guest guest = Guest.builder()
                .fullName(fullName)
                .user(user)
                .build();

        return guestRepository.save(guest);
    }

    @Transactional
    public Guest updateProfile(Long guestId, Map<String, String> data) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));

        if (data.containsKey("fullName")) {
            guest.setFullName(data.get("fullName"));
            User user = guest.getUser();
            if (user != null) {
                user.setName(data.get("fullName"));
                userRepository.save(user);
            }
        }
        if (data.containsKey("password")) {
            User user = guest.getUser();
            if (user != null) {
                user.setPasswordHash(passwordEncoder.encode(data.get("password")));
                userRepository.save(user);
            }
        }

        return guestRepository.save(guest);
    }
}

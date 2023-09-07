package com.epapers.epapers.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {
    
    @Autowired
    UserRepository userRepository;

    @Autowired
    EpaperService epaperService;

    @Autowired
    EmailService emailService;

    @Autowired
    BCryptPasswordEncoder encoder;

    public Map<String, String> saveNewUser(EpapersUser user) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "true");

        Map<String, String> editions = epaperService.getEditionFromCity(Optional.ofNullable(user.getCity()).orElse("").toUpperCase());
        if(editions.isEmpty()) {
            log.info("User Registration Failed. Invalid City. {}", user);
            response.put("status", "false");
            response.put("error", "No ePaper edition found for this city. Please choose another city.");
            return response;
        } else {
            user.setEditions(editions);
        }

        Optional<EpapersUser> dbUser = userRepository.findById(user.getUsername());

        if(dbUser.isPresent()) {
            log.info("User Registration Failed. User already exists. {}", user);
            response.put("status", "false");
            response.put("error", "Username already exists. Please try again.");
        } else {
            try {
                user.setPassword(encoder.encode(Optional.ofNullable(user.getPassword()).orElse("")));
                userRepository.save(user);
                log.info("\nUSER ADDED SUCCESSFULLY: {}\n", user);
                long usersCount = userRepository.count();
                emailService.notifyUserActivity(
                    String.format("""
                        New User Registered (non-OAuth)! 🤔

                        Name: %s.

                        Email ID: %s.

                        City: %s.

                        Total subscriber count: %d
                        """,
                        user.getUsername(),
                        user.getEmail(),
                        user.getCity(),
                        usersCount)
                );
            } catch(Exception e) {
                log.error("\nFAILED TO SAVE USER: {}\n", user);
                response.put("status", "false");
                response.put("error", "Something went wrong. Please try again later.");
            }
        }
        return response;
    }

    public EpapersUser getUserByUserName(String username) {
        return userRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException("Invalid username/password"));
    }

    public boolean userExists(String username) {
        return userRepository.existsById(username);
    }

    public List<EpapersUser> getAllUsers() {
        return userRepository.findAll();
    }

    public void refreshDB() {
        userRepository.deleteAll();
    }

}

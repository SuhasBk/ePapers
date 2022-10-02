package com.epapers.epapers.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {
    
    @Autowired
    UserRepository userRepository;

    public boolean canAccess(EpapersUser user) {
        Optional<EpapersUser> dbUser = userRepository.findById(user.getChatId());
        if(dbUser.isPresent()) {
            log.info("Quota done for the day for: {}", user);
            return false;
        } else {
            try {
                userRepository.save(user);
                log.info("\nUSER RECORDED SUCCESSFULLY: {}\n", user);
            } catch(Exception e) {
                log.error("\nFAILED TO SAVE USER: {}\n", user);
            }
        }
        return true;
    }

    public void refreshDB() {
        userRepository.deleteAll();
    }

}

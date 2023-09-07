package com.epapers.epapers.config;

import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.EmailService;
import com.epapers.epapers.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class AuthProvider implements AuthenticationProvider {

    @Autowired
    UserService userService;

    @Autowired
    EmailService emailService;

    @Autowired
    BCryptPasswordEncoder encoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        EpapersUser dbUser = userService.getUserByUserName(username);
        if(dbUser != null && encoder.matches(password, dbUser.getPassword())) {
            emailService.notifyUserActivity(
                String.format("""
                        New User Signing In! (non-OAuth) 🤔

                        Name: %s.

                        Email ID: %s.

                        City: %s.
                        """,
                        dbUser.getUsername(),
                        dbUser.getEmail(),
                        dbUser.getCity())
            );
            return new UsernamePasswordAuthenticationToken(authentication.getName(), authentication.getCredentials().toString(), new ArrayList<>());
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

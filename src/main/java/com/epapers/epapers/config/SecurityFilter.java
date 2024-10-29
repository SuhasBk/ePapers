package com.epapers.epapers.config;

import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.EmailService;
import com.epapers.epapers.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityFilter {

    @Autowired
    AuthProvider authProvider;

    @Autowired
    UserService userService;

    @Autowired
    EmailService emailService;

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            // Delegate to the default implementation for loading a user
            OidcUser oidcUser = delegate.loadUser(userRequest);
            log.info("Google OAuth user logging in - {}", oidcUser);
            String email = oidcUser.getEmail();
            String username = Optional.ofNullable(oidcUser.getPreferredUsername()).orElse(email);

            if(userService.userExists(username)) {
                emailService.notifyUserActivity(
                    String.format("""
                    New Google User Signing In! 😊

                    Name: %s.

                    Email ID: %s.
                    """,
                    oidcUser.getFullName(),
                    email)
                );
                return oidcUser;
            }

            userService.saveNewUser(EpapersUser
                    .builder()
                    .username(username)
                    .email(email)
                    .isOAuth(true)
                    .build());
            return oidcUser;
        };
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            log.info("GitHub OAuth user logging in - {}", user);
            String username = user.getAttribute("login");
            String email = user.getAttribute("email");

            if(userService.userExists(username)) {
                emailService.notifyUserActivity(
                    String.format("""
                    New GitHub User Signing In! 😊

                    Name: %s.

                    Email ID: %s.
                    """,
                    username,
                    email)
                );
                return user;
            }

            userService.saveNewUser(EpapersUser
                    .builder()
                    .username(username)
                    .email(email)
                    .isOAuth(true)
                    .build());
            return user;
        };
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authProvider);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authorize -> authorize
                        .antMatchers(
                                "/signin.html",
                                "/register.html",
                                "/scripts/**",
                                "/styles/**",
                                "/api/getEditionList**",
                                "/api/register**",
                                "/api/trigger",
                                "/api/triggerCache",
                                "/api/sendMail",
                                "/api/file/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
        )
        .oauth2Login(
            httpSecurityOAuth2LoginConfigurer -> httpSecurityOAuth2LoginConfigurer
            .loginPage("/signin.html")
            .failureUrl("/signin.html?error=true"))
        .formLogin(form -> form
                .loginPage("/signin.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/")
                .failureUrl("/signin.html?error=true"));

        return http.build();
    }

}

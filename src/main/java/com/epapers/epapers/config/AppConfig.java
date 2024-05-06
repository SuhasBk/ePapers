package com.epapers.epapers.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class AppConfig {

    public final static Float HT_SCALE_PERCENT = 21f;
    public final static Float TOI_SCALE_PERCENT = 29f;
    public static final int INPUT_BUFFER_SIZE = 4096;
    public final static String HOSTNAME = Optional.ofNullable(System.getenv("EPAPERS_HOSTNAME")).orElse("http://localhost:8000");
    public final static String CHATSTOMP_URL = System.getenv("CHATSTOMP_URL");
    public final static String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");

    @Bean
    JavaMailSender getJavaMailSender() {
        String username = System.getenv("EMAIL_ID");
        String password = System.getenv("EMAIL_PASSWORD");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

    @Bean
    BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}

package com.epapers.epapers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

@Configuration
public class AppConfig {

    public final static Float HT_SCALE_PERCENT = 21f;
    public final static Float TOI_SCALE_PERCENT = 29f;
    public static final int INPUT_BUFFER_SIZE = 4096;
    public final static String HOSTNAME = Optional.ofNullable(System.getenv("EPAPERS_HOSTNAME")).orElse("http://localhost:8000");
    public final static String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
    public static final String PORTFOLIO_URL = System.getenv("PORTFOLIO_URL");
    public static final String CHATSTOMP_URL = System.getenv("CHATSTOMP_URL");

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
    WebClient webClient() {
        // configure pooling strategy, max connections, connection availability with eviction
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120)).build();

        // configure in-memory size for http request and response bodies, if it exceeds, write to disk:
        final int size = 16 * 1024 * 1024;  // 16MB
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        // return custom webclient instance with defined config
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
                .exchangeStrategies(strategies)
                .build();
    }
}

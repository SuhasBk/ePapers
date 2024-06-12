package com.epapers.epapers.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.service.EmailService;
import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.AppUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AppScheduler {

    @Autowired
    EpapersBot telegramBot;

    @Autowired
    EmailService emailService;

    @Autowired
    HttpClient httpClient;

    private static final String SERVER_URL = AppConfig.HOSTNAME;
    private static final String CHATSTOMP_URL = AppConfig.CHATSTOMP_URL;

    @Scheduled(fixedDelay = 5, initialDelay = 2, timeUnit = TimeUnit.MINUTES)
    public void keepAlive() {
        try {
            httpClient.send(HttpRequest.newBuilder()
                    .uri(new URI(SERVER_URL))
                    .GET()
                    .build(), BodyHandlers.ofString());

            httpClient.send(HttpRequest.newBuilder()
                .uri(new URI(CHATSTOMP_URL))
                .GET()
                .build(), BodyHandlers.ofString());
        } catch (IOException | InterruptedException | URISyntaxException e) {
            log.error("Error while keeping alive: {}", e.getLocalizedMessage());
        }
    }

    @Scheduled(fixedDelay = 30, initialDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void collectGarbage() {
        System.gc();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata")
    public void refreshDB() {
        try {
            log.info("Starting a new day. 😊");
            File currDir = new File(".");
            File[] pdfFiles = currDir.listFiles(file -> file.getName().endsWith(".pdf"));
            for (File file : pdfFiles) {
                AppUtils.deleteFile(file);
            }
            log.info("Old files purged successfully!");
        } catch (Exception e) {
            log.error("Failed to clear cached files.");
        }
    }

    @Scheduled(cron = "0 30 7 * * *", zone = "Asia/Kolkata")
    public void cacheTelegramSubscriptions() {
        telegramBot.triggerSubscriptions(true);
    }

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Kolkata")
    public void telegramSubscriptions() {
        telegramBot.triggerSubscriptions(false);
    }
}

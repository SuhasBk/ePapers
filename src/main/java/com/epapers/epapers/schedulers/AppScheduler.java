package com.epapers.epapers.schedulers;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epapers.epapers.service.EmailService;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.SubscriptionService;
import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.AppUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppScheduler {

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    EpaperService epaperService;

    @Autowired
    EpapersBot telegramBot;

    @Autowired
    EmailService emailService;

    private static final String SERVER_URL = "https://epapers.onrender.com";

    @Scheduled(fixedDelay = 10, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void keepAlive() {
        try {
            new URL(SERVER_URL).openStream();
            log.info("🎵 stayin' alive! 🎵");
        } catch (Exception e) {
            log.error("SOS! I AM DYING! SAVE ME!!!");
            emailService.mailSOS();      
        }
    }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void collectGarbage() {
        System.gc();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata")
    public void refreshDB() {
        try {
            log.info("Starting a new day. 😊");
            new URL(SERVER_URL + "/api/epapers/refreshDB").openStream();
            File currDir = new File(".");
            for (File file : currDir.listFiles(file -> file.getName().endsWith(".pdf"))) {
                AppUtils.deleteFile(file);
            }
            log.info("Old files purged successfully!");
        } catch (Exception e) {
            log.error("Failed to refresh db or delete old files.");
        }
    }

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Kolkata")
    public void telegramSubscriptions() {
        telegramBot.triggerSubscriptions();
    }
}

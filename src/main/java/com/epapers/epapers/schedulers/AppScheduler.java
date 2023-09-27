package com.epapers.epapers.schedulers;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.service.EmailService;
import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.AppUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppScheduler {

    @Autowired
    EpapersBot telegramBot;

    @Autowired
    EmailService emailService;

    @Autowired
    WebClient webClient;

    private static final String SERVER_URL = AppConfig.HOSTNAME;
    private static final String PORTFOLIO_URL = AppConfig.PORTFOLIO_URL;    // fly.io
    private static final String CHATSTOMP_URL = AppConfig.CHATSTOMP_URL;    // render.com

    // @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
    // public void keepChatStompAlive() {
    //     webClient
    //         .get()
    //         .uri(CHATSTOMP_URL)
    //         .retrieve()
    //         .toBodilessEntity()
    //         .doOnError(err -> {
    //             log.error("SHOULD REVIVE CHATSTOMP!");
    //             emailService.mailSOS();
    //         })
    //         .block();
    // }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void keepPortfolioAlive() {
        webClient
            .get()
            .uri(PORTFOLIO_URL)
            .retrieve()
            .toBodilessEntity()
            .doOnError(err -> {
                log.error("REVIVING PORTFOLIO");
                emailService.mailSOS();
            })
            .block();
    }

    @Scheduled(fixedDelay = 8, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void keepAlive() {
        webClient
            .get()
            .uri(SERVER_URL)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(resp -> {
                log.info("🎵 stayin' alive! 🎵");
            })
            .doOnError(err -> {
                log.error("SOS! I AM DYING! SAVE ME!!!");
                emailService.mailSOS();
            })
            .block();
    }

    @Scheduled(fixedDelay = 10, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
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

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Kolkata")
    public void telegramSubscriptions() {
        telegramBot.triggerSubscriptions(false);
    }
}

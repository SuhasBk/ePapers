package com.epapers.epapers.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.service.EmailService;
import com.epapers.epapers.telegram.EpapersBot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;

@Component
public class AppScheduler {

    @Autowired
    EpapersBot telegramBot;

    @Autowired
    EmailService emailService;

    @Autowired
    HttpClient httpClient;

    // @Autowired
    // WebClient webClient;

    private static final String SERVER_URL = AppConfig.HOSTNAME;
    // private static final String PORTFOLIO_URL = AppConfig.PORTFOLIO_URL;    // fly.io
    // private static final String CHATSTOMP_URL = AppConfig.CHATSTOMP_URL;    // render.com

    // @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    // public void keepPortfolioAlive() {
    //     webClient
    //         .get()
    //         .uri(PORTFOLIO_URL)
    //         .retrieve()
    //         .toBodilessEntity()
    //         .doOnError(err -> {
    //             log.error("REVIVING PORTFOLIO");
    //             emailService.mailSOS();
    //         })
    //         .block();
    // }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void keepAlive() throws Exception {
        httpClient.send(HttpRequest.newBuilder().uri(new URI(SERVER_URL)).GET().build(), null);
    }

    @Scheduled(fixedDelay = 10, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void collectGarbage() {
        System.gc();
    }

    // @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata")
    // public void refreshDB() {
    //     try {
    //         log.info("Starting a new day. 😊");
    //         File currDir = new File(".");
    //         File[] pdfFiles = currDir.listFiles(file -> file.getName().endsWith(".pdf"));
    //         for (File file : pdfFiles) {
    //             AppUtils.deleteFile(file);
    //         }
    //         log.info("Old files purged successfully!");
    //     } catch (Exception e) {
    //         log.error("Failed to clear cached files.");
    //     }
    // }

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Kolkata")
    public void telegramSubscriptions() {
        telegramBot.triggerSubscriptions(false);
    }
}

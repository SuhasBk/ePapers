package com.epapers.epapers.schedulers;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.EpapersSubscription;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.SubscriptionService;
import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.AppUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppScheduler {

    private static final String FILE_ACCESS_URL = "https://epapers.onrender.com/api/file?name=%s";

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    EpaperService epaperService;

    @Autowired
    EpapersBot telegramBot;

    @Scheduled(fixedDelay = 10, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void keepAlive() {
        try {
            new URL("https://epapers.onrender.com/").openStream();
            log.info("keeping it alive!");
        } catch (Exception e) {
            log.error("Oops. Cannot be kept-alive.");
        }
    }

    @Scheduled(fixedDelay = 2, initialDelay = 2, timeUnit = TimeUnit.HOURS)
    public void cleanUp() {
        try {
            File currDir = new File(".");
            for (File file : currDir.listFiles(file -> file.getName().endsWith(".pdf"))) {
                AppUtils.deleteFile(file);
            }
            log.info("Old files purged successfully!");
        } catch (Exception e) {
            log.error("Could not delete old files. {}", e);
        }
    }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void collectGarbage() {
        System.gc();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata")
    public void refreshDB() {
        try {
            new URL("https://epapers.onrender.com/api/epapers/refreshDB").openStream();
            log.info("Starting a new day. 😊");
        } catch (Exception e) {
            log.error("Failed to refresh db.");
        }
    }

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Kolkata")
    // @Scheduled(fixedRate = 2, timeUnit = TimeUnit.MINUTES)
    public void telegramSubscriptions() {
        String today = AppUtils.getTodaysDate();
        List<EpapersSubscription> subscriptions = subscriptionService.getAllSubscriptions();
        log.info("Processing all subscriptions - {}", subscriptions);
        subscriptions.forEach(subscription -> {
            try {
                String chatId = subscription.getChatId();
                Map<String, String> editions = subscription.getEditions();
                String toiEdition = editions.get("TOI");
                String htEdition = editions.get("HT");
                
                if(htEdition != null) {
                    Epaper htPdf = (Epaper) epaperService.getHTpdf(htEdition, today).get("epaper");
                    telegramBot.sendSubscriptionMessage(chatId, "Access your HT ePaper here: " + String.format(FILE_ACCESS_URL, htPdf.getFile().getName()), htPdf.getFile());
                }

                if(toiEdition != null) {
                    Epaper toiPdf = (Epaper) epaperService.getTOIpdf(toiEdition, today).get("epaper");
                    telegramBot.sendSubscriptionMessage(chatId, "Access your TOI ePaper here: " + String.format(FILE_ACCESS_URL, toiPdf.getFile().getName()), toiPdf.getFile());
                }
                log.info("ePapers successfully sent to - {}", chatId);                
            } catch (Exception e) {
                log.error("Subscription service failed. - {}", e);
            }
        });
    }
}

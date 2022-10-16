package com.epapers.epapers.telegram;

import java.io.File;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.EpapersSubscription;
import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.SubscriptionService;
import com.epapers.epapers.service.UserService;
import com.epapers.epapers.util.AppUtils;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class EpapersBot extends TelegramLongPollingBot {
    @Autowired
    EpaperService ePaperService;

    @Autowired
    UserService userService;

    @Autowired
    SubscriptionService subscriptionService;

    private static final String BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
    private static final String BOT_USERNAME = "ePapers";
    private static final String FILE_ACCESS_URL = "https://epapers.onrender.com/api/file?name=%s";
    private static final String BENGALURU_CITY_KANNADA = "ನಮ್ಮ ಬೆಂಗಳೂರು 🤘";
    private static final String EPAPER_KEY_STRING = "epaper";
    private static final String ACCESS_STRING = "Access it using: ";

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    public void sendSubscriptionMessage(String chatId, String message) {
        try {
            executeAsync(new SendMessage(chatId, message));
        } catch (TelegramApiException e) {
            log.error("Failed to send subscribed message to {}", chatId);
        }
    }

    public void handleErrors(String chatId, String feedback) {
        try {
            executeAsync(new SendMessage(chatId, feedback));
        } catch (TelegramApiException e1) {
            log.error("TELEGRAM-SERVICE: Failed to send feedback");
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && !update.getMessage().getText().isEmpty()) {
            String chatId = update.getMessage().getChatId().toString();
            User user = update.getMessage().getFrom();

            try {
                String userMessage = update.getMessage().getText().toUpperCase();
                StringBuilder editions = new StringBuilder();
                switch(userMessage) {
                    case "HTBNG":
                        executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing HT ePaper for : " + BENGALURU_CITY_KANNADA + " 🎉"));
                        Epaper htpdf = (Epaper) ePaperService.getHTpdf("102", AppUtils.getTodaysDate()).get(EPAPER_KEY_STRING);
                        executeAsync(new SendMessage(chatId, ACCESS_STRING + String.format(FILE_ACCESS_URL, htpdf.getFile().getName())));
                        executeAsync(new SendDocument(chatId, new InputFile(htpdf.getFile())));
                        break;
                    case "TOIBNG":
                        executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing TOI ePaper for " + BENGALURU_CITY_KANNADA + " 🎉"));
                        Epaper toipdf = (Epaper) ePaperService.getTOIpdf("toibgc", AppUtils.getTodaysDate()).get(EPAPER_KEY_STRING);
                        executeAsync(new SendMessage(chatId, ACCESS_STRING + String.format(FILE_ACCESS_URL, toipdf.getFile().getName())));
                        executeAsync(new SendDocument(chatId, new InputFile(toipdf.getFile())));
                        break;
                    case "HT":
                        editions.append("💡 Copy the WHOLE text for your city and type: 'download <copied_text>'\n\n");
                        editions.append("Example: download Bengaluru_102_HT\n\n");
                        ePaperService.getHTEditionList().forEach(edition -> editions.append("👉 "+edition.getEditionName() + "_" + edition.getEditionId() + "_" + "HT\n\n"));
                        executeAsync(new SendMessage(chatId, editions.toString()));
                        break;
                    case "TOI":
                        editions.append("💡 Copy the WHOLE text for your city and type: 'download <copied_text>'\n\n");
                        editions.append("Example: download Bangalore_toibgc_TOI\n\n");
                        ePaperService.getTOIEditionList().forEach(edition -> editions.append("👉 "+edition.getEditionName() + "_" + edition.getEditionId() + "_" + "TOI\n\n"));
                        executeAsync(new SendMessage(chatId, editions.toString()));
                        break;
                    case "SUBSCRIBE":
                        executeAsync(new SendMessage(chatId, "Alright! Please enter 'subscribe <city>' to start your daily subscription.\n\n\nP.S.\nTo unsubscribe, please enter 'unsubscribe'."));
                        break;
                    case "UNSUBSCRIBE":
                        executeAsync(new SendMessage(chatId, "Awww. Sad to hear that! 😢"));
                        subscriptionService.removeSubscription(chatId);
                        break;
                    default:
                        if (userMessage.startsWith("DOWNLOAD ")) {
                            sendPDF(chatId, user, userMessage);
                        } else if (userMessage.startsWith("SUBSCRIBE ")) {
                            subscribeNewUser(chatId, user, userMessage);
                        } else {
                            executeAsync(new SendMessage(chatId,"Hello there!\n\n👉 Enter publication: HT or TOI.\n\n👉 Enter 'download <copy_paste_edition>'\n\n👉 Have patience! 🙂\n\n👉 Enter 'subscribe' to get ePapers daily at 8:00 A.M."));
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
                handleErrors(chatId, "Could not process that request.");
            }
        }
    }

    public void sendPDF(String chatId, User user, String userMessage) throws Exception {
        String payload = userMessage.trim().split(" ")[1];
        String[] metaData = payload.split("_");
        if(metaData.length == 3) {
            String city = (metaData[0].equalsIgnoreCase("bengaluru")) ? BENGALURU_CITY_KANNADA : metaData[0];
            String editionId = metaData[1];
            String publication = metaData[2];
            EpapersUser epapersUser = new EpapersUser(chatId, user, editionId, city, AppUtils.getTodaysDate() + "_" + new Date().getTime(), 1);
            if(!userService.canAccess(epapersUser)) {
                executeAsync(new SendMessage(chatId, "Access denied ❌. Quota Exceeded."));
                return;
            }
            executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing " + publication + " ePaper for : " + city + " 🎉"));
            
            try{
                switch(publication) {
                    case "HT":
                        Epaper htPDF = (Epaper) ePaperService.getHTpdf(editionId, AppUtils.getTodaysDate()).get(EPAPER_KEY_STRING);
                        executeAsync(new SendMessage(chatId, ACCESS_STRING + String.format(FILE_ACCESS_URL, htPDF.getFile().getName())));
                        executeAsync(new SendDocument(chatId, new InputFile(htPDF.getFile())));
                        break;
                    case "TOI":
                        Epaper toiPDF = (Epaper) ePaperService.getTOIpdf(editionId.toLowerCase(), AppUtils.getTodaysDate()).get(EPAPER_KEY_STRING);
                        executeAsync(new SendMessage(chatId, ACCESS_STRING + String.format(FILE_ACCESS_URL, toiPDF.getFile().getName())));
                        File file = toiPDF.getFile();
                        if(!AppUtils.isLargeFile(file, "TELEGRAM")) {
                            executeAsync(new SendDocument(chatId, new InputFile(file)));
                        }
                        break;
                    default: break;
                }
            } catch(Exception e) {
                e.printStackTrace();
                handleErrors(chatId, "Something went wrong while downloading ePaper. 😢\n\n Try downloading it directly from https://epapers.onrender.com");
            }
        }
    }

    public void subscribeNewUser(String chatId, User user, String userMessage) throws Exception {
        String[] metaData = userMessage.split(" ");
        if (metaData.length == 2) {
            String city = metaData[1];
            Map<String, String> subscribedEditions = ePaperService.getEditionFromCity(city);
            if (!subscribedEditions.isEmpty()) {
                EpapersSubscription subscription = new EpapersSubscription(chatId, user, subscribedEditions);
                subscriptionService.addSubscription(subscription);
                executeAsync(new SendMessage(chatId, "You have successfully subscribed to: " + city + " ePaper. ✅\n\nSend 'unsubscribe' any time you wish."));
            } else {
                executeAsync(new SendMessage(chatId,"Could not find your city 🤔. To see a list of available cities, enter 'HT' or 'TOI'."));
            }
        }
    }
}

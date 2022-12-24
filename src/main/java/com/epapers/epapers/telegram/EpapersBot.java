package com.epapers.epapers.telegram;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private static final String PROMPT_STRING = """
        Hello there!
        
        👉 Enter publication: /HT or /TOI.
        
        👉 Enter '/download <copy_paste_edition>'
        
        👉 Have patience! 🙂
        
        👉 Enter '/subscribe' to get ePapers daily at 8:00 A.M.
        
        
        /help to view this again
        """;

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        new Thread(() -> {
            if (update.hasMessage() && !update.getMessage().getText().isEmpty()) {
                String chatId = update.getMessage().getChatId().toString();
                User user = update.getMessage().getFrom();

                try {
                    String userMessage = update.getMessage().getText().toUpperCase();
                    StringBuilder editionPrompt = new StringBuilder();
                    switch (userMessage) {
                        case "/HELP":
                            executeAsync(new SendMessage(chatId, PROMPT_STRING));
                            break;
                        case "/REVEAL_USERS":
                            List<EpapersUser> allUsers = userService.getAllUsers();
                            executeAsync(new SendMessage(chatId, allUsers.toString()));
                            break;
                        case "/REVEAL_SUBSCRIBERS":
                            List<EpapersSubscription> allSubscribers = subscriptionService.getAllSubscriptions();
                            executeAsync(new SendMessage(chatId, allSubscribers.toString()));
                            break;
                        case "/REFRESH_USERS":
                            userService.refreshDB();
                            executeAsync(new SendMessage(chatId, "Cleared User History 👍"));
                            break;
                        case "/HTBNG":
                            executeAsync(new SendMessage(chatId,
                                    "🎉 Cool! Preparing HT ePaper for : " + BENGALURU_CITY_KANNADA + " 🎉"));
                            Epaper htpdf = (Epaper) ePaperService.getHTpdf("102", AppUtils.getTodaysDate())
                                    .get(EPAPER_KEY_STRING);
                            executeAsync(new SendMessage(chatId,
                                    ACCESS_STRING + String.format(FILE_ACCESS_URL, htpdf.getFile().getName())));
                            executeAsync(new SendDocument(chatId, new InputFile(htpdf.getFile())));
                            break;
                        case "/TOIBNG":
                            executeAsync(new SendMessage(chatId,
                                    "🎉 Cool! Preparing TOI ePaper for " + BENGALURU_CITY_KANNADA + " 🎉"));
                            Epaper toipdf = (Epaper) ePaperService.getTOIpdf("toibgc", AppUtils.getTodaysDate())
                                    .get(EPAPER_KEY_STRING);
                            executeAsync(new SendMessage(chatId,
                                    ACCESS_STRING + String.format(FILE_ACCESS_URL, toipdf.getFile().getName())));
                            executeAsync(new SendDocument(chatId, new InputFile(toipdf.getFile())));
                            break;
                        case "/KP":
                            executeAsync(new SendMessage(chatId,
                                    "🎉 Cool! Preparing KP ePaper for " + BENGALURU_CITY_KANNADA + " 🎉"));
                            Epaper kpPdf = (Epaper) ePaperService.getKannadaPrabha().get(EPAPER_KEY_STRING);
                            executeAsync(new SendMessage(chatId,
                                    ACCESS_STRING + String.format(FILE_ACCESS_URL, kpPdf.getFile().getName())));
                            executeAsync(new SendDocument(chatId, new InputFile(kpPdf.getFile())));
                            break;
                        case "/HT":
                            editionPrompt.append(
                                    "💡 Copy the WHOLE text for your city and send: '/download <copied_text>'\n\n");
                            editionPrompt.append("Example: /download Bengaluru_102_HT\n\n");
                            ePaperService.getHTEditionList()
                                    .forEach(edition -> editionPrompt.append("👉 ").append(edition.getEditionName())
                                            .append("_").append(edition.getEditionId()).append("_").append("HT\n\n"));
                            executeAsync(new SendMessage(chatId, editionPrompt.toString()));
                            break;
                        case "/TOI":
                            editionPrompt.append(
                                    "💡 Copy the WHOLE text for your city and send: '/download <copied_text>'\n\n");
                            editionPrompt.append("Example: /download Bangalore_toibgc_TOI\n\n");
                            ePaperService.getTOIEditionList()
                                    .forEach(edition -> editionPrompt.append("👉 ").append(edition.getEditionName())
                                            .append("_").append(edition.getEditionId()).append("_").append("TOI\n\n"));
                            executeAsync(new SendMessage(chatId, editionPrompt.toString()));
                            break;
                        case "/SUBSCRIBE":
                            executeAsync(new SendMessage(chatId,
                                    "Alright! Please enter '/subscribe <city>' to start your daily subscription.\n\n\nP.S.\nTo unsubscribe, please enter '/unsubscribe'."));
                            break;
                        case "/UNSUBSCRIBE":
                            boolean unsubscribed = subscriptionService.removeSubscription(chatId);
                            if (!unsubscribed) {
                                executeAsync(new SendMessage(chatId,
                                        "You are not subscribed to any edition!\n\n/subscribe to start daily subscription."));
                            } else {
                                executeAsync(new SendMessage(chatId, "Awww. Sad to hear that! 😢"));
                            }
                            break;
                        default:
                            if (userMessage.startsWith("/DOWNLOAD ")) {
                                sendPDF(chatId, user, userMessage);
                            } else if (userMessage.startsWith("/SUBSCRIBE ")) {
                                subscribeNewUser(chatId, user, userMessage);
                            } else {
                                if (!update.getMessage().isGroupMessage()) {
                                    executeAsync(new SendMessage(chatId, PROMPT_STRING));
                                }
                            }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handleErrors(chatId, "Could not process that request.");
                }
            }
        }).start();
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
                EpapersSubscription subscription = new EpapersSubscription(chatId, user, subscribedEditions, true);
                subscriptionService.addSubscription(subscription);
                executeAsync(new SendMessage(chatId, "You have successfully subscribed to: " + city + " ePaper. ✅\n\nSend '/unsubscribe' any time you wish."));
            } else {
                executeAsync(new SendMessage(chatId,"Could not find your city 🤔. To see a list of available cities, enter '/HT' or '/TOI'."));
            }
        }
    }

    public boolean sendSubscriptionMessage(String chatId, String message, File file) {
        try {
            executeAsync(new SendMessage(chatId, message));
            if (file != null && !AppUtils.isLargeFile(file, "TELEGRAM")) {
                executeAsync(new SendDocument(chatId, new InputFile(file)));
            }
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to send subscribed message to {}", chatId);
            return false;
        }
    }

    public void handleErrors(String chatId, String feedback) {
        try {
            executeAsync(new SendMessage(chatId, feedback));
        } catch (TelegramApiException e1) {
            log.error("TELEGRAM-SERVICE: Failed to send feedback");
        }
    }

    public void triggerSubscriptions() {
        ExecutorService executor = Executors.newCachedThreadPool();
        String today = AppUtils.getTodaysDate();
        List<EpapersSubscription> subscriptions = subscriptionService.getAllSubscriptions()
                                                    .stream().filter(sub -> sub.getIsActive()).collect(Collectors.toList());

        log.info("Processing all subscriptions - {}", subscriptions);

        subscriptions.forEach(subscription -> {
            Runnable runnableTask = () -> {
                String chatId = subscription.getChatId();
                Map<String, String> editions = subscription.getEditions();
                String toiEdition = editions.get("TOI");
                String htEdition = editions.get("HT");

                if (htEdition != null) {
                    try {
                        Epaper htPdf = (Epaper) ePaperService.getHTpdf(htEdition, today).get("epaper");
                        sendSubscriptionMessage(chatId, "Access your HT ePaper here: "+ String.format(FILE_ACCESS_URL, htPdf.getFile().getName()), htPdf.getFile());

                        if (htEdition.equals("102")) {
                            log.info("Sending surprise paper - Kannada Prabha to user/group - {}", chatId);
                            Epaper kpPdf = (Epaper) ePaperService.getKannadaPrabha().get("epaper");
                            sendSubscriptionMessage(chatId,"Access today's bonus KP ePaper here: "+ String.format(FILE_ACCESS_URL, kpPdf.getFile().getName()),kpPdf.getFile());
                        }
                    } catch (Exception e) {
                        log.error("HT Subscription service failed. - {}", e);
                    }
                }

                if (toiEdition != null) {
                    try {
                        Epaper toiPdf = (Epaper) ePaperService.getTOIpdf(toiEdition, today).get("epaper");
                        sendSubscriptionMessage(chatId, "Access your TOI ePaper here: " + String.format(FILE_ACCESS_URL, toiPdf.getFile().getName()), toiPdf.getFile());
                    } catch (Exception e) {
                        log.error("TOI Subscription service failed. - {}", e);
                    }
                }

                log.info("ePapers successfully sent to - {}", chatId);
            };

            executor.submit(runnableTask);
        });
    }
}

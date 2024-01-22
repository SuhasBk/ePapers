package com.epapers.epapers.telegram;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.EpapersSubscription;
import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.SubscriptionService;
import com.epapers.epapers.service.UserService;
import com.epapers.epapers.util.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


@Component
@Slf4j
public class EpapersBot extends TelegramLongPollingBot {
    @Autowired
    EpaperService ePaperService;

    @Autowired
    UserService userService;

    @Autowired
    SubscriptionService subscriptionService;

    private static final String BOT_TOKEN = AppConfig.TELEGRAM_BOT_TOKEN;
    private static final String BOT_USERNAME = "ePapers";
    private static final String SERVER_URL = AppConfig.HOSTNAME;
    private static final String FILE_ACCESS_URL = SERVER_URL + "/api/file?name=%s";
    private static final String BENGALURU_CITY_KANNADA = "ನಮ್ಮ ಬೆಂಗಳೂರು 🤘";
    private static final String EPAPER_KEY_STRING = "epaper";
    private static final String ACCESS_STRING = "Access it using: ";
    private static final String PROMPT_STRING = """
        Hello there!
        
        👉 Choose publication: /HT or /TOI.

        👉 Search your city and copy the edition from the list.
        
        👉 Send '/download <copied_edition>'.
        
        👉 Have patience! 🙂
        
        👉 Send '/subscribe' to get ePapers daily at 8:00 A.M.
        
        
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
                    log.info("\n\nINCOMING MESSAGE: {} FROM USER: {}\n\n", userMessage, user.getUserName()+"_"+user.getFirstName()+"_"+user.getLastName());
                    StringBuilder editionPrompt = new StringBuilder();

                    switch (userMessage) {
                        case "/HELP" -> executeAsync(new SendMessage(chatId, PROMPT_STRING));
                        case "/REVEAL" -> {
                            List<EpapersUser> allUsers = userService.getAllUsers();
                            executeAsync(new SendMessage(chatId, "USERS:\n" + allUsers.toString()));
                            List<EpapersSubscription> allSubscribers = subscriptionService.getAllSubscriptions();
                            executeAsync(new SendMessage(chatId, "SUBSCRIBERS:\n" + allSubscribers.toString()));
                        }
                        case "/CLEAR" -> {
                            File currDir = new File(".");
                            File[] pdfFiles = currDir.listFiles(file -> file.getName().endsWith(".pdf"));
                            for (File file : pdfFiles) {
                                log.info("Deleting file: " + file.getName());
                                AppUtils.deleteFile(file);
                            }
                            executeAsync(new SendMessage(chatId, "Cleared Cached Files 👍"));
                        }
                        case "/CACHE" -> {
                            executeAsync(new SendMessage(chatId, "Brace yourselves. Caching today's ePapers for subscribers !!! 🫠"));
                            this.triggerSubscriptions(true);
                        }
                        case "/THROW" -> {
                            executeAsync(new SendMessage(chatId, "Brace yourselves. Triggering ePapers to ALL subscribers !!! 🫠"));
                            this.triggerSubscriptions(false);
                        }
                        case "/HTBNG" -> {
                            executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing HT ePaper for : " + BENGALURU_CITY_KANNADA + " 🎉"));
                            Epaper htpdf = (Epaper) ePaperService.getHTpdf("102", AppUtils.getTodaysDate()).get(EPAPER_KEY_STRING);
                            executeAsync(new SendMessage(chatId, ACCESS_STRING + String.format(FILE_ACCESS_URL, htpdf.getFile().getName())));
                            executeAsync(new SendDocument(chatId, new InputFile(htpdf.getFile())));
                        }
                        case "/TOIBNG" -> {
                            executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing TOI ePaper for " + BENGALURU_CITY_KANNADA + " 🎉"));
                            Epaper toipdf = (Epaper) ePaperService.getTOIpdf("toibgc", AppUtils.getTodaysDate()).get(EPAPER_KEY_STRING);
                            executeAsync(new SendMessage(chatId, ACCESS_STRING + String.format(FILE_ACCESS_URL, toipdf.getFile().getName())));
                            executeAsync(new SendDocument(chatId, new InputFile(toipdf.getFile())));
                        }
                        case "/KP" -> {
                            executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing KP ePaper for " + BENGALURU_CITY_KANNADA + " 🎉"));
                            Epaper kpPdf = (Epaper) ePaperService.getKannadaPrabha().get(EPAPER_KEY_STRING);
                            executeAsync(new SendMessage(chatId, ACCESS_STRING + String.format(FILE_ACCESS_URL, kpPdf.getFile().getName())));
                            executeAsync(new SendDocument(chatId, new InputFile(kpPdf.getFile())));
                        }
                        case "/HT" -> {
                            editionPrompt.append("💡 From the below list, copy the WHOLE text and then send: '/download <copied_text>'\n\n");
                            editionPrompt.append("Example: /download Bengaluru_102_HT\n\n");
                            ePaperService.getHTEditionList().forEach(edition -> 
                                editionPrompt
                                .append("👉 ")
                                .append(edition.getEditionName())
                                .append(" - Copy this: '")
                                .append(edition.getEditionName())
                                .append("_")
                                .append(edition.getEditionId())
                                .append("_")
                                .append("HT'\n\n"));
                            executeAsync(new SendMessage(chatId, editionPrompt.toString()));
                        }
                        case "/TOI" -> {
                            editionPrompt.append("💡 From the below list, copy the WHOLE text and then send: '/download <copied_text>'\n\n");
                            editionPrompt.append("Example: /download Bangalore_toibgc_TOI\n\n");
                            ePaperService.getTOIEditionList().forEach(edition -> 
                                editionPrompt
                                .append("👉 ")
                                .append(edition.getEditionName())
                                .append(" - Copy this: '")
                                .append(edition.getEditionName())
                                .append("_")
                                .append(edition.getEditionId())
                                .append("_")
                                .append("TOI'\n\n"));
                            executeAsync(new SendMessage(chatId, editionPrompt.toString()));
                        }
                        case "/SUBSCRIBE" ->
                            executeAsync(new SendMessage(chatId, "Alright! Please enter '/subscribe <city>' to start your daily subscription.\n\n\nP.S.\nTo unsubscribe, please enter '/unsubscribe'."));
                        case "/UNSUBSCRIBE" -> {
                            boolean unsubscribed = subscriptionService.removeSubscription(chatId);
                            if (!unsubscribed) {
                                executeAsync(new SendMessage(chatId, "You are not subscribed to any edition!\n\n/subscribe to start daily subscription."));
                            } else {
                                executeAsync(new SendMessage(chatId, "Awwww. Sad to hear that! 😢"));
                            }
                        }
                        default -> {
                            if (userMessage.startsWith("/DOWNLOAD ")) {
                                sendPDF(chatId, user, userMessage);
                            } else if (userMessage.startsWith("/SUBSCRIBE ")) {
                                subscribeNewUser(chatId, user, userMessage);
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
                handleErrors(chatId, "Something went wrong while downloading ePaper. 😢\n\n Try downloading it directly from " + SERVER_URL);
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
                executeAsync(new SendMessage(chatId, "You have successfully subscribed to: " + city + " ePaper. ✅\n\nSend '/unsubscribe' any time you wish to stop receiving papers."));
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

    public void triggerSubscriptions(Boolean cacheOnly) {
        ExecutorService executor = Executors.newCachedThreadPool();
        String today = AppUtils.getTodaysDate();
        List<EpapersSubscription> subscriptions = subscriptionService.getAllSubscriptions()
                .stream()
                .filter(EpapersSubscription::getIsActive)
                .collect(Collectors.toList());

        log.info("Processing all subscriptions - {}", subscriptions);

        subscriptions.forEach(subscription -> {
            Runnable runnableTask = () -> {
                String chatId = subscription.getChatId();
                Map<String, String> editions = subscription.getEditions();
                String toiEdition = editions.get("TOI");
                String htEdition = editions.get("HT");

                if (htEdition != null) {
                    try {
                        // Epaper htPdf = (Epaper) ePaperService.getHTpdf(htEdition, today).get(EPAPER_KEY_STRING);
                        // if (!cacheOnly) {
                        //     sendSubscriptionMessage(chatId, "Access your HT ePaper here: "+ String.format(FILE_ACCESS_URL, htPdf.getFile().getName()), htPdf.getFile());
                        // }

                        if (htEdition.equals("102")) {
                            log.info("Sending surprise paper - Kannada Prabha to user/group - {}", chatId);
                            Epaper kpPdf = (Epaper) ePaperService.getKannadaPrabha().get(EPAPER_KEY_STRING);
                            if (!cacheOnly) {
                                sendSubscriptionMessage(chatId,"Access today's bonus KP ePaper here: "+ String.format(FILE_ACCESS_URL, kpPdf.getFile().getName()),kpPdf.getFile());
                            }
                        }
                    } catch (Exception e) {
                        log.error("HT/KP Subscription service failed. - {}", e);
                    }
                }

                if (toiEdition != null) {
                    try {
                        Epaper toiPdf = (Epaper) ePaperService.getTOIpdf(toiEdition, today).get(EPAPER_KEY_STRING);
                        if(!cacheOnly) {
                            sendSubscriptionMessage(chatId, "Access your TOI ePaper here: " + String.format(FILE_ACCESS_URL, toiPdf.getFile().getName()), toiPdf.getFile());
                        }
                    } catch (Exception e) {
                        log.error("TOI Subscription service failed. - {}", e);
                    }
                }

                log.info("ePapers successfully sent to - {}", chatId);
                
                try {
                    execute(new SendMessage(chatId, "It's feedback time! Please fill out this form for any queries/suggestions you might have: https://forms.gle/KM12YNRUdqrPZN2K6", "", true, false, null, null, null, true, false));
                } catch (Exception e) {
                    log.error("FAILED TO SEND FEEDBACK!, {}", e);
                }
            };

            executor.submit(runnableTask);
        });
    }
}

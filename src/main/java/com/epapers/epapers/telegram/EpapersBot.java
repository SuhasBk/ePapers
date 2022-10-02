package com.epapers.epapers.telegram;

import java.io.File;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.epapers.epapers.EpapersApplication;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.UserService;


@Component
public class EpapersBot extends TelegramLongPollingBot {
    @Autowired
    EpaperService ePaperService;

    @Autowired
    UserService userService;

    @Autowired
    String TODAYS_DATE;

    public static String BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
    public static String BOT_USERNAME = "ePapers";
    public static String POLL_ANSWER;
    public static String FILE_ACCESS_URL = "https://epapers.onrender.com/api/file?name=%s";
    public static String BENGALURU_CITY_KANNADA = "ನಮ್ಮ ಬೆಂಗಳೂರು 🤘";

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
        if(update.hasMessage() && !update.getMessage().getText().isEmpty()) {
            String chatId = update.getMessage().getChatId().toString();
            User user = update.getMessage().getFrom();

            try {
                String userMessage = update.getMessage().getText().toUpperCase();
                StringBuilder editions = new StringBuilder();
                switch(userMessage) {
                    case "HTBNG":
                        executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing HT ePaper for : " + BENGALURU_CITY_KANNADA + " 🎉"));
                        Epaper htPdf = (Epaper) ePaperService.getHTpdf("102", EpapersApplication.getDate()).get("epaper");
                        executeAsync(new SendMessage(chatId, "Access it using: " + String.format(FILE_ACCESS_URL, htPdf.getFile().getName())));
                        executeAsync(new SendDocument(chatId, new InputFile(htPdf.getFile())));
                        break;
                    case "TOIBNG":
                        executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing TOI ePaper for " + BENGALURU_CITY_KANNADA + " 🎉"));
                        Epaper toiPdf = (Epaper) ePaperService.getTOIpdf("toibgc", EpapersApplication.getDate()).get("epaper");
                        executeAsync(new SendMessage(chatId, "Access it using: " + String.format(FILE_ACCESS_URL, toiPdf.getFile().getName())));
                        executeAsync(new SendDocument(chatId, new InputFile(toiPdf.getFile())));
                        break;
                    case "HT":
                        editions.append("💡 Copy the WHOLE text for your city and type: 'download <copied_text>'\n\n");
                        editions.append("Example: download Bengaluru_102_HT\n\n");
                        ePaperService.getHTEditionList().forEach(edition -> editions.append("👉 "+edition.getEditionName() + "_" + Double.valueOf(edition.getEditionId()).intValue() + "_" + "HT\n\n"));
                        executeAsync(new SendMessage(chatId, editions.toString()));
                        break;
                    case "TOI":
                        editions.append("💡 Copy the WHOLE text for your city and type: 'download <copied_text>'\n\n");
                        editions.append("Example: download Bangalore_toibgc_TOI\n\n");
                        ePaperService.getTOIEditionList().forEach(edition -> editions.append("👉 "+edition.getEditionName() + "_" + edition.getEditionId() + "_" + "TOI\n\n"));
                        executeAsync(new SendMessage(chatId, editions.toString()));
                        break;
                    default:
                        if(userMessage.startsWith("DOWNLOAD ")) {
                            String payload = userMessage.trim().split(" ")[1];
                            String[] metaData = payload.split("_");
                            if(metaData.length == 3) {
                                String city = (metaData[0].toLowerCase().equals("bangalore") || metaData[0].toLowerCase().equals("bengaluru")) ? BENGALURU_CITY_KANNADA : metaData[0];
                                String editionId = metaData[1];
                                String publication = metaData[2];
                                EpapersUser epapersUser = new EpapersUser(chatId, user, editionId, city, TODAYS_DATE + new Date().getTime(), 1);
                                if(!userService.canAccess(epapersUser)) {
                                    executeAsync(new SendMessage(chatId, "Access denied ❌. Quota Exceeded."));
                                    return;
                                }
                                executeAsync(new SendMessage(chatId, "🎉 Cool! Preparing " + publication + " ePaper for : " + city + " 🎉"));
                                
                                try{
                                    switch(publication) {
                                        case "HT":
                                            Epaper HTpdf = (Epaper) ePaperService.getHTpdf(editionId, EpapersApplication.getDate()).get("epaper");
                                            executeAsync(new SendMessage(chatId, "Access it using: " + String.format(FILE_ACCESS_URL, HTpdf.getFile().getName())));
                                            executeAsync(new SendDocument(chatId, new InputFile(HTpdf.getFile())));
                                            break;
                                        case "TOI":
                                            Epaper TOIpdf = (Epaper) ePaperService.getTOIpdf(editionId.toLowerCase(), EpapersApplication.getDate()).get("epaper");
                                            executeAsync(new SendMessage(chatId, "Access it using: " + String.format(FILE_ACCESS_URL, TOIpdf.getFile().getName())));
                                            File file = TOIpdf.getFile();
                                            if(file.length() / (1024*1024) < 40) {
                                                executeAsync(new SendDocument(chatId, new InputFile(file)));
                                            }
                                            break;
                                    }
                                } catch(Exception e) {
                                    e.printStackTrace();
                                    try {
                                        executeAsync(new SendMessage(chatId, "Something went wrong while downloading ePaper. 😢\n\n Try downloading it directly from https://epapers.onrender.com"));
                                    } catch (TelegramApiException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            executeAsync(new SendMessage(chatId, "Hello there!\n\n👉 Enter publication: HT or TOI.\n\n👉 Enter 'download <copy_paste_edition>'\n\n👉 Have patience! 🙂"));
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }  
}

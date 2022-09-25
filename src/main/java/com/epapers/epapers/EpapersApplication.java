package com.epapers.epapers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.DesktopApp;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class EpapersApplication {

	@Bean
	public static String getDate() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String TODAYS_DATE = dtf.format(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
		log.info("Today's date (default) is : {}", TODAYS_DATE);
		return TODAYS_DATE;
	}

	public static void main(String[] args) {
		if (args.length != 0) {
			if (args[0].equals("HT") || args[0].equals("TOI")) {
				try {
					DesktopApp.download(args[0], getDate());
				} catch (Exception e) {
					log.error("Something went wrong...", e);
				}
			}
		} else {
			SpringApplication.run(EpapersApplication.class, args);
			new Thread(() -> {
				try {
					TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
					botsApi.registerBot(new EpapersBot());
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
			}).start();
		}
	}

	@Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
	public void keepAlive() {
		try {
			new URL("https://epapers.onrender.com/").openStream();
			System.out.println("keeping it alive!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
	public void cleaUp() {
		try {
			File currDir = new File(".");
			for(File file: currDir.listFiles(file -> file.getName().endsWith(".pdf"))) {
				file.delete();
				System.out.println("Old files purged successfully!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Bean
	public JavaMailSender getJavaMailSender() {
		String username = System.getenv("EMAIL_ID");
		String password = System.getenv("EMAIL_PASSWORD");

		// if(username == null || password == null || username.isEmpty() ||
		// password.isEmpty()) {
		// throw new RuntimeException("Email credentials not configured. Failed to start
		// HT.");
		// }

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

}

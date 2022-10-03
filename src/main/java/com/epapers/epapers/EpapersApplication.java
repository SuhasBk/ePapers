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
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.epapers.epapers.util.AppUtils;
import com.epapers.epapers.util.DesktopApp;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableMongoRepositories
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
		System.out.println(Runtime.getRuntime().maxMemory());
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

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
	public void cleanUp() {
		try {
			File currDir = new File(".");
			for(File file: currDir.listFiles(file -> file.getName().endsWith(".pdf"))) {
				AppUtils.deleteFile(file);
			}
			System.out.println("Old files purged successfully!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
	public void GC() {
		try {
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata")
	public void refreshDB() {
		try {
			new URL("https://epapers.onrender.com/api/epapers/refreshDB").openStream();
			System.out.println("Starting a new day. 😊");
		} catch (Exception e) {
			System.out.println("Failed to refresh db.");
		}
	}

	@Bean
	public JavaMailSender getJavaMailSender() {
		String username = System.getenv("EMAIL_ID");
		String password = System.getenv("EMAIL_PASSWORD");

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

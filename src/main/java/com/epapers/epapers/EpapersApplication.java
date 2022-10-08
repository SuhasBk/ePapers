package com.epapers.epapers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;

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

}

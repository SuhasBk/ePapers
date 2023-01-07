package com.epapers.epapers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

//@SpringBootApplication(exclude = org.telegram.telegrambots.starter.TelegramBotStarterConfiguration.class)
 @SpringBootApplication
@EnableMongoRepositories
@EnableScheduling
public class EpapersApplication {

	public static void main(String[] args) {
		SpringApplication.run(EpapersApplication.class, args);
	}
}

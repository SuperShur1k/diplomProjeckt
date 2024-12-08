package com.example.telegramBotNailsBooking;

import com.example.telegramBotNailsBooking.bot.BotController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
@EnableScheduling
public class TelegramBotNailsBookingApplication {

	public static void main(String[] args) throws TelegramApiException {
		// Запуск Spring Boot приложения и получение контекста
		ApplicationContext context = SpringApplication.run(TelegramBotNailsBookingApplication.class, args);

		// Получение экземпляра BotController из контекста Spring
		BotController botController = context.getBean(BotController.class);
	}
}

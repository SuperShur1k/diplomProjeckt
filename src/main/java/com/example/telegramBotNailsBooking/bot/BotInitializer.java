package com.example.telegramBotNailsBooking.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BotInitializer {
    private static final Logger logger = LoggerFactory.getLogger(BotInitializer.class);

    @Autowired
    private BotController bot;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            logger.info("Bot registered successfully");
        } catch (TelegramApiException e) {
            logger.error("Error occurred while registering the bot: {}", e.getMessage(), e);
        }
    }
}

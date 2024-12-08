package com.example.telegramBotNailsBooking.bot;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("classpath:application.properties")
public class BotConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String token;

    @PostConstruct
    public void init() {
        logger.info("Bot Config initialized: name={}, token={}", botName, token);
    }

}

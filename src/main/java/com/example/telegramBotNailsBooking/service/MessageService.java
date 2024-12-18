package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.bot.BotSender;
import com.example.telegramBotNailsBooking.model.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Locale;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final ObjectProvider<BotSender> botSenderProvider;
    private final MessageSource messageSource;

    @Autowired
    public MessageService(ObjectProvider<BotSender> botSenderProvider, MessageSource messageSource) {
        this.botSenderProvider = botSenderProvider;
        this.messageSource = messageSource;
    }

    /**
     * Отправка простого сообщения
     */
    public void sendMessage(Long chatId, String text) {
        BotSender botSender = botSenderProvider.getIfAvailable();
        if (botSender != null) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            botSender.sendMessage(chatId, message);
        } else {
            logger.error("BotSender is not available");
        }
    }

    /**
     * Получение локализованного сообщения
     */
    public String getLocalizedMessage(String key, String languageCode, Object... args) {
        Locale locale = switch (languageCode) {
            case "ru" -> Locale.forLanguageTag("ru");
            case "uk" -> Locale.forLanguageTag("uk");
            case "en" -> Locale.forLanguageTag("en");
            default -> Locale.forLanguageTag("en");
        };

        logger.info("Using locale: {}", locale); // Логируем текущую локаль
        return messageSource.getMessage(key, args, locale);
    }


    /**
     * Отправка локализованного сообщения
     */
    public void sendLocalizedMessage(Long chatId, String key, String languageCode, Object... args) {
        String localizedText = getLocalizedMessage(key, languageCode, args);
        sendMessage(chatId, localizedText);
    }

    /**
     * Отправка локализованного сообщения с inline-клавиатурой
     */
    public void sendLocalizedMessageWithInlineKeyboard(Long chatId, String key, String languageCode, InlineKeyboardMarkup keyboardMarkup, Object... args) {
        String localizedText = getLocalizedMessage(key, languageCode, args);
        BotSender botSender = botSenderProvider.getIfAvailable();
        if (botSender != null) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(localizedText);
            message.setReplyMarkup(keyboardMarkup);
            botSender.sendMessage(chatId, message);
        } else {
            logger.error("BotSender is not available");
        }
    }

    public void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        BotSender botSender = botSenderProvider.getIfAvailable();
        if (botSender != null) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            message.setReplyMarkup(keyboardMarkup);
            botSender.sendMessage(chatId, message);
        } else {
            logger.error("BotSender is not available");
        }
    }

    public String getLocalizedServiceName(Services service, String languageCode) {
        return switch (languageCode) {
            case "ru" -> service.getNameRu();
            case "uk" -> service.getNameUk();
            default -> service.getNameEn();
        };
    }
}

package com.example.telegramBotNailsBooking.bot;

import com.example.telegramBotNailsBooking.bot.commands.CommandController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class BotController extends TelegramLongPollingBot implements BotSender {

    private static final Logger log = LoggerFactory.getLogger(BotController.class);

    @Autowired
    private CommandController commandController;

    @Autowired
    private BotConfig config;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String text = message.getText();

            log.info("Received message from chat ID {}: {}", chatId, text);
            commandController.handleCommand(chatId, text, update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery(), update);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery, Update update) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackData = callbackQuery.getData();

        log.info("Received callback query from chat ID {}: {}", chatId, callbackData);

        // Передаем обработку callbackData как команды
        commandController.handleCommand(chatId, callbackData, update);
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void sendMessage(Long chatId, String text) {

    }

    @Override
    public void sendMessage(Long chatId, SendMessage message) {
        try {
            log.info("Sending message to chat ID {}: {}", chatId, message.getText());
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message to chat ID {}: {}", chatId, e.getMessage());
        }
    }
}

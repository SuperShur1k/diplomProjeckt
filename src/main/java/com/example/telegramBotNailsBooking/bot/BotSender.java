package com.example.telegramBotNailsBooking.bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface BotSender {
    void sendMessage(Long chatId, String text);
    void sendMessage(Long chatId, SendMessage message);
}

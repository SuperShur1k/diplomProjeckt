package com.example.telegramBotNailsBooking.service.buttons;

import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Service
public class MenuService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    private List<InlineKeyboardButton> createButtonRow(String messageKey, String callbackData, String languageCode) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(messageService.getLocalizedMessage(messageKey, languageCode));
        button.setCallbackData(callbackData);
        return List.of(button);
    }

    // Метод для создания общего меню
    public InlineKeyboardMarkup getMenuInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(createButtonRow("menu.booking.manager", "/book_service", languageCode));
        rowsInline.add(createButtonRow("menu.settings", "/settings", languageCode));
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    // Метод для обработки команды /settings
    public void handleSettingsCommand(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(createButtonRow("settings.change.name", "/change_name", languageCode));
        rowsInline.add(createButtonRow("settings.change.language", "/change_language", languageCode));
        rowsInline.add(createButtonRow("settings.change.phone", "/change_phone", languageCode));
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "settings.menu.prompt", languageCode, inlineKeyboardMarkup);
    }

    // Метод для отображения менеджера бронирования
    public void bookingManagerButton(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(createButtonRow("booking.book.service", "/book", languageCode));
        rowsInline.add(createButtonRow("booking.info", "/book_info", languageCode));
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "booking.menu.prompt", languageCode, inlineKeyboardMarkup);
    }
}
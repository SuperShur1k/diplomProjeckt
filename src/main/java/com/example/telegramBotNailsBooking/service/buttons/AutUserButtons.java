package com.example.telegramBotNailsBooking.service.buttons;

import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Service
public class AutUserButtons {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MasterRepository masterRepository;

    /**
     * Универсальный метод для создания кнопки
     */
    private List<InlineKeyboardButton> createButtonRow(String messageKey, String callbackData, String languageCode) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(messageService.getLocalizedMessage(messageKey, languageCode));
        button.setCallbackData(callbackData);
        return List.of(button);
    }

    private List<InlineKeyboardButton> createButtonRow(String textKey1, String callbackData1, String languageCode,
                                                       String textKey2, String callbackData2, String languageCode2) {
        String buttonText1 = messageService.getLocalizedMessage(textKey1, languageCode);
        String buttonText2 = messageService.getLocalizedMessage(textKey2, languageCode2);

        InlineKeyboardButton button1 = new InlineKeyboardButton(buttonText1);
        button1.setCallbackData(callbackData1);

        InlineKeyboardButton button2 = new InlineKeyboardButton(buttonText2);
        button2.setCallbackData(callbackData2);

        return List.of(button1, button2);
    }


    /**
     * Клавиатура для аутентифицированного пользователя
     */
    public InlineKeyboardMarkup getAuthenticatedInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопки "reviews" и "price list" в одной строке
        rowsInline.add(createButtonRow("button.reviews", "/review", languageCode, "button.price.list", "/services", languageCode));

        // Кнопки "menu" и "help" в одной строке
        rowsInline.add(createButtonRow("button.menu", "/menu", languageCode, "button.help", "/help", languageCode));

        // Проверка ролей пользователя
        Users users = userRepository.findByChatId(chatId);
        if (users != null && users.getRole() == Users.Role.ADMIN) {
            rowsInline.add(createButtonRow("button.admin.panel", "/admin", languageCode));
        }
        if (users != null && masterRepository.existsByChatId(users.getChatId())) {
            rowsInline.add(createButtonRow("button.master.panel", "/master", languageCode));
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }


    /**
     * Клавиатура для выбора типа записей
     */
    public void showBookingInfoMenu(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(createButtonRow("button.confirmed.bookings", "/book_confirmed", languageCode));
        rowsInline.add(createButtonRow("button.completed.bookings", "/book_completed", languageCode));
        rowsInline.add(createButtonRow("button.cancelled.bookings", "/book_cancelled", languageCode));
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "message.choose.booking.type", languageCode, inlineKeyboardMarkup);
    }

    /**
     * Клавиатура с кнопкой "Отмена"
     */
    public InlineKeyboardMarkup getCancelInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(createButtonRow("button.cancel", "/cancel", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    /**
     * Панель мастера
     */
    public InlineKeyboardMarkup masterPanel(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(createButtonRow("button.view.appointments", "/view_appointments", languageCode));
        rowsInline.add(createButtonRow("button.write.client", "/master_write_client", languageCode));
        rowsInline.add(createButtonRow("button.record.client", "/master_record_client", languageCode));
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }
}
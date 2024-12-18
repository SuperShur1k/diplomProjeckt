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
public class AdminButtons {

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

    public InlineKeyboardMarkup getAdminInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Настройка ролей"
        rowsInline.add(createButtonRow("admin.roles.settings", "/role", languageCode));

        // Кнопка "Настройка дат"
        rowsInline.add(createButtonRow("admin.date.settings", "/date", languageCode));

        // Кнопка "Настройка услуг"
        rowsInline.add(createButtonRow("admin.service.settings", "/service", languageCode));

        // Кнопка "Другие действия"
        rowsInline.add(createButtonRow("admin.other.actions", "/other_actions", languageCode));

        // Кнопка "Назад"
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getOtherActionsInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Упрощаем добавление кнопок с помощью метода addLocalizedButton
        rowsInline.add(createButtonRow("button.reply.help", "/reply_to_help", languageCode));
        rowsInline.add(createButtonRow("button.write.client", "/write_to_client", languageCode));
        rowsInline.add(createButtonRow("button.write.master", "/write_to_master", languageCode));
        rowsInline.add(createButtonRow("button.manage.appointments", "/appointments_manage", languageCode));
        rowsInline.add(createButtonRow("button.back", "/admin", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public void getDateInlineKeyboard(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Добавить дату и время
        rowsInline.add(createButtonRow("button.add.date", "/add_date", languageCode, "button.add.time", "/add_time", languageCode));

        // Удалить дату и время
        rowsInline.add(createButtonRow("button.delete.date", "/del_date", languageCode, "button.delete.time", "/del_time", languageCode));

        // Назад
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Локализованное сообщение
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "message.add.date.time", languageCode, inlineKeyboardMarkup);
    }

    public void getServiceInlineKeyboard(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Добавляем кнопки с помощью createButtonRow
        rowsInline.add(createButtonRow("button.add.service", "/add_service", languageCode));
        rowsInline.add(createButtonRow("button.delete.service", "/del_service", languageCode));
        rowsInline.add(createButtonRow("button.change.price", "/change_price", languageCode));
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "service.manage.menu", languageCode, inlineKeyboardMarkup);
    }

    public void getRoleInlineKeyboard(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(createButtonRow("role.set.admin", "/setadmin", languageCode, "role.set.master", "/add_master", languageCode));

        rowsInline.add(createButtonRow("role.remove.admin", "/del_admin", languageCode, "role.manage.masters", "/manage_masters", languageCode));

        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        // Устанавливаем клавиатуру и отправляем локализованное сообщение
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "role.manage.prompt", languageCode, inlineKeyboardMarkup);
    }

    public InlineKeyboardMarkup getManageAppointmentsKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Добавляем строки кнопок
        rowsInline.add(createButtonRow("button.reschedule.appointment", "/reschedule_appointment", languageCode));
        rowsInline.add(createButtonRow("button.cancel.appointment", "/admin_cancel_appointment", languageCode));
        rowsInline.add(createButtonRow("button.add.client.appointment", "/add_appointment", languageCode));
        rowsInline.add(createButtonRow("button.back", "/back", languageCode));

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }
}
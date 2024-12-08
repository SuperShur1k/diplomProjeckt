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

    public InlineKeyboardMarkup getAuthenticatedInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        List<InlineKeyboardButton> row4 = new ArrayList<>();

        InlineKeyboardButton reviewButton = new InlineKeyboardButton();
        reviewButton.setText(
                "ru".equals(languageCode) ? "Отзывы" : "uk".equals(languageCode) ? "Відгуки" : "Reviews"
        );
        reviewButton.setCallbackData("/review");
        row1.add(reviewButton);

        InlineKeyboardButton serviceButton = new InlineKeyboardButton();
        serviceButton.setText(
                "ru".equals(languageCode) ? "Прайс-лист" : "uk".equals(languageCode) ? "Прайс-лист" : "Price List"
        );
        serviceButton.setCallbackData("/services");
        row1.add(serviceButton);

        InlineKeyboardButton restrictedButton = new InlineKeyboardButton();
        restrictedButton.setText(
                "ru".equals(languageCode) ? "Меню" : "uk".equals(languageCode) ? "Меню" : "Menu"
        );
        restrictedButton.setCallbackData("/menu");
        row2.add(restrictedButton);

        InlineKeyboardButton helpButton = new InlineKeyboardButton();
        helpButton.setText(
                "ru".equals(languageCode) ? "Помощь" : "uk".equals(languageCode) ? "Допомога" : "Help"
        );
        helpButton.setCallbackData("/help");
        row2.add(helpButton);

        Users users = userRepository.findByChatId(chatId);
        if (users != null && users.getRole() == Users.Role.ADMIN) {
            InlineKeyboardButton adminPanelButton = new InlineKeyboardButton();
            adminPanelButton.setText(
                    "ru".equals(languageCode) ? "Панель администратора" : "uk".equals(languageCode) ? "Панель адміністратора" : "Admin Panel"
            );
            adminPanelButton.setCallbackData("/admin");
            row3.add(adminPanelButton);
        }

        if (users != null && masterRepository.existsByChatId(users.getChatId())) {
            InlineKeyboardButton masterPanelButton = new InlineKeyboardButton();
            masterPanelButton.setText(
                    "ru".equals(languageCode) ? "Панель мастера" : "uk".equals(languageCode) ? "Панель мастера" : "Master Panel"
            );
            masterPanelButton.setCallbackData("/master");
            row4.add(masterPanelButton);
        }

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);
        rowsInline.add(row4);
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }

    public void showBookingInfoMenu(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton confirmedButton = new InlineKeyboardButton();
        confirmedButton.setText(
                "ru".equals(languageCode) ? "Подтвержденные записи" : "uk".equals(languageCode) ? "Підтверджені записи" : "Confirmed Bookings"
        );
        confirmedButton.setCallbackData("/book_confirmed");
        row1.add(confirmedButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton completedButton = new InlineKeyboardButton();
        completedButton.setText(
                "ru".equals(languageCode) ? "Завершенные записи" : "uk".equals(languageCode) ? "Завершені записи" : "Completed Bookings"
        );
        completedButton.setCallbackData("/book_completed");
        row2.add(completedButton);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton cancelledButton = new InlineKeyboardButton();
        cancelledButton.setText(
                "ru".equals(languageCode) ? "Отмененные записи" : "uk".equals(languageCode) ? "Скасовані записи" : "Cancelled Bookings"
        );
        cancelledButton.setCallbackData("/book_cancelled");
        row3.add(cancelledButton);
        rows.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(
                "ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back"
        );
        backButton.setCallbackData("/back");
        row4.add(backButton);
        rows.add(row4);

        keyboard.setKeyboard(rows);

        String selectBookingMessage = "ru".equals(languageCode)
                ? "Выберите тип записей для просмотра:"
                : "uk".equals(languageCode)
                ? "Оберіть тип записів для перегляду:"
                : "Select the type of bookings to view:";
        messageService.sendMessageWithInlineKeyboard(chatId, selectBookingMessage, keyboard);
    }

    public InlineKeyboardMarkup getCancelInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(
                "ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel"
        );
        cancelButton.setCallbackData("/cancel");
        row1.add(cancelButton);

        rowsInline.add(row1);
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup masterPanel(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Первая кнопка: "Просмотр записей"
        InlineKeyboardButton checkAppointment = new InlineKeyboardButton();
        checkAppointment.setText(
                "ru".equals(languageCode)
                        ? "Просмотр записей"
                        : "uk".equals(languageCode)
                        ? "Перегляд записів"
                        : "View Appointments"
        );
        checkAppointment.setCallbackData("/view_appointments");
        List<InlineKeyboardButton> row1 = List.of(checkAppointment);

        // Вторая кнопка: "Написать клиенту"
        InlineKeyboardButton writeToClient = new InlineKeyboardButton();
        writeToClient.setText(
                "ru".equals(languageCode)
                        ? "Написать клиенту"
                        : "uk".equals(languageCode)
                        ? "Написати клієнту"
                        : "Write to Client"
        );
        writeToClient.setCallbackData("/master_write_to_client");
        List<InlineKeyboardButton> row2 = List.of(writeToClient);

        // Третья кнопка: "Записать клиента"
        InlineKeyboardButton recordClient = new InlineKeyboardButton();
        recordClient.setText(
                "ru".equals(languageCode)
                        ? "Записать клиента"
                        : "uk".equals(languageCode)
                        ? "Записати клієнта"
                        : "Record Client"
        );
        recordClient.setCallbackData("/master_record_client");
        List<InlineKeyboardButton> row3 = List.of(recordClient);

        // Четвертая кнопка: "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(
                "ru".equals(languageCode)
                        ? "Назад"
                        : "uk".equals(languageCode)
                        ? "Назад"
                        : "Back"
        );
        backButton.setCallbackData("/back");
        List<InlineKeyboardButton> row4 = List.of(backButton);

        // Добавляем строки с кнопками в разметку
        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);
        rowsInline.add(row4);
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }

}

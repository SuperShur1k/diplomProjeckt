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

    public InlineKeyboardMarkup getAdminInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка настройки ролей
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton setAdminButton = new InlineKeyboardButton();
        setAdminButton.setText("ru".equals(languageCode) ? "Настройка ролей" :
                "uk".equals(languageCode) ? "Налаштування ролей" :
                        "Roles Settings");
        setAdminButton.setCallbackData("/role");
        row1.add(setAdminButton);

        // Кнопка настройки дат
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton dateButton = new InlineKeyboardButton();
        dateButton.setText("ru".equals(languageCode) ? "Настройка дат" :
                "uk".equals(languageCode) ? "Налаштування дат" :
                        "Date Settings");
        dateButton.setCallbackData("/date");
        row2.add(dateButton);

        // Кнопка настройки услуг
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton serviceButton = new InlineKeyboardButton();
        serviceButton.setText("ru".equals(languageCode) ? "Настройка услуг" :
                "uk".equals(languageCode) ? "Налаштування послуг" :
                        "Service Settings");
        serviceButton.setCallbackData("/service");
        row3.add(serviceButton);

        // Кнопка "Другие действия"
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton otherActionsButton = new InlineKeyboardButton();
        otherActionsButton.setText("ru".equals(languageCode) ? "Другие действия" :
                "uk".equals(languageCode) ? "Інші дії" :
                        "Other Actions");
        otherActionsButton.setCallbackData("/other_actions");
        row4.add(otherActionsButton);

        // Кнопка "Назад"
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" :
                "uk".equals(languageCode) ? "Назад" :
                        "Back");
        backButton.setCallbackData("/back");
        row5.add(backButton);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);
        rowsInline.add(row4);
        rowsInline.add(row5);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getOtherActionsInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Ответить на помощь"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton helpReplyButton = new InlineKeyboardButton();
        helpReplyButton.setText("ru".equals(languageCode) ? "Ответить на помощь" :
                "uk".equals(languageCode) ? "Відповісти на допомогу" :
                        "Reply to Help");
        helpReplyButton.setCallbackData("/reply_to_help");
        row1.add(helpReplyButton);

        // Кнопка "Написать клиенту"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton writeClientButton = new InlineKeyboardButton();
        writeClientButton.setText("ru".equals(languageCode) ? "Написать клиенту" :
                "uk".equals(languageCode) ? "Написати клієнту" :
                        "Write to Client");
        writeClientButton.setCallbackData("/write_to_client");
        row2.add(writeClientButton);

        // Кнопка "Написать мастеру"
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton writeMasterButton = new InlineKeyboardButton();
        writeMasterButton.setText("ru".equals(languageCode) ? "Написать мастеру" :
                "uk".equals(languageCode) ? "Написати майстру" :
                        "Write to Master");
        writeMasterButton.setCallbackData("/write_to_master");
        row3.add(writeMasterButton);

        // Кнопка "Управление записями"
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton manageAppointmentsButton = new InlineKeyboardButton();
        manageAppointmentsButton.setText("ru".equals(languageCode) ? "Управление записями" :
                "uk".equals(languageCode) ? "Управління записами" :
                        "Manage Appointments");
        manageAppointmentsButton.setCallbackData("/appointments_manage");
        row4.add(manageAppointmentsButton);

        // Кнопка "Назад" для возврата в предыдущее меню
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" :
                "uk".equals(languageCode) ? "Назад" :
                        "Back");
        backButton.setCallbackData("/admin");
        row5.add(backButton);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);
        rowsInline.add(row4);
        rowsInline.add(row5);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }


    public void getDateInlineKeyboard(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton addDateButton = new InlineKeyboardButton();
        addDateButton.setText("ru".equals(languageCode) ? "Добавить дату" : "uk".equals(languageCode) ? "Додати дату" : "Add Date");
        addDateButton.setCallbackData("/add_date");
        row1.add(addDateButton);

        InlineKeyboardButton addTimeButton = new InlineKeyboardButton();
        addTimeButton.setText("ru".equals(languageCode) ? "Добавить время" : "uk".equals(languageCode) ? "Додати час" : "Add Time");
        addTimeButton.setCallbackData("/add_time");
        row1.add(addTimeButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton delDateButton = new InlineKeyboardButton();
        delDateButton.setText("ru".equals(languageCode) ? "Удалить дату" : "uk".equals(languageCode) ? "Видалити дату" : "Delete Date");
        delDateButton.setCallbackData("/del_date");
        row2.add(delDateButton);

        InlineKeyboardButton delTimeButton = new InlineKeyboardButton();
        delTimeButton.setText("ru".equals(languageCode) ? "Удалить время" : "uk".equals(languageCode) ? "Видалити час" : "Delete Time");
        delTimeButton.setCallbackData("/del_time");
        row2.add(delTimeButton);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        row4.add(backButton);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row4);
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        String message = "ru".equals(languageCode)
                ? "Здесь вы можете добавить дату и время записи на процедуру."
                : "uk".equals(languageCode)
                ? "Тут ви можете додати дату і час запису на процедуру."
                : "Here you can add the date and time of the appointment for the procedure.";
        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    public void getServiceInlineKeyboard(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Добавить услугу"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton setAddServiceButton = new InlineKeyboardButton();
        setAddServiceButton.setText("ru".equals(languageCode)
                ? "Добавить услугу"
                : "uk".equals(languageCode)
                ? "Додати послугу"
                : "Add Service");
        setAddServiceButton.setCallbackData("/add_service");
        row1.add(setAddServiceButton);

        // Кнопка "Удалить услугу"
        InlineKeyboardButton setDelServiceButton = new InlineKeyboardButton();
        setDelServiceButton.setText("ru".equals(languageCode)
                ? "Удалить услугу"
                : "uk".equals(languageCode)
                ? "Видалити послугу"
                : "Delete Service");
        setDelServiceButton.setCallbackData("/del_service");
        row1.add(setDelServiceButton);

        // Кнопка "Изменить цену"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton changePriceButton = new InlineKeyboardButton();
        changePriceButton.setText("ru".equals(languageCode)
                ? "Изменить цену"
                : "uk".equals(languageCode)
                ? "Змінити ціну"
                : "Change Price");
        changePriceButton.setCallbackData("/change_price");
        row2.add(changePriceButton);

        // Кнопка "Назад"
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode)
                ? "Назад"
                : "uk".equals(languageCode)
                ? "Назад"
                : "Back");
        backButton.setCallbackData("/back");
        row4.add(backButton);

        // Добавляем кнопки в клавиатуру
        rowsInline.add(row1);
        rowsInline.add(row2); // Добавляем строку с кнопкой "Изменить цену"
        rowsInline.add(row4);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        String message = "ru".equals(languageCode)
                ? "Здесь вы можете управлять услугами."
                : "uk".equals(languageCode)
                ? "Тут ви можете керувати послугами."
                : "Here you can manage services.";
        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    public void getRoleInlineKeyboard(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton setAdminButton = new InlineKeyboardButton();
        setAdminButton.setText("ru".equals(languageCode) ? "Назначить администратора" : "uk".equals(languageCode) ? "Призначити адміністратора" : "Set Admin");
        setAdminButton.setCallbackData("/setadmin");
        row1.add(setAdminButton);

        InlineKeyboardButton setMasterButton = new InlineKeyboardButton();
        setMasterButton.setText("ru".equals(languageCode) ? "Назначить мастера" : "uk".equals(languageCode) ? "Призначити майстра" : "Set Master");
        setMasterButton.setCallbackData("/add_master");
        row1.add(setMasterButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton removeAdminButton = new InlineKeyboardButton();
        removeAdminButton.setText(
                "ru".equals(languageCode) ? "Удалить администратора" :
                        "uk".equals(languageCode) ? "Видалити адміністратора" :
                                "Remove Admin"
        );
        removeAdminButton.setCallbackData("/del_admin");
        row2.add(removeAdminButton);

        InlineKeyboardButton masterSettingButton = new InlineKeyboardButton();
        String manageMastersText = "ru".equals(languageCode)
                ? "Управление мастерами"
                : "uk".equals(languageCode)
                ? "Управління майстрами"
                : "Manage Masters";
        masterSettingButton.setText(manageMastersText);
        masterSettingButton.setCallbackData("/manage_masters");
        row2.add(masterSettingButton);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        row4.add(backButton);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row4);
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        String message = "ru".equals(languageCode)
                ? "Здесь вы можете управлять ролями."
                : "uk".equals(languageCode)
                ? "Тут ви можете керувати ролями."
                : "Here you can manage roles.";
        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    public InlineKeyboardMarkup getManageAppointmentsKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Перенести запись"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton rescheduleAppointmentButton = new InlineKeyboardButton();
        rescheduleAppointmentButton.setText("ru".equals(languageCode) ? "Перенести запись" :
                "uk".equals(languageCode) ? "Перенести запис" :
                        "Reschedule Appointment");
        rescheduleAppointmentButton.setCallbackData("/reschedule_appointment");
        row1.add(rescheduleAppointmentButton);

        // Кнопка "Отменить запись"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton cancelAppointmentButton = new InlineKeyboardButton();
        cancelAppointmentButton.setText("ru".equals(languageCode) ? "Отменить запись" :
                "uk".equals(languageCode) ? "Скасувати запис" :
                        "Cancel Appointment");
        cancelAppointmentButton.setCallbackData("/cancel_appointment");
        row2.add(cancelAppointmentButton);

        // Кнопка "Записать клиента"
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton addClientAppointmentButton = new InlineKeyboardButton();
        addClientAppointmentButton.setText("ru".equals(languageCode) ? "Записать клиента" :
                "uk".equals(languageCode) ? "Записати клієнта" :
                        "Add Client Appointment");
        addClientAppointmentButton.setCallbackData("/add_appointment");
        row3.add(addClientAppointmentButton);

        // Кнопка "Назад" для возврата в предыдущее меню
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" :
                "uk".equals(languageCode) ? "Назад" :
                        "Back");
        backButton.setCallbackData("/back");
        row4.add(backButton);

        // Добавляем все кнопки в общий список
        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);
        rowsInline.add(row4);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }

}
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

    // Метод для создания общего меню
    public InlineKeyboardMarkup getMenuInlineKeyboard(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка для бронирования
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton bookingButton = new InlineKeyboardButton();
        bookingButton.setText(
                "ru".equals(languageCode) ? "Менеджер бронирования" : "uk".equals(languageCode) ? "Менеджер бронювання" : "Booking Manager"
        );
        bookingButton.setCallbackData("/book_service");
        row1.add(bookingButton);

        // Кнопка настроек
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText(
                "ru".equals(languageCode) ? "Настройки" : "uk".equals(languageCode) ? "Налаштування" : "Settings"
        );
        settingsButton.setCallbackData("/settings");
        row1.add(settingsButton);

        rowsInline.add(row1);

        // Кнопки "Back" и "Logout"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(
                "ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back"
        );
        backButton.setCallbackData("/back");
        row2.add(backButton);

        rowsInline.add(row2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    // Метод для обработки команды /settings
    public void handleSettingsCommand(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка для смены имени и фамилии
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton changeNameButton = new InlineKeyboardButton();
        changeNameButton.setText("ru".equals(languageCode) ? "Сменить имя и фамилию" :
                "uk".equals(languageCode) ? "Змінити ім'я та прізвище" :
                        "Change Name and Last Name");
        changeNameButton.setCallbackData("/change_name");
        row1.add(changeNameButton);
        rowsInline.add(row1);

        // Кнопка для смены языка
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton changeLanguageButton = new InlineKeyboardButton();
        changeLanguageButton.setText("ru".equals(languageCode) ? "Сменить язык" :
                "uk".equals(languageCode) ? "Змінити мову" :
                        "Change Language");
        changeLanguageButton.setCallbackData("/change_language");
        row2.add(changeLanguageButton);
        rowsInline.add(row2);

        // Кнопка для смены номера телефона
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton changePhoneButton = new InlineKeyboardButton();
        changePhoneButton.setText("ru".equals(languageCode) ? "Сменить номер телефона" :
                "uk".equals(languageCode) ? "Змінити номер телефону" :
                        "Change Phone Number");
        changePhoneButton.setCallbackData("/change_phone");
        row3.add(changePhoneButton);
        rowsInline.add(row3);

        // Кнопка "Назад"
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" :
                "uk".equals(languageCode) ? "Назад" :
                        "Back");
        backButton.setCallbackData("/back");
        row4.add(backButton);
        rowsInline.add(row4);

        // Установка клавиатуры
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Отправка сообщения с клавиатурой
        String settingsMessage = "ru".equals(languageCode)
                ? "Здесь вы можете изменить ваши настройки."
                : "uk".equals(languageCode)
                ? "Тут ви можете змінити ваші налаштування."
                : "Here you can change your settings.";
        messageService.sendMessageWithInlineKeyboard(chatId, settingsMessage, inlineKeyboardMarkup);
    }

    // Метод для отображения менеджера бронирования
    public void bookingManagerButton(Long chatId, MessageService messageService) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton bookingButton = new InlineKeyboardButton();
        bookingButton.setText(
                "ru".equals(languageCode) ? "Забронировать услугу" : "uk".equals(languageCode) ? "Забронювати послугу" : "Book a Service"
        );
        bookingButton.setCallbackData("/book");
        row1.add(bookingButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton infoButton = new InlineKeyboardButton();
        infoButton.setText(
                "ru".equals(languageCode) ? "Информация о бронировании" : "uk".equals(languageCode) ? "Інформація про бронювання" : "Booking Information"
        );
        infoButton.setCallbackData("/book_info");
        row2.add(infoButton);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(
                "ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back"
        );
        backButton.setCallbackData("/back");
        row3.add(backButton);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        String bookingMessage = "ru".equals(languageCode)
                ? "Здесь вы можете управлять своим бронированием."
                : "uk".equals(languageCode)
                ? "Тут ви можете керувати своїм бронюванням."
                : "Here you can manage your booking.";
        messageService.sendMessageWithInlineKeyboard(chatId, bookingMessage, inlineKeyboardMarkup);
    }
}

package com.example.telegramBotNailsBooking.service;


import com.example.telegramBotNailsBooking.model.Help;
import com.example.telegramBotNailsBooking.model.Master;
import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.model.UserSession;
import com.example.telegramBotNailsBooking.repository.HelpRepository;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;

import com.example.telegramBotNailsBooking.service.buttons.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AutUserButtons autUserButtons;

    @Autowired
    private HelpService helpService;

    @Autowired
    private HelpRepository helpRepository;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private MenuService menuService;

    public boolean isAdmin(Long chatId) {
        Users users = userRepository.findByChatId(chatId);
        return users != null && users.getRole() == Users.Role.ADMIN;
    }

    protected void startCommand(Long chatId, Update update) {
        User user = update.getMessage().getFrom();
        String languageCode = user.getLanguageCode();

        if (userRepository.findByChatId(chatId) != null) {
            userSession.clearStates(chatId);
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, "/main_menu");

            String mainMenuMessage = "ru".equals(languageCode)
                    ? "Главное меню"
                    : "uk".equals(languageCode)
                    ? "Головне меню"
                    : "Main Menu";
            messageService.sendMessageWithInlineKeyboard(chatId, mainMenuMessage, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/start");

        if (update.getMessage() == null || update.getMessage().getFrom() == null) {
            // Обработка случая, когда update.getMessage() или getFrom() равно null
            String errorMessage = "ru".equals(languageCode)
                    ? "Ошибка: Не удалось обработать этот запрос."
                    : "uk".equals(languageCode)
                    ? "Помилка: Не вдалося обробити цей запит."
                    : "Error: Unable to process this request.";
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        if ("en".equals(languageCode)) {
            String message = "Hello! Welcome to our appointment booking bot. " +
                    "Here, you can easily schedule an appointment for your desired service. " +
                    "Simply follow the prompts, and we’ll guide you through the booking process. Let's get started!";
            messageService.sendMessage(chatId, message);
            personalData(chatId, "en");
        } else if ("ru".equals(languageCode)) {
            String message = "Здравствуйте! Добро пожаловать в наш бот для записи на процедуры." +
                    " Здесь вы можете легко записаться на нужную услугу." +
                    " Следуйте подсказкам, и мы поможем вам пройти процесс записи. Начнем!";
            messageService.sendMessage(chatId, message);
            personalData(chatId, "ru");
        } else if ("uk".equals(languageCode)) {
            String message = "Привіт! Вітаємо у нашому боті для запису на процедури." +
                    " Тут ви зможете легко записатися на бажану послугу." +
                    " Просто дотримуйтесь підказок, і ми допоможемо вам пройти процес запису. Почнемо!";
            messageService.sendMessage(chatId, message);
            personalData(chatId, "uk");
        } else {
            messageService.sendMessage(chatId, "Please choose a language.");
            chooseLanguage(chatId);
        }
    }

    private void chooseLanguage(Long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton ruButton = new InlineKeyboardButton();
        ruButton.setText("Русский");
        ruButton.setCallbackData("/lang_ru");

        InlineKeyboardButton enButton = new InlineKeyboardButton();
        enButton.setText("English");
        enButton.setCallbackData("/lang_en");

        InlineKeyboardButton ukButton = new InlineKeyboardButton();
        ukButton.setText("Українська");
        ukButton.setCallbackData("/lang_uk");

        rows.add(Arrays.asList(ruButton, enButton, ukButton));
        inlineKeyboardMarkup.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, "Please select your language:", inlineKeyboardMarkup);
    }

    protected void personalData(Long chatId, String languageCode) {
        String message;
        if ("ru".equals(languageCode)) {
            message = "Для продолжения работы с ботом просим вас дать согласие на обработку персональных данных. " +
                    "Эта информация будет использоваться исключительно для управления вашей записью. " +
                    "Вы согласны на обработку персональных данных?";
        } else if ("uk".equals(languageCode)) {
            message = "Для продовження роботи з ботом просимо надати вашу згоду на обробку персональних даних." +
                    " Ця інформація буде використовуватись виключно для управління вашим записом." +
                    " Чи погоджуєтесь ви на обробку персональних даних?";
        } else {
            message = "To continue using the bot, we kindly ask for your consent to process your personal data. " +
                    "This information will only be used to manage your appointment. " +
                    "Do you consent to the processing of your personal data?";
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton consentButton = new InlineKeyboardButton();
        consentButton.setText(
                "ru".equals(languageCode) ? "Да" : "uk".equals(languageCode) ? "Так" : "Yes"
        );
        consentButton.setCallbackData("/consent_yes_" + languageCode);

        InlineKeyboardButton declineButton = new InlineKeyboardButton();
        declineButton.setText(
                "ru".equals(languageCode) ? "Нет" : "uk".equals(languageCode) ? "Ні" : "No"
        );
        declineButton.setCallbackData("/consent_no_" + languageCode);

        rows.add(Arrays.asList(consentButton, declineButton));
        inlineKeyboardMarkup.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    protected void register(Long chatId, String languageCode, Update update) {
        String firstNamePrompt = "ru".equals(languageCode) ? "Пожалуйста, введите ваше имя:" :
                "uk".equals(languageCode) ? "Будь ласка, введіть ваше ім'я:" :
                        "Please enter your first name:";
        messageService.sendMessage(chatId, firstNamePrompt);

        // Устанавливаем начальное состояние для ввода имени
        userSession.setCurrentState(chatId, "/awaiting_first_name_" + languageCode);
    }

    protected void processFirstName(Long chatId, String languageCode, Update update) {
        // Проверяем, содержит ли update текстовое сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            String firstName = update.getMessage().getText();
            userSession.setUserInfo(chatId, new String[]{firstName, null});

            String lastNamePrompt = "ru".equals(languageCode) ? "Теперь введите вашу фамилию:" :
                    "uk".equals(languageCode) ? "Тепер введіть ваше прізвище:" :
                            "Now, please enter your last name:";
            messageService.sendMessage(chatId, lastNamePrompt);

            // Передаем управление в processLastName, устанавливая состояние
            userSession.setCurrentState(chatId, "/awaiting_last_name_" + languageCode);
        } else {
            // Отправляем ошибку, если нет текстового сообщения
            String errorMessage = "ru".equals(languageCode)
                    ? "Ошибка: введите текстовое сообщение."
                    : "uk".equals(languageCode)
                    ? "Помилка: введіть текстове повідомлення."
                    : "Error: Please enter a text message.";

            messageService.sendMessage(chatId, errorMessage);
        }
    }

    protected void processLastName(Long chatId, String languageCode, Update update) {
        // Проверяем, содержит ли update текстовое сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            String lastName = update.getMessage().getText();
            String[] userInfo = userSession.getUserInfo(chatId);
            userInfo[1] = lastName;

            requestPhoneNumber(chatId, languageCode);
        } else {
            String errorMessage = "ru".equals(languageCode)
                    ? "Ошибка: введите текстовое сообщение."
                    : "uk".equals(languageCode)
                    ? "Помилка: введіть текстове повідомлення."
                    : "Error: Please enter a text message.";

            messageService.sendMessage(chatId, errorMessage);
        }
    }

    private void requestPhoneNumber(Long chatId, String languageCode) {
        String message;
        if ("ru".equals(languageCode)) {
            message = "Пожалуйста, введите свой номер телефона, чтобы продолжить.";
        } else if ("uk".equals(languageCode)) {
            message = "Будь ласка, введіть свій номер телефону, щоб продовжити.";
        } else {
            message = "Please enter your phone number to continue.";
        }

        // Отправляем сообщение с просьбой ввести номер телефона вручную
        messageService.sendMessage(chatId, message);

        // Обновляем состояние пользователя
        userSession.setCurrentState(chatId, "/awaiting_phone_" + languageCode);
    }

    protected Users createUser(Long chatId, String[] userInfo, String phoneNumber, String languageCode) {
        Users users = new Users();
        users.setLanguage(languageCode);
        users.setChatId(chatId);
        users.setFirstName(userInfo[0]);
        users.setLastName(userInfo[1]);
        users.setPhoneNumber(phoneNumber);
        users.setCreatedAt(LocalDateTime.now());
        users.setUpdatedAt(LocalDateTime.now());
        users.setRole(Users.Role.CLIENT);
        return users;
    }

    protected void sendSuccessMessage(Long chatId, String languageCode) {
        String message = "ru".equals(languageCode) ?
                "Спасибо за ваше согласие! Теперь вы можете продолжить работу с ботом и записаться на услугу." :
                "uk".equals(languageCode) ?
                        "Дякуємо за вашу згоду! Тепер ви можете продовжити роботу з ботом і записатися на послугу." :
                        "Thank you for your consent! You may now continue using the bot and proceed with booking your appointment.";
        messageService.sendMessageWithInlineKeyboard(chatId, message, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
    }

    protected boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^\\+\\d{10,15}$");
    }

    protected void initialHelp(long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Сообщение помощи
        String message = "ru".equals(languageCode)
                ? "Добро пожаловать в меню помощи! Вы можете:\n" +
                "1. Написать админу для решения вопросов.\n" +
                "2. Написать мастеру для уточнения записи.\n" +
                "3. Ознакомиться со списком доступных команд."
                : "uk".equals(languageCode)
                ? "Ласкаво просимо до меню допомоги! Ви можете:\n" +
                "1. Написати адміністратору для вирішення питань.\n" +
                "2. Написати майстру для уточнення запису.\n" +
                "3. Ознайомитися зі списком доступних команд."
                : "Welcome to the help menu! You can:\n" +
                "1. Contact the admin for assistance.\n" +
                "2. Message the master for booking clarification.\n" +
                "3. View the list of available commands.";

        // Создаем клавиатуру с кнопками
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Написать админу"
        InlineKeyboardButton adminButton = new InlineKeyboardButton();
        adminButton.setText("ru".equals(languageCode) ? "Написать админу" : "uk".equals(languageCode) ? "Написати адміністратору" : "Contact Admin");
        adminButton.setCallbackData("/contact_admin");

        // Кнопка "Написать мастеру"
        InlineKeyboardButton masterButton = new InlineKeyboardButton();
        masterButton.setText("ru".equals(languageCode) ? "Написать мастеру" : "uk".equals(languageCode) ? "Написати майстру" : "Message Master");
        masterButton.setCallbackData("/contact_master");

        // Кнопка "Список команд"
        InlineKeyboardButton commandsButton = new InlineKeyboardButton();
        commandsButton.setText("ru".equals(languageCode) ? "Список команд" : "uk".equals(languageCode) ? "Список команд" : "List of Commands");
        commandsButton.setCallbackData("/list_commands");

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");

        // Добавляем кнопки в строки
        rows.add(List.of(adminButton));
        rows.add(List.of(masterButton));
        rows.add(List.of(commandsButton));
        rows.add(List.of(backButton)); // Добавляем кнопку "Назад"

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с кнопками
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void listCommands(long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Текст для заголовка
        String header = "ru".equals(languageCode)
                ? "Добро пожаловать в меню помощи! Вот доступные команды:\n"
                : "uk".equals(languageCode)
                ? "Ласкаво просимо до меню допомоги! Ось доступні команди:\n"
                : "Welcome to the help menu! Here are the available commands:\n";

        // Список команд
        Map<String, String> commands = Map.of(
                "/services", "ru".equals(languageCode) ? "Список услуг" : "uk".equals(languageCode) ? "Список послуг" : "List of services",
                "/review", "ru".equals(languageCode) ? "Оставить отзыв" : "uk".equals(languageCode) ? "Залишити відгук" : "Leave a review",
                "/start", "ru".equals(languageCode) ? "Запустить бота" : "uk".equals(languageCode) ? "Запустити бота" : "Start the bot",
                "/help", "ru".equals(languageCode) ? "Помощь" : "uk".equals(languageCode) ? "Допомога" : "Help",
                "/main_menu", "ru".equals(languageCode) ? "Главное меню" : "uk".equals(languageCode) ? "Головне меню" : "Main menu",
                "/back", "ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back",
                "/menu", "ru".equals(languageCode) ? "Меню" : "uk".equals(languageCode) ? "Меню" : "Menu",
                "/cancel", "ru".equals(languageCode) ? "Отмена операции" : "uk".equals(languageCode) ? "Скасування операції" : "Cancel operation",
                "/settings", "ru".equals(languageCode) ? "Настройки" : "uk".equals(languageCode) ? "Налаштування" : "Settings",
                "/book", "ru".equals(languageCode) ? "Записаться на услугу" : "uk".equals(languageCode) ? "Записатися на послугу" : "Book a service"
        );

        // Формирование текста сообщения
        StringBuilder message = new StringBuilder(header);
        commands.forEach((command, description) -> message.append(command).append(" - ").append(description).append("\n"));

        // Создаем кнопку "Назад"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");

        keyboard.setKeyboard(List.of(List.of(backButton)));

        // Отправляем сообщение с кнопкой
        messageService.sendMessageWithInlineKeyboard(chatId, message.toString(), keyboard);
    }

    public void contactAdmin(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Выберите действие:"
                : "uk".equals(languageCode)
                ? "Оберіть дію:"
                : "Choose an action:";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Задать новый вопрос"
        InlineKeyboardButton newQuestionButton = new InlineKeyboardButton();
        newQuestionButton.setText("ru".equals(languageCode) ? "Задать новый вопрос" : "uk".equals(languageCode) ? "Задати нове запитання" : "Ask a new question");
        newQuestionButton.setCallbackData("/ask_new_question");

        // Кнопка "Посмотреть старые запросы"
        InlineKeyboardButton viewRequestsButton = new InlineKeyboardButton();
        viewRequestsButton.setText("ru".equals(languageCode) ? "Посмотреть запросы" : "uk".equals(languageCode) ? "Переглянути запити" : "View requests");
        viewRequestsButton.setCallbackData("/view_requests");

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/help");

        rows.add(List.of(newQuestionButton));
        rows.add(List.of(viewRequestsButton));
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void askNewQuestion(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Напишите ваш вопрос:"
                : "uk".equals(languageCode)
                ? "Напишіть ваше запитання:"
                : "Write your question:";

        // Устанавливаем текущую команду
        userSession.setCurrentState(chatId, "/new_question");

        messageService.sendMessage(chatId, message);
    }

    public void handleNewQuestion(Long chatId, String questionText) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Users users = userRepository.findByChatId(chatId);

        // Сохраняем запрос в базе данных
        helpService.createHelpRequest(users.getId(), questionText);

        String confirmationMessage = "ru".equals(languageCode)
                ? "Ваш вопрос успешно отправлен админу."
                : "uk".equals(languageCode)
                ? "Ваше запитання успішно надіслано адміністратору."
                : "Your question has been successfully sent to the admin.";

        messageService.sendMessage(chatId, confirmationMessage);

        // Возвращаем в меню "Написать админу"
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/contact_admin");
        userSession.setPreviousState(chatId, "/help");
        contactAdmin(chatId);
    }

    public void viewRequests(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Выберите категорию запросов:"
                : "uk".equals(languageCode)
                ? "Оберіть категорію запитів:"
                : "Choose a request category:";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Открытые"
        InlineKeyboardButton openRequestsButton = new InlineKeyboardButton();
        openRequestsButton.setText("ru".equals(languageCode) ? "Открытые" : "uk".equals(languageCode) ? "Відкриті" : "Open");
        openRequestsButton.setCallbackData("/view_open_requests");

        // Кнопка "В процессе"
        InlineKeyboardButton inProgressRequestsButton = new InlineKeyboardButton();
        inProgressRequestsButton.setText("ru".equals(languageCode) ? "В процессе" : "uk".equals(languageCode) ? "У процесі" : "In Progress");
        inProgressRequestsButton.setCallbackData("/view_in_progress_requests");

        // Кнопка "Завершенные"
        InlineKeyboardButton closedRequestsButton = new InlineKeyboardButton();
        closedRequestsButton.setText("ru".equals(languageCode) ? "Завершенные" : "uk".equals(languageCode) ? "Завершені" : "Closed");
        closedRequestsButton.setCallbackData("/view_closed_requests");

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/contact_admin");

        rows.add(List.of(openRequestsButton));
        rows.add(List.of(inProgressRequestsButton));
        rows.add(List.of(closedRequestsButton));
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleOpenRequests(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        // Передаем объект enum вместо строки
        List<Help> openRequests = helpRepository.findByUser_IdAndStatus(userId, Help.HelpStatus.WAIT);

        if (openRequests.isEmpty()) {
            String noRequestsMessage = "ru".equals(languageCode)
                    ? "У вас нет открытых запросов."
                    : "uk".equals(languageCode)
                    ? "У вас немає відкритих запитів."
                    : "You have no open requests.";
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        String message = "ru".equals(languageCode)
                ? "Выберите дату открытого запроса:"
                : "uk".equals(languageCode)
                ? "Оберіть дату відкритого запиту:"
                : "Select the date of the open request:";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для каждого запроса
        for (Help help : openRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString()); // Только дата
            button.setCallbackData("/view_request_" + help.getId());
            rows.add(List.of(button));
        }

        // Добавляем кнопки назад
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleViewRequest(Long chatId, Long helpId) {
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Ваш запрос:\n" + help.getHelpQuestion()
                : "uk".equals(languageCode)
                ? "Ваш запит:\n" + help.getHelpQuestion()
                : "Your request:\n" + help.getHelpQuestion();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleInProgressRequests(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        List<Help> inProgressRequests = helpRepository.findByUser_IdAndStatus(userId, Help.HelpStatus.OPEN);

        if (inProgressRequests.isEmpty()) {
            String noRequestsMessage = "ru".equals(languageCode)
                    ? "У вас нет запросов в процессе."
                    : "uk".equals(languageCode)
                    ? "У вас немає запитів у процесі."
                    : "You have no requests in progress.";
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        String message = "ru".equals(languageCode)
                ? "Выберите дату запроса в процессе:"
                : "uk".equals(languageCode)
                ? "Оберіть дату запиту у процесі:"
                : "Select the date of the request in progress:";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Help help : inProgressRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString());
            button.setCallbackData("/progress_request_" + help.getId());
            rows.add(List.of(button));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleViewInProgressRequest(Long chatId, Long helpId) {
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Ваш запрос:\n" + help.getHelpQuestion() + "\nОтвет администратора:\n" + help.getAdminResponse()
                : "uk".equals(languageCode)
                ? "Ваш запит:\n" + help.getHelpQuestion() + "\nВідповідь адміністратора:\n" + help.getAdminResponse()
                : "Your request:\n" + help.getHelpQuestion() + "\nAdmin's response:\n" + help.getAdminResponse();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("ru".equals(languageCode) ? "Да" : "uk".equals(languageCode) ? "Так" : "Yes");
        yesButton.setCallbackData("/close_request_" + helpId);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("ru".equals(languageCode) ? "Нет" : "uk".equals(languageCode) ? "Ні" : "No");
        noButton.setCallbackData("/new_question_" + helpId);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/view_requests");

        rows.add(List.of(yesButton, noButton));
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleCloseRequest(Long chatId, Long helpId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        help.setStatus(Help.HelpStatus.CLOSED);
        help.setClosedAt(LocalDateTime.now());
        helpRepository.save(help);

        String message = "ru".equals(languageCode)
                ? "Ваш запрос был закрыт."
                : "uk".equals(languageCode)
                ? "Ваш запит було закрито."
                : "Your request has been closed.";

        messageService.sendMessage(chatId, message);

        viewRequests(chatId);
    }

    public void handleAskNewQuestion(Long chatId, Long helpId) {
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        help.setStatus(Help.HelpStatus.CLOSED);
        helpRepository.save(help);

        askNewQuestion(chatId);
    }

    public void handleClosedRequests(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        List<Help> closedRequests = helpRepository.findByUser_IdAndStatus(userId, Help.HelpStatus.CLOSED);

        if (closedRequests.isEmpty()) {
            String noRequestsMessage = "ru".equals(languageCode)
                    ? "У вас нет завершенных запросов."
                    : "uk".equals(languageCode)
                    ? "У вас немає завершених запитів."
                    : "You have no closed requests.";
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        String message = "ru".equals(languageCode)
                ? "Выберите дату завершенного запроса:"
                : "uk".equals(languageCode)
                ? "Оберіть дату завершеного запиту:"
                : "Select the date of the closed request:";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Help help : closedRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString());
            button.setCallbackData("/closed_request_" + help.getId());
            rows.add(List.of(button));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleViewClosedRequest(Long chatId, Long helpId) {
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Ваш запрос:\n" + help.getHelpQuestion() + "\nОтвет администратора:\n" + help.getAdminResponse()
                : "uk".equals(languageCode)
                ? "Ваш запит:\n" + help.getHelpQuestion() + "\nВідповідь адміністратора:\n" + help.getAdminResponse()
                : "Your request:\n" + help.getHelpQuestion() + "\nAdmin's response:\n" + help.getAdminResponse();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton newRequestButton = new InlineKeyboardButton();
        newRequestButton.setText("ru".equals(languageCode) ? "Создать новый запрос" : "uk".equals(languageCode) ? "Створити новий запит" : "Create new request");
        newRequestButton.setCallbackData("/ask_new_question");

        InlineKeyboardButton deleteRequestButton = new InlineKeyboardButton();
        deleteRequestButton.setText("ru".equals(languageCode) ? "Удалить запрос" : "uk".equals(languageCode) ? "Видалити запит" : "Delete request");
        deleteRequestButton.setCallbackData("/delete_request_" + helpId);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/view_requests");

        rows.add(List.of(newRequestButton));
        rows.add(List.of(deleteRequestButton));
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void deleteRequests(Long chatId, Long helpId) {
        // Получаем язык пользователя
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем userId из chatId
        Long userId = userRepository.findByChatId(chatId).getId();

        if (userId == null) {
            // Отправляем сообщение, если пользователь не найден
            String userNotFoundMessage = "ru".equals(languageCode)
                    ? "Пользователь не найден."
                    : "uk".equals(languageCode)
                    ? "Користувач не знайдений."
                    : "User not found.";
            messageService.sendMessage(chatId, userNotFoundMessage);
            return;
        }

        // Проверяем, существует ли запрос с таким ID и принадлежит ли он пользователю
        Optional<Help> helpOptional = helpRepository.findById(helpId);

        if (helpOptional.isEmpty() || !helpOptional.get().getUser().getId().equals(userId)) {
            // Сообщаем, что запрос не найден
            String helpNotFoundMessage = "ru".equals(languageCode)
                    ? "Запрос не найден или не принадлежит вам."
                    : "uk".equals(languageCode)
                    ? "Запит не знайдений або не належить вам."
                    : "Request not found or does not belong to you.";
            messageService.sendMessage(chatId, helpNotFoundMessage);
            return;
        }

        // Удаляем запрос
        helpRepository.deleteById(helpId);

        // Подтверждаем удаление
        String deleteSuccessMessage = "ru".equals(languageCode)
                ? "Запрос успешно удален."
                : "uk".equals(languageCode)
                ? "Запит успішно видалений."
                : "Request successfully deleted.";
        messageService.sendMessage(chatId, deleteSuccessMessage);
        viewRequests(chatId);
    }

    protected void masterRequests(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список всех мастеров
        List<Master> masters = masterRepository.findAllByStatus(Master.Status.ACTIVE);

        if (masters.isEmpty()) {
            String noMastersMessage = "ru".equals(languageCode)
                    ? "В системе нет мастеров."
                    : "uk".equals(languageCode)
                    ? "У системі немає майстрів."
                    : "There are no masters in the system.";
            messageService.sendMessage(chatId, noMastersMessage);
            initialHelp(chatId);
            return;
        }

        // Создаем inline-кнопки с именами мастеров
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName());
            button.setCallbackData("/contact_master_" + master.getId());
            rows.add(List.of(button));
        }

        // Добавляем кнопку "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(
                "ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back"
        );
        cancelButton.setCallbackData("/help");
        rows.add(List.of(cancelButton));

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите мастера:"
                : "uk".equals(languageCode)
                ? "Виберіть майстра:"
                : "Select a master:";

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void masterContactRequests(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем информацию о мастере по его ID
        Optional<Master> masterOptional = masterRepository.findById(masterId);

        if (masterOptional.isEmpty()) {
            String noMasterFoundMessage = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
            messageService.sendMessage(chatId, noMasterFoundMessage);
            return;
        }

        Master master = masterOptional.get();

        // Формируем сообщение с информацией о мастере
        String message = "ru".equals(languageCode)
                ? "Информация о мастере:\n" +
                "Имя: " + master.getName() + "\n" +
                "Телефон: " + master.getPhoneNumber() + "\n" +
                "Описание: " + master.getDescription()
                : "uk".equals(languageCode)
                ? "Інформація про майстра:\n" +
                "Ім'я: " + master.getName() + "\n" +
                "Телефон: " + master.getPhoneNumber() + "\n" +
                "Опис: " + master.getDescription()
                : "Information about the master:\n" +
                "Name: " + master.getName() + "\n" +
                "Phone: " + master.getPhoneNumber() + "\n" +
                "Description: " + master.getDescription();

        // Создаем inline-клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для социальных сетей мастера (если есть ссылка)
        if (master.getSocialLink() != null && !master.getSocialLink().isEmpty()) {
            InlineKeyboardButton socialButton = new InlineKeyboardButton();
            socialButton.setText("ru".equals(languageCode) ? "Социальная сеть" :
                    "uk".equals(languageCode) ? "Соціальна мережа" : "Social Link");
            socialButton.setUrl(master.getSocialLink());
            rows.add(List.of(socialButton));
        }
        // Кнопка для возврата назад
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" :
                "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/contact_master");

        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        // Отправляем клавиатуру
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void initialChangeNameNLastName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Users user = userRepository.findByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Привет, " + user.getFirstName() + " " + user.getLastName() + "! Здесь вы можете изменить свое имя и фамилию."
                : "uk".equals(languageCode)
                ? "Привіт, " + user.getFirstName() + " " + user.getLastName() + "! Тут ви можете змінити своє ім'я та прізвище."
                : "Hello, " + user.getFirstName() + " " + user.getLastName() + "! Here you can change your name and last name.";

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка для изменения имени
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton changeFirstNameButton = new InlineKeyboardButton();
        changeFirstNameButton.setText("ru".equals(languageCode) ? "Сменить имя" :
                "uk".equals(languageCode) ? "Змінити ім'я" :
                        "Change First Name");
        changeFirstNameButton.setCallbackData("/change_first_name");
        row1.add(changeFirstNameButton);
        rowsInline.add(row1);

        // Кнопка для изменения фамилии
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton changeLastNameButton = new InlineKeyboardButton();
        changeLastNameButton.setText("ru".equals(languageCode) ? "Сменить фамилию" :
                "uk".equals(languageCode) ? "Змінити прізвище" :
                        "Change Last Name");
        changeLastNameButton.setCallbackData("/change_last_name");
        row2.add(changeLastNameButton);
        rowsInline.add(row2);

        // Кнопка "Назад" для возврата в настройки
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" :
                "uk".equals(languageCode) ? "Назад" :
                        "Back");
        backButton.setCallbackData("/settings");
        row3.add(backButton);
        rowsInline.add(row3);

        // Установка клавиатуры
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Отправка сообщения с клавиатурой
        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    protected void changeFirstName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Отправляем запрос на ввод нового имени
        String message = "ru".equals(languageCode)
                ? "Введите новое имя:"
                : "uk".equals(languageCode)
                ? "Введіть нове ім'я:"
                : "Please enter the new first name:";

        messageService.sendMessage(chatId, message);

        // Устанавливаем состояние ожидания ввода нового имени
        userSession.setCurrentState(chatId, "/waiting_for_first_name");
    }

    public void handleNewFirstName(Long chatId, String newFirstName) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Проверка длины имени
        if (newFirstName == null || newFirstName.trim().length() < 2) {
            String errorMessage = "ru".equals(languageCode)
                    ? "Имя должно содержать не менее 2 символов. Попробуйте еще раз:"
                    : "uk".equals(languageCode)
                    ? "Ім'я має містити не менше 2 символів. Спробуйте ще раз:"
                    : "The first name must contain at least 2 characters. Please try again:";
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        // Сообщение с подтверждением
        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы хотите сменить имя на: " + newFirstName + "?"
                : "uk".equals(languageCode)
                ? "Ви хочете змінити ім'я на: " + newFirstName + "?"
                : "Do you want to change your name to: " + newFirstName + "?";

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Да"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("ru".equals(languageCode) ? "Да" :
                "uk".equals(languageCode) ? "Так" :
                        "Yes");
        yesButton.setCallbackData("/confirm_change_first_name_" + newFirstName);
        row1.add(yesButton);
        rowsInline.add(row1);

        // Кнопка "Нет"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("ru".equals(languageCode) ? "Нет" :
                "uk".equals(languageCode) ? "Ні" :
                        "No");
        noButton.setCallbackData("/cancel_change_first_name");
        row2.add(noButton);
        rowsInline.add(row2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        userSession.setCurrentState(chatId, "/settings");

        // Отправляем сообщение с подтверждением
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, inlineKeyboardMarkup);
    }

    protected void confirmChangeFirstName(Long chatId, String newFirstName) {
        // Обновляем имя в базе данных
        userRepository.updateFirstName(chatId, newFirstName);

        // Сообщение об успешном изменении
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String successMessage = "ru".equals(languageCode)
                ? "Имя успешно изменено на: " + newFirstName
                : "uk".equals(languageCode)
                ? "Ім'я успішно змінено на: " + newFirstName
                : "First name successfully changed to: " + newFirstName;

        messageService.sendMessage(chatId, successMessage);

        // Возвращаемся в начальное меню изменения имени и фамилии
        initialChangeNameNLastName(chatId);
    }

    protected void cancelChangeFirstName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Сообщение об отмене
        String cancelMessage = "ru".equals(languageCode)
                ? "Изменение имени отменено."
                : "uk".equals(languageCode)
                ? "Зміна імені скасована."
                : "First name change canceled.";

        messageService.sendMessage(chatId, cancelMessage);

        // Возвращаемся в начальное меню изменения имени и фамилии
        initialChangeNameNLastName(chatId);
    }

    protected void changeLastName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Request for the new last name
        String message = "ru".equals(languageCode)
                ? "Введите новую фамилию:"
                : "uk".equals(languageCode)
                ? "Введіть нове прізвище:"
                : "Please enter the new last name:";

        messageService.sendMessage(chatId, message);

        // Set the state for waiting for the new last name
        userSession.setCurrentState(chatId, "/waiting_for_last_name");
    }

    public void handleNewLastName(Long chatId, String newLastName) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Проверка длины фамилии
        if (newLastName == null || newLastName.trim().length() < 2) {
            String errorMessage = "ru".equals(languageCode)
                    ? "Фамилия должна содержать не менее 2 символов. Попробуйте еще раз:"
                    : "uk".equals(languageCode)
                    ? "Прізвище має містити не менше 2 символів. Спробуйте ще раз:"
                    : "The last name must contain at least 2 characters. Please try again:";
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        // Сообщение с подтверждением
        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы хотите сменить фамилию на: " + newLastName + "?"
                : "uk".equals(languageCode)
                ? "Ви хочете змінити прізвище на: " + newLastName + "?"
                : "Do you want to change your last name to: " + newLastName + "?";

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Да"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("ru".equals(languageCode) ? "Да" :
                "uk".equals(languageCode) ? "Так" :
                        "Yes");
        yesButton.setCallbackData("/confirm_change_last_name_" + newLastName);
        row1.add(yesButton);
        rowsInline.add(row1);

        // Кнопка "Нет"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("ru".equals(languageCode) ? "Нет" :
                "uk".equals(languageCode) ? "Ні" :
                        "No");
        noButton.setCallbackData("/cancel_change_last_name");
        row2.add(noButton);
        rowsInline.add(row2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Устанавливаем состояние и отправляем сообщение
        userSession.setCurrentState(chatId, "/settings");
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, inlineKeyboardMarkup);
    }

    protected void confirmChangeLastName(Long chatId, String newLastName) {
        // Update the last name in the database
        userRepository.updateLastName(chatId, newLastName);

        // Success message
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String successMessage = "ru".equals(languageCode)
                ? "Фамилия успешно изменена на: " + newLastName
                : "uk".equals(languageCode)
                ? "Прізвище успішно змінено на: " + newLastName
                : "Last name successfully changed to: " + newLastName;

        messageService.sendMessage(chatId, successMessage);

        // Return to the initial menu for changing names
        initialChangeNameNLastName(chatId);
    }

    protected void cancelChangeLastName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Cancellation message
        String cancelMessage = "ru".equals(languageCode)
                ? "Изменение фамилии отменено."
                : "uk".equals(languageCode)
                ? "Зміна прізвища скасована."
                : "Last name change canceled.";

        messageService.sendMessage(chatId, cancelMessage);

        // Return to the initial menu for changing names
        initialChangeNameNLastName(chatId);
    }

    protected void initialChangeLanguage(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        userSession.setCurrentState(chatId, "/choose_lang");

        // Создаем клавиатуру с кнопками для выбора языка
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для выбора русского языка
        InlineKeyboardButton ruButton = new InlineKeyboardButton();
        ruButton.setText("Русский");
        ruButton.setCallbackData("/lang_ru");

        // Кнопка для выбора английского языка
        InlineKeyboardButton enButton = new InlineKeyboardButton();
        enButton.setText("English");
        enButton.setCallbackData("/lang_en");

        // Кнопка для выбора украинского языка
        InlineKeyboardButton ukButton = new InlineKeyboardButton();
        ukButton.setText("Українська");
        ukButton.setCallbackData("/lang_uk");

        // Добавляем кнопки в строку
        rows.add(Arrays.asList(ruButton, enButton, ukButton));

        // Назад
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/settings");
        rows.add(Collections.singletonList(backButton));

        inlineKeyboardMarkup.setKeyboard(rows);

        // Сообщение о выборе языка
        String message = "ru".equals(languageCode)
                ? "Пожалуйста, выберите язык:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть мову:"
                : "Please select your language:";

        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    protected void changeLanguage(Long chatId, String newLanguageCode) {
        // Проверка валидности кода языка
        List<String> validLanguageCodes = Arrays.asList("ru", "en", "uk");
        if (!validLanguageCodes.contains(newLanguageCode)) {
            messageService.sendMessage(chatId, "Invalid language code.");
            return;
        }

        // Обновляем язык в базе данных
        userRepository.updateLanguageCodeByChatId(chatId, newLanguageCode);

        // Отправляем сообщение о том, что язык успешно изменен
        String successMessage = "ru".equals(newLanguageCode)
                ? "Язык успешно изменен на Русский."
                : "uk".equals(newLanguageCode)
                ? "Мову успішно змінено на Українську."
                : "Language successfully changed to English.";

        messageService.sendMessage(chatId, successMessage);

        // Возвращаем пользователя в главное меню настроек
        initialChangeLanguage(chatId);
    }

    protected void initialChangePhoneNumber(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Сообщение для запроса нового номера телефона
        String message = "ru".equals(languageCode)
                ? "Введите новый номер телефона в формате +[код страны][номер]:"
                : "uk".equals(languageCode)
                ? "Введіть новий номер телефону у форматі +[код країни][номер]:"
                : "Please enter the new phone number in the format +[country code][number]:";

        // Создаем клавиатуру с кнопкой "Назад"
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(
                "ru".equals(languageCode) ? "Назад" :
                        "uk".equals(languageCode) ? "Назад" :
                                "Back"
        );
        backButton.setCallbackData("/settings"); // Команда для возврата в настройки
        rows.add(List.of(backButton));

        inlineKeyboardMarkup.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);

        // Устанавливаем состояние ожидания ввода нового номера телефона
        userSession.setCurrentState(chatId, "/waiting_for_phone_number");
    }

    public void handleNewPhoneNumber(Long chatId, String newPhoneNumber) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Проверка формата номера телефона: должно начинаться с "+" и содержать от 10 до 15 цифр
        if (!newPhoneNumber.matches("^\\+\\d{10,15}$")) {
            String errorMessage = "ru".equals(languageCode)
                    ? "Неверный формат номера. Убедитесь, что номер начинается с '+' и содержит от 10 до 15 цифр."
                    : "uk".equals(languageCode)
                    ? "Невірний формат номера. Переконайтеся, що номер починається з '+' і містить від 10 до 15 цифр."
                    : "Invalid phone number format. Ensure the number starts with '+' and contains 10 to 15 digits.";

            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        // Сообщение для подтверждения изменения
        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы хотите сменить номер телефона на: " + newPhoneNumber + "?"
                : "uk".equals(languageCode)
                ? "Ви хочете змінити номер телефону на: " + newPhoneNumber + "?"
                : "Do you want to change your phone number to: " + newPhoneNumber + "?";

        // Создаем inline-кнопки для подтверждения или отмены
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Да"
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("ru".equals(languageCode) ? "Да" : "uk".equals(languageCode) ? "Так" : "Yes");
        yesButton.setCallbackData("/confirm_change_phone_number_" + newPhoneNumber);

        // Кнопка "Нет"
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("ru".equals(languageCode) ? "Нет" : "uk".equals(languageCode) ? "Ні" : "No");
        noButton.setCallbackData("/cancel_change_phone_number");

        rowsInline.add(List.of(yesButton, noButton));
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Отправляем сообщение с подтверждением
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, inlineKeyboardMarkup);
        userSession.setCurrentState(chatId, "/settings");
    }

    protected void confirmChangePhoneNumber(Long chatId, String newPhoneNumber) {
        // Обновляем номер телефона в базе данных
        userRepository.updatePhoneNumberByChatId(chatId, newPhoneNumber);

        // Сообщение об успешном изменении
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String successMessage = "ru".equals(languageCode)
                ? "Номер телефона успешно изменен на: " + newPhoneNumber
                : "uk".equals(languageCode)
                ? "Номер телефону успішно змінено на: " + newPhoneNumber
                : "Phone number successfully changed to: " + newPhoneNumber;

        messageService.sendMessage(chatId, successMessage);
        userSession.setCurrentState(chatId, "/settings");
        // Возвращаемся в главное меню настроек
        menuService.handleSettingsCommand(chatId, messageService);
    }

    protected void cancelChangePhoneNumber(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Сообщение об отмене изменения номера
        String cancelMessage = "ru".equals(languageCode)
                ? "Изменение номера телефона отменено."
                : "uk".equals(languageCode)
                ? "Зміна номера телефону скасована."
                : "Phone number change canceled.";

        messageService.sendMessage(chatId, cancelMessage);

        // Возвращаемся в главное меню настроек
        userSession.setCurrentState(chatId, "/settings");
        menuService.handleSettingsCommand(chatId, messageService);
    }

    public void initialWriteToAdmin(Long chatId, Long adminChatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем информацию об администраторе
        Users admin = userRepository.findByChatId(adminChatId);
        if (admin == null) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/main_menu");
            messageService.sendMessageWithInlineKeyboard(chatId,"Admin not found with ID: " + adminChatId, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
        }

        // Сообщение пользователю
        String message = "ru".equals(languageCode)
                ? "Напишите сообщение администратору: " + admin.getFirstName()
                : "uk".equals(languageCode)
                ? "Напишіть повідомлення адміністратору: " + admin.getFirstName()
                : "Write a message to the admin: " + admin.getFirstName();

        messageService.sendMessage(chatId, message);

        // Устанавливаем состояние для чата
        userSession.setCurrentState(chatId, "/writing_to_admin_from_user_" + adminChatId);
        userSession.setPreviousState(chatId, "/main_menu");
    }

    public void writeToAdmin(Long masterChatId, Long adminChatId, String messageText) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);
        String adminLanguageCode = userRepository.findLanguageCodeByChatId(adminChatId);

        Users users = userRepository.findByChatId(masterChatId);
        if (users == null) {
            userSession.clearStates(masterChatId);
            userSession.setCurrentState(masterChatId, "/main_menu");
            messageService.sendMessage(masterChatId, "User not found with chat ID: " + masterChatId);
        }

        // Формируем сообщение для администратора
        String messageToAdmin = "ru".equals(adminLanguageCode)
                ? "Вам написал пользователь:\n" + users.getFirstName() + "\n" + users.getLastName() + "\n\nСообщение:\n" + messageText
                : "uk".equals(adminLanguageCode)
                ? "Вам написав користувач:\n" + users.getFirstName() + "\n" + users.getLastName() + "\n\nПовідомлення:\n" + messageText
                : "A user wrote to you:\n" + users.getFirstName() + "\n" + users.getLastName() + "\n\nMessage:\n" + messageText;

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText("ru".equals(adminLanguageCode)
                ? "Ответить"
                : "uk".equals(adminLanguageCode)
                ? "Відповісти"
                : "Reply");
        replyButton.setCallbackData("/write_user_" + masterChatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Отправляем сообщение администратору
        messageService.sendMessageWithInlineKeyboard(adminChatId, messageToAdmin, keyboard);

        // Подтверждаем мастеру отправку сообщения
        String confirmationMessage = "ru".equals(languageCode)
                ? "Ваше сообщение отправлено администратору."
                : "uk".equals(languageCode)
                ? "Ваше повідомлення надіслано адміністратору."
                : "Your message has been sent to the admin.";

        // Очищаем состояние чата мастера
        userSession.clearStates(masterChatId);
        userSession.setCurrentState(masterChatId, "/main_menu");

        messageService.sendMessageWithInlineKeyboard(masterChatId, confirmationMessage, autUserButtons.getAuthenticatedInlineKeyboard(masterChatId));
    }

    public void initialWriteToMaster(Long chatId, Long masterChatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем информацию об администраторе
        Users master = userRepository.findByChatId(masterChatId);
        if (master == null) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/main_menu");
            messageService.sendMessageWithInlineKeyboard(chatId,"Master not found with ID: " + masterChatId, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
        }

        // Сообщение пользователю
        String message = "ru".equals(languageCode)
                ? "Напишите сообщение администратору: " + master.getFirstName()
                : "uk".equals(languageCode)
                ? "Напишіть повідомлення адміністратору: " + master.getFirstName()
                : "Write a message to the admin: " + master.getFirstName();

        messageService.sendMessage(chatId, message);

        // Устанавливаем состояние для чата
        userSession.setCurrentState(chatId, "/writing_to_master_from_user_" + masterChatId);
        userSession.setPreviousState(chatId, "/main_menu");
    }

    public void writeToMaster(Long chatId, Long masterChatId, String messageText) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);
        String masterLanguageCode = userRepository.findLanguageCodeByChatId(masterChatId);

        Users users = userRepository.findByChatId(chatId);
        if (users == null) {
            userSession.clearStates(masterChatId);
            userSession.setCurrentState(masterChatId, "/main_menu");
            messageService.sendMessage(masterChatId, "User not found with chat ID: " + masterChatId);
        }

        String messageToAdmin = "ru".equals(masterLanguageCode)
                ? "Вам написал пользователь:\n" + users.getFirstName() + "\n" + users.getLastName() + "\n\nСообщение:\n" + messageText
                : "uk".equals(masterLanguageCode)
                ? "Вам написав користувач:\n" + users.getFirstName() + "\n" + users.getLastName() + "\n\nПовідомлення:\n" + messageText
                : "A user wrote to you:\n" + users.getFirstName() + "\n" + users.getLastName() + "\n\nMessage:\n" + messageText;

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText("ru".equals(masterLanguageCode)
                ? "Ответить"
                : "uk".equals(masterLanguageCode)
                ? "Відповісти"
                : "Reply");
        replyButton.setCallbackData("/message_client_" + chatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        messageService.sendMessageWithInlineKeyboard(masterChatId, messageToAdmin, keyboard);

        // Подтверждаем мастеру отправку сообщения
        String confirmationMessage = "ru".equals(languageCode)
                ? "Ваше сообщение отправлено администратору."
                : "uk".equals(languageCode)
                ? "Ваше повідомлення надіслано адміністратору."
                : "Your message has been sent to the admin.";

        // Очищаем состояние чата мастера
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/main_menu");

        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
    }

}

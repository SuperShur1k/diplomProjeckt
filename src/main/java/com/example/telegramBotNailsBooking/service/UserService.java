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
        return users != null && users.getRole().equals(Users.Role.ADMIN);
    }

    public void startCommand(Long chatId, Update update) {
        User user = update.getMessage().getFrom();
        String languageCode = user.getLanguageCode();

        if (update == null) {
            userSession.clearStates(chatId);
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, "/main_menu");

            String mainMenuMessage = messageService.getLocalizedMessage("main_menu", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, mainMenuMessage, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        if (userRepository.findByChatId(chatId) != null) {
            userSession.clearStates(chatId);
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, "/main_menu");

            String mainMenuMessage = messageService.getLocalizedMessage("main_menu", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, mainMenuMessage, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/start");

        if (update.getMessage() == null || update.getMessage().getFrom() == null) {
            // Обработка случая, когда update.getMessage() или getFrom() равно null
            String errorMessage = messageService.getLocalizedMessage("error_message", languageCode);
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        String welcomeMessage;
        if ("en".equals(languageCode)) {
            welcomeMessage = messageService.getLocalizedMessage("welcome_message_en", languageCode);
            messageService.sendMessage(chatId, welcomeMessage);
            personalData(chatId, "en");
        } else if ("ru".equals(languageCode)) {
            welcomeMessage = messageService.getLocalizedMessage("welcome_message_ru", languageCode);
            messageService.sendMessage(chatId, welcomeMessage);
            personalData(chatId, "ru");
        } else if ("uk".equals(languageCode)) {
            welcomeMessage = messageService.getLocalizedMessage("welcome_message_uk", languageCode);
            messageService.sendMessage(chatId, welcomeMessage);
            personalData(chatId, "uk");
        } else {
            messageService.sendMessage(chatId, "Please choose a language.");
            chooseLanguage(chatId);
        }
    }

    public void chooseLanguage(Long chatId) {
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

    public void personalData(Long chatId, String languageCode) {
        // Получаем локализованное сообщение для запроса согласия
        String message = messageService.getLocalizedMessage("personal_data_consent", languageCode);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для согласия
        InlineKeyboardButton consentButton = new InlineKeyboardButton();
        consentButton.setText(messageService.getLocalizedMessage("consent_yes", languageCode));
        consentButton.setCallbackData("/consent_yes_" + languageCode);

        // Кнопка для отказа
        InlineKeyboardButton declineButton = new InlineKeyboardButton();
        declineButton.setText(messageService.getLocalizedMessage("consent_no", languageCode));
        declineButton.setCallbackData("/consent_no_" + languageCode);

        // Добавляем кнопки в клавиатуру
        rows.add(Arrays.asList(consentButton, declineButton));
        inlineKeyboardMarkup.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    protected void register(Long chatId, String languageCode, Update update) {
        // Локализуем запрос для ввода имени с использованием messageService
        String firstNamePrompt = messageService.getLocalizedMessage("register_first_name_prompt", languageCode);

        // Отправляем локализованное сообщение
        messageService.sendMessage(chatId, firstNamePrompt);

        // Устанавливаем начальное состояние для ввода имени
        userSession.setCurrentState(chatId, "/awaiting_first_name_" + languageCode);
    }

    protected void processFirstName(Long chatId, String languageCode, Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String firstName = update.getMessage().getText();

            // Validate the length of the first name
            if (firstName.length() < 2 || firstName.length() > 50) {
                String errorMessage = messageService.getLocalizedMessage("error_first_name_length", languageCode);
                messageService.sendMessage(chatId, errorMessage);
                return;
            }

            userSession.setUserInfo(chatId, new String[]{firstName, null});

            // Request last name
            String lastNamePrompt = messageService.getLocalizedMessage("register_last_name_prompt", languageCode);
            messageService.sendMessage(chatId, lastNamePrompt);

            userSession.setCurrentState(chatId, "/awaiting_last_name_" + languageCode);
        } else {
            String errorMessage = messageService.getLocalizedMessage("error_invalid_input", languageCode);
            messageService.sendMessage(chatId, errorMessage);
        }
    }

    protected void processLastName(Long chatId, String languageCode, Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String lastName = update.getMessage().getText();

            // Validate the length of the last name
            if (lastName.length() < 2 || lastName.length() > 50) {
                String errorMessage = messageService.getLocalizedMessage("error_last_name_length", languageCode);
                messageService.sendMessage(chatId, errorMessage);
                return;
            }

            String[] userInfo = userSession.getUserInfo(chatId);
            userInfo[1] = lastName;

            // Request phone number
            requestPhoneNumber(chatId, languageCode);
        } else {
            String errorMessage = messageService.getLocalizedMessage("error_invalid_input", languageCode);
            messageService.sendMessage(chatId, errorMessage);
        }
    }

    private void requestPhoneNumber(Long chatId, String languageCode) {
        // Локализуем сообщение для запроса номера телефона
        String message = messageService.getLocalizedMessage("request_phone_number", languageCode);

        // Отправляем локализованное сообщение
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
        // Use messageService to get localized success message
        String message = messageService.getLocalizedMessage("success_message", languageCode);

        // Send the localized message with an inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, message, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
    }

    protected boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^\\+\\d{10,15}$");
    }

    protected void initialHelp(long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем локализованное сообщение для помощи
        String message = messageService.getLocalizedMessage("help_message", languageCode);

        // Создаем клавиатуру с кнопками
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Написать админу"
        InlineKeyboardButton adminButton = new InlineKeyboardButton();
        adminButton.setText(messageService.getLocalizedMessage("contact_admin_button", languageCode));
        adminButton.setCallbackData("/contact_admin");

        // Кнопка "Написать мастеру"
        InlineKeyboardButton masterButton = new InlineKeyboardButton();
        masterButton.setText(messageService.getLocalizedMessage("contact_master_button", languageCode));
        masterButton.setCallbackData("/contact_master");

        // Кнопка "Список команд"
        InlineKeyboardButton commandsButton = new InlineKeyboardButton();
        commandsButton.setText(messageService.getLocalizedMessage("list_commands_button", languageCode));
        commandsButton.setCallbackData("/list_commands");

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
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

        // Локализуем заголовок
        String header = messageService.getLocalizedMessage("help_menu_header", languageCode);

        // Список команд с локализацией
        Map<String, String> commands = Map.of(
                "/services", messageService.getLocalizedMessage("command_services", languageCode),
                "/review", messageService.getLocalizedMessage("command_review", languageCode),
                "/start", messageService.getLocalizedMessage("command_start", languageCode),
                "/help", messageService.getLocalizedMessage("command_help", languageCode),
                "/main_menu", messageService.getLocalizedMessage("command_main_menu", languageCode),
                "/back", messageService.getLocalizedMessage("command_back", languageCode),
                "/menu", messageService.getLocalizedMessage("command_menu", languageCode),
                "/cancel", messageService.getLocalizedMessage("command_cancel", languageCode),
                "/settings", messageService.getLocalizedMessage("command_settings", languageCode),
                "/book", messageService.getLocalizedMessage("command_book", languageCode)
        );

        // Формирование текста сообщения
        StringBuilder message = new StringBuilder(header);
        commands.forEach((command, description) -> message.append(command).append(" - ").append(description).append("\n"));

        // Создаем кнопку "Назад"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("button_back", languageCode));
        backButton.setCallbackData("/back");

        keyboard.setKeyboard(List.of(List.of(backButton)));

        // Отправляем сообщение с кнопкой
        messageService.sendMessageWithInlineKeyboard(chatId, message.toString(), keyboard);
    }

    public void contactAdmin(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Локализуем сообщение выбора действия
        String message = messageService.getLocalizedMessage("choose_action", languageCode);

        // Создаем клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Задать новый вопрос"
        InlineKeyboardButton newQuestionButton = new InlineKeyboardButton();
        newQuestionButton.setText(messageService.getLocalizedMessage("ask_new_question", languageCode));
        newQuestionButton.setCallbackData("/ask_new_question");

        // Кнопка "Посмотреть старые запросы"
        InlineKeyboardButton viewRequestsButton = new InlineKeyboardButton();
        viewRequestsButton.setText(messageService.getLocalizedMessage("view_requests", languageCode));
        viewRequestsButton.setCallbackData("/view_requests");

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("button_back", languageCode));
        backButton.setCallbackData("/help");

        // Добавляем кнопки в строки
        rows.add(List.of(newQuestionButton));
        rows.add(List.of(viewRequestsButton));
        rows.add(List.of(backButton));

        // Устанавливаем клавиатуру и отправляем сообщение
        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void askNewQuestion(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Локализуем сообщение для запроса нового вопроса
        String message = messageService.getLocalizedMessage("ask_new_question_prompt", languageCode);

        // Устанавливаем текущую команду
        userSession.setCurrentState(chatId, "/new_question");

        // Отправляем сообщение с просьбой написать вопрос
        messageService.sendMessage(chatId, message);
    }

    public void handleNewQuestion(Long chatId, String questionText) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Users users = userRepository.findByChatId(chatId);

        // Сохраняем запрос в базе данных
        helpService.createHelpRequest(users.getId(), questionText);

        // Локализуем сообщение о подтверждении
        String confirmationMessage = messageService.getLocalizedMessage("question_sent_confirmation", languageCode);
        messageService.sendMessage(chatId, confirmationMessage);

        // Возвращаем в меню "Написать админу"
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/contact_admin");
        userSession.setPreviousState(chatId, "/help");
        contactAdmin(chatId);
    }

    public void viewRequests(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Локализуем сообщение для выбора категории запросов
        String message = messageService.getLocalizedMessage("choose_request_category", languageCode);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Открытые"
        InlineKeyboardButton openRequestsButton = new InlineKeyboardButton();
        openRequestsButton.setText(messageService.getLocalizedMessage("open_requests", languageCode));
        openRequestsButton.setCallbackData("/view_open_requests");

        // Кнопка "В процессе"
        InlineKeyboardButton inProgressRequestsButton = new InlineKeyboardButton();
        inProgressRequestsButton.setText(messageService.getLocalizedMessage("in_progress_requests", languageCode));
        inProgressRequestsButton.setCallbackData("/view_in_progress_requests");

        // Кнопка "Завершенные"
        InlineKeyboardButton closedRequestsButton = new InlineKeyboardButton();
        closedRequestsButton.setText(messageService.getLocalizedMessage("closed_requests", languageCode));
        closedRequestsButton.setCallbackData("/view_closed_requests");

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back", languageCode));
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
            // Локализуем сообщение о том, что нет открытых запросов
            String noRequestsMessage = messageService.getLocalizedMessage("no_open_requests", languageCode);
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        // Локализуем сообщение для выбора даты открытого запроса
        String message = messageService.getLocalizedMessage("select_open_request_date", languageCode);

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
        backButton.setText(messageService.getLocalizedMessage("back", languageCode));
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleViewRequest(Long chatId, Long helpId) {
        // Retrieve the Help object from the repository
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        // Retrieve the language code for the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Localize the message using the messageService
        String message = messageService.getLocalizedMessage("view_request_message", languageCode)
                + "\n" + help.getHelpQuestion();

        // Create the keyboard with the "Back" button
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Localize the "Back" button text
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back", languageCode));
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Send the message with the inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleInProgressRequests(Long chatId) {
        // Retrieve the language code for the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        // Fetch in-progress requests
        List<Help> inProgressRequests = helpRepository.findByUser_IdAndStatus(userId, Help.HelpStatus.OPEN);

        // If no in-progress requests, send an appropriate message
        if (inProgressRequests.isEmpty()) {
            String noRequestsMessage = messageService.getLocalizedMessage("no_in_progress_requests", languageCode);
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        // Localized message for selecting a date
        String message = messageService.getLocalizedMessage("select_in_progress_request_date", languageCode);

        // Create the inline keyboard with buttons for each in-progress request
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Help help : inProgressRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString());  // Only date of the request
            button.setCallbackData("/progress_request_" + help.getId());
            rows.add(List.of(button));
        }

        // Add the "Back" button
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back", languageCode));
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));

        // Set the keyboard and send the message
        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleViewInProgressRequest(Long chatId, Long helpId) {
        // Retrieve the help request from the repository
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        // Retrieve the language code for the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Create the message with localized content
        String message = messageService.getLocalizedMessage("view_in_progress_request_message", languageCode);
        message = message.replace("{question}", help.getHelpQuestion())
                .replace("{response}", help.getAdminResponse());

        // Create the inline keyboard markup
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Create the "Yes" button (to close the request)
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(messageService.getLocalizedMessage("yes_button", languageCode));
        yesButton.setCallbackData("/close_request_" + helpId);

        // Create the "No" button (to ask a new question)
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(messageService.getLocalizedMessage("no_button", languageCode));
        noButton.setCallbackData("/new_question_" + helpId);

        // Create the "Back" button (to view requests)
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/view_requests");

        // Add buttons to rows
        rows.add(List.of(yesButton, noButton));
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        // Send the message with the inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleCloseRequest(Long chatId, Long helpId) {
        // Retrieve the language code for the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Retrieve the help request
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        // Set the status of the help request to CLOSED and save the date
        help.setStatus(Help.HelpStatus.CLOSED);
        help.setClosedAt(LocalDateTime.now());
        helpRepository.save(help);

        // Get localized message for request closure
        String message = messageService.getLocalizedMessage("request_closed", languageCode);

        // Send the closure confirmation message
        messageService.sendMessage(chatId, message);

        // Show the list of requests again
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
        // Retrieve the language code for the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        // Fetch the closed help requests for the user
        List<Help> closedRequests = helpRepository.findByUser_IdAndStatus(userId, Help.HelpStatus.CLOSED);

        if (closedRequests.isEmpty()) {
            // Get the localized message for when there are no closed requests
            String noRequestsMessage = messageService.getLocalizedMessage("no_closed_requests", languageCode);
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        // Get the localized message for selecting a closed request
        String message = messageService.getLocalizedMessage("select_closed_request_date", languageCode);

        // Prepare the inline keyboard
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Create a button for each closed request
        for (Help help : closedRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString()); // Show only the date
            button.setCallbackData("/closed_request_" + help.getId());
            rows.add(List.of(button));
        }

        // Add the back button
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/view_requests");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Send the message with the inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleViewClosedRequest(Long chatId, Long helpId) {
        // Retrieve the help request from the database
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        // Get the language code for the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Prepare the message using localized text
        String message = messageService.getLocalizedMessage("view_closed_request_message", languageCode) +
                "\n" + help.getHelpQuestion() +
                "\n" + messageService.getLocalizedMessage("admin_response", languageCode) + "\n" + help.getAdminResponse();

        // Create the inline keyboard
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Button to create a new request
        InlineKeyboardButton newRequestButton = new InlineKeyboardButton();
        newRequestButton.setText(messageService.getLocalizedMessage("create_new_request", languageCode));
        newRequestButton.setCallbackData("/ask_new_question");

        // Button to delete the request
        InlineKeyboardButton deleteRequestButton = new InlineKeyboardButton();
        deleteRequestButton.setText(messageService.getLocalizedMessage("delete_request", languageCode));
        deleteRequestButton.setCallbackData("/delete_request_" + helpId);

        // Back button to view requests
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/view_requests");

        // Add buttons to the keyboard
        rows.add(List.of(newRequestButton));
        rows.add(List.of(deleteRequestButton));
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        // Send the message with the inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void deleteRequests(Long chatId, Long helpId) {
        // Получаем язык пользователя
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем userId из chatId
        Long userId = userRepository.findByChatId(chatId).getId();

        if (userId == null) {
            // Отправляем сообщение, если пользователь не найден
            String userNotFoundMessage = messageService.getLocalizedMessage("user_not_found", languageCode);
            messageService.sendMessage(chatId, userNotFoundMessage);
            return;
        }

        // Проверяем, существует ли запрос с таким ID и принадлежит ли он пользователю
        Optional<Help> helpOptional = helpRepository.findById(helpId);

        if (helpOptional.isEmpty() || !helpOptional.get().getUser().getId().equals(userId)) {
            // Сообщаем, что запрос не найден
            String helpNotFoundMessage = messageService.getLocalizedMessage("help_not_found", languageCode);
            messageService.sendMessage(chatId, helpNotFoundMessage);
            return;
        }

        // Удаляем запрос
        helpRepository.deleteById(helpId);

        // Подтверждаем удаление
        String deleteSuccessMessage = messageService.getLocalizedMessage("delete_success", languageCode);
        messageService.sendMessage(chatId, deleteSuccessMessage);

        // Возвращаем в меню запросов
        viewRequests(chatId);
    }

    protected void masterRequests(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список всех мастеров
        List<Master> masters = masterRepository.findAllByStatus(Master.Status.ACTIVE);

        if (masters.isEmpty()) {
            // Локализованное сообщение, если мастера не найдены
            String noMastersMessage = messageService.getLocalizedMessage("no_masters_in_system", languageCode);
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
        cancelButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        cancelButton.setCallbackData("/help");
        rows.add(List.of(cancelButton));

        keyboard.setKeyboard(rows);

        // Локализованное сообщение для выбора мастера
        String message = messageService.getLocalizedMessage("select_master", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void masterContactRequests(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем информацию о мастере по его ID
        Optional<Master> masterOptional = masterRepository.findById(masterId);

        if (masterOptional.isEmpty()) {
            // Локализуем сообщение, если мастер не найден
            String noMasterFoundMessage = messageService.getLocalizedMessage("master_not_found", languageCode);
            messageService.sendMessage(chatId, noMasterFoundMessage);
            return;
        }

        Master master = masterOptional.get();

        // Формируем сообщение с информацией о мастере
        String message = messageService.getLocalizedMessage("master_info", languageCode);
        message = String.format(message, master.getName(), master.getPhoneNumber(), master.getDescription());

        // Создаем inline-клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для социальных сетей мастера (если есть ссылка)
        if (master.getSocialLink() != null && !master.getSocialLink().isEmpty()) {
            InlineKeyboardButton socialButton = new InlineKeyboardButton();
            socialButton.setText(messageService.getLocalizedMessage("social_link", languageCode));
            socialButton.setUrl(master.getSocialLink());
            rows.add(List.of(socialButton));
        }

        // Кнопка для возврата назад
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/contact_master");

        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void initialChangeNameNLastName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Users user = userRepository.findByChatId(chatId);

        // Локализуем приветственное сообщение
        String message = messageService.getLocalizedMessage("change_name_greeting", languageCode);
        message = String.format(message, user.getFirstName(), user.getLastName());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка для изменения имени
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton changeFirstNameButton = new InlineKeyboardButton();
        changeFirstNameButton.setText(messageService.getLocalizedMessage("change_first_name", languageCode));
        changeFirstNameButton.setCallbackData("/change_first_name");
        row1.add(changeFirstNameButton);
        rowsInline.add(row1);

        // Кнопка для изменения фамилии
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton changeLastNameButton = new InlineKeyboardButton();
        changeLastNameButton.setText(messageService.getLocalizedMessage("change_last_name", languageCode));
        changeLastNameButton.setCallbackData("/change_last_name");
        row2.add(changeLastNameButton);
        rowsInline.add(row2);

        // Кнопка "Назад" для возврата в настройки
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
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

        // Локализуем запрос для ввода нового имени
        String message = messageService.getLocalizedMessage("change_first_name_prompt", languageCode);

        messageService.sendMessage(chatId, message);

        // Устанавливаем состояние ожидания ввода нового имени
        userSession.setCurrentState(chatId, "/waiting_for_first_name");
    }

    public void handleNewFirstName(Long chatId, String newFirstName) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Проверка длины имени
        if (newFirstName == null || newFirstName.trim().length() < 2) {
            // Локализуем ошибку
            String errorMessage = messageService.getLocalizedMessage("first_name_length_error", languageCode);
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        // Локализуем сообщение с подтверждением
        String confirmationMessage = messageService.getLocalizedMessage("confirm_first_name_change", languageCode, newFirstName);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Кнопка "Да"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(messageService.getLocalizedMessage("yes", languageCode));
        yesButton.setCallbackData("/confirm_change_first_name_" + newFirstName);
        row1.add(yesButton);
        rowsInline.add(row1);

        // Кнопка "Нет"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(messageService.getLocalizedMessage("no", languageCode));
        noButton.setCallbackData("/cancel_change_first_name");
        row2.add(noButton);
        rowsInline.add(row2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        userSession.setCurrentState(chatId, "/settings");

        // Отправляем сообщение с клавиатурой
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, inlineKeyboardMarkup);
    }

    protected void confirmChangeFirstName(Long chatId, String newFirstName) {
        // Обновляем имя в базе данных
        userRepository.updateFirstName(chatId, newFirstName);

        // Получаем код языка пользователя
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Локализуем сообщение об успешном изменении имени
        String successMessage = messageService.getLocalizedMessage("first_name_change_success", languageCode, newFirstName);

        messageService.sendMessage(chatId, successMessage);

        // Возвращаемся в начальное меню изменения имени и фамилии
        initialChangeNameNLastName(chatId);
    }

    protected void cancelChangeFirstName(Long chatId) {
        // Получаем код языка пользователя
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Локализуем сообщение об отмене
        String cancelMessage = messageService.getLocalizedMessage("first_name_change_cancelled", languageCode);

        messageService.sendMessage(chatId, cancelMessage);

        // Возвращаемся в начальное меню изменения имени и фамилии
        initialChangeNameNLastName(chatId);
    }

    protected void changeLastName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Localize the message for the new last name request
        String message = messageService.getLocalizedMessage("enter_new_last_name", languageCode);

        messageService.sendMessage(chatId, message);

        // Set the state for waiting for the new last name
        userSession.setCurrentState(chatId, "/waiting_for_last_name");
    }

    public void handleNewLastName(Long chatId, String newLastName) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Validation for last name length
        if (newLastName == null || newLastName.trim().length() < 2) {
            // Localize error message
            String errorMessage = messageService.getLocalizedMessage("last_name_error", languageCode);
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        // Localize confirmation message
        String confirmationMessage = messageService.getLocalizedMessage("confirm_change_last_name", languageCode) + " " + newLastName + "?";

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Button "Yes"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(messageService.getLocalizedMessage("yes", languageCode));
        yesButton.setCallbackData("/confirm_change_last_name_" + newLastName);
        row1.add(yesButton);
        rowsInline.add(row1);

        // Button "No"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(messageService.getLocalizedMessage("no", languageCode));
        noButton.setCallbackData("/cancel_change_last_name");
        row2.add(noButton);
        rowsInline.add(row2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Set state and send message with the keyboard
        userSession.setCurrentState(chatId, "/settings");
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, inlineKeyboardMarkup);
    }

    protected void confirmChangeLastName(Long chatId, String newLastName) {
        // Update the last name in the database
        userRepository.updateLastName(chatId, newLastName);

        // Localize success message
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String successMessage = messageService.getLocalizedMessage("success_change_last_name", languageCode) + " " + newLastName;

        messageService.sendMessage(chatId, successMessage);

        // Return to the initial menu for changing names
        initialChangeNameNLastName(chatId);
    }

    protected void cancelChangeLastName(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Localize cancellation message
        String cancelMessage = messageService.getLocalizedMessage("cancel_change_last_name", languageCode);

        messageService.sendMessage(chatId, cancelMessage);

        // Return to the initial menu for changing names
        initialChangeNameNLastName(chatId);
    }

    protected void initialChangeLanguage(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        userSession.setCurrentState(chatId, "/choose_lang");

        // Create the keyboard with buttons for language selection
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Button for selecting Russian language
        InlineKeyboardButton ruButton = new InlineKeyboardButton();
        ruButton.setText(messageService.getLocalizedMessage("language_russian", languageCode));
        ruButton.setCallbackData("/lang_ru");

        // Button for selecting English language
        InlineKeyboardButton enButton = new InlineKeyboardButton();
        enButton.setText(messageService.getLocalizedMessage("language_english", languageCode));
        enButton.setCallbackData("/lang_en");

        // Button for selecting Ukrainian language
        InlineKeyboardButton ukButton = new InlineKeyboardButton();
        ukButton.setText(messageService.getLocalizedMessage("language_ukrainian", languageCode));
        ukButton.setCallbackData("/lang_uk");

        // Add buttons to the row
        rows.add(Arrays.asList(ruButton, enButton, ukButton));

        // Back button
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/settings");
        rows.add(Collections.singletonList(backButton));

        inlineKeyboardMarkup.setKeyboard(rows);

        // Localized message for selecting a language
        String message = messageService.getLocalizedMessage("select_language_message", languageCode);

        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    protected void changeLanguage(Long chatId, String newLanguageCode) {
        // Validation of language code
        List<String> validLanguageCodes = Arrays.asList("ru", "en", "uk");
        if (!validLanguageCodes.contains(newLanguageCode)) {
            messageService.sendMessage(chatId, messageService.getLocalizedMessage("invalid_language_code", newLanguageCode));
            return;
        }

        // Update the language in the database
        userRepository.updateLanguageCodeByChatId(chatId, newLanguageCode);

        // Send message about successful language change
        String successMessage = messageService.getLocalizedMessage("language_change_success", newLanguageCode);
        messageService.sendMessage(chatId, successMessage);

        // Return the user to the main settings menu
        initialChangeLanguage(chatId);
    }

    protected void initialChangePhoneNumber(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Get the localized message for requesting a new phone number
        String message = messageService.getLocalizedMessage("phone_number_change_prompt", languageCode);

        // Create the inline keyboard with a "Back" button
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // "Back" button to return to the settings menu
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/settings"); // Command to return to settings
        rows.add(List.of(backButton));

        inlineKeyboardMarkup.setKeyboard(rows);

        // Send the message with the inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, message, inlineKeyboardMarkup);

        // Set the current state to wait for the new phone number
        userSession.setCurrentState(chatId, "/waiting_for_phone_number");
    }

    public void handleNewPhoneNumber(Long chatId, String newPhoneNumber) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Check phone number format: should start with "+" and contain 10 to 15 digits
        if (!newPhoneNumber.matches("^\\+\\d{10,15}$")) {
            String errorMessage = messageService.getLocalizedMessage("invalid_phone_number_format", languageCode);
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        // Confirmation message for phone number change
        String confirmationMessage = messageService.getLocalizedMessage("confirm_phone_number_change", languageCode);
        confirmationMessage = String.format(confirmationMessage, newPhoneNumber);

        // Create inline buttons for confirmation or cancellation
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // "Yes" button for confirming the phone number change
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(messageService.getLocalizedMessage("yes", languageCode));
        yesButton.setCallbackData("/confirm_change_phone_number_" + newPhoneNumber);

        // "No" button for cancelling the phone number change
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(messageService.getLocalizedMessage("no", languageCode));
        noButton.setCallbackData("/cancel_change_phone_number");

        rowsInline.add(List.of(yesButton, noButton));
        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Send the confirmation message with inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, inlineKeyboardMarkup);
        userSession.setCurrentState(chatId, "/settings");
    }

    protected void confirmChangePhoneNumber(Long chatId, String newPhoneNumber) {
        // Update the phone number in the database
        userRepository.updatePhoneNumberByChatId(chatId, newPhoneNumber);

        // Get the language code of the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Localized success message for phone number change
        String successMessage = messageService.getLocalizedMessage("phone_number_change_success", languageCode);
        successMessage = String.format(successMessage, newPhoneNumber);

        // Send the success message
        messageService.sendMessage(chatId, successMessage);

        // Update user session state
        userSession.setCurrentState(chatId, "/settings");

        // Return to the settings menu
        menuService.handleSettingsCommand(chatId);
    }

    public void cancelChangePhoneNumber(Long chatId) {
        // Get the language code of the user
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Localized cancellation message
        String cancelMessage = messageService.getLocalizedMessage("phone_number_change_cancelled", languageCode);

        // Send the cancellation message
        messageService.sendMessage(chatId, cancelMessage);

        // Update user session state
        userSession.setCurrentState(chatId, "/settings");

        // Return to the settings menu
        menuService.handleSettingsCommand(chatId);
    }

    public void initialWriteToAdmin(Long chatId, Long adminChatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Get information about the admin
        Users admin = userRepository.findByChatId(adminChatId);
        if (admin == null) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/main_menu");
            messageService.sendMessageWithInlineKeyboard(chatId,
                    messageService.getLocalizedMessage("admin_not_found", languageCode, adminChatId),
                    autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        // Localized message for the user
        String message = messageService.getLocalizedMessage("write_message_to_admin", languageCode) + " " + admin.getFirstName();
        messageService.sendMessage(chatId, message);

        // Set the session state for writing to admin
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
            messageService.sendMessage(masterChatId, messageService.getLocalizedMessage("user_not_found", languageCode));
            return;
        }

        // Формируем сообщение для администратора с использованием локализации
        String messageToAdmin = messageService.getLocalizedMessage("message_to_admin", adminLanguageCode) + "\n"
                + users.getFirstName() + " " + users.getLastName() + "\n\n"
                + messageService.getLocalizedMessage("message_from_user", adminLanguageCode) + "\n" + messageText;

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText(messageService.getLocalizedMessage("reply_button", adminLanguageCode));
        replyButton.setCallbackData("/write_user_" + masterChatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Отправляем сообщение администратору
        messageService.sendMessageWithInlineKeyboard(adminChatId, messageToAdmin, keyboard);

        // Подтверждаем мастеру отправку сообщения
        String confirmationMessage = messageService.getLocalizedMessage("message_sent_confirmation", languageCode);

        // Очищаем состояние чата мастера
        userSession.clearStates(masterChatId);
        userSession.setCurrentState(masterChatId, "/main_menu");

        messageService.sendMessageWithInlineKeyboard(masterChatId, confirmationMessage, autUserButtons.getAuthenticatedInlineKeyboard(masterChatId));
    }

    public void initialWriteToMaster(Long chatId, Long masterChatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем информацию о мастере
        Users master = userRepository.findByChatId(masterChatId);
        if (master == null) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/main_menu");
            messageService.sendMessageWithInlineKeyboard(chatId,
                    messageService.getLocalizedMessage("master_not_found", languageCode) + masterChatId,
                    autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        // Сообщение пользователю
        String message = messageService.getLocalizedMessage("write_to_master", languageCode) + " " + master.getFirstName();

        messageService.sendMessage(chatId, message);

        // Устанавливаем состояние для чата
        userSession.setCurrentState(chatId, "/writing_to_master_from_user_" + masterChatId);
        userSession.setPreviousState(chatId, "/main_menu");
    }

    public void writeToMaster(Long chatId, Long masterChatId, String messageText) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // For the user
        String masterLanguageCode = userRepository.findLanguageCodeByChatId(masterChatId); // For the master

        Users users = userRepository.findByChatId(chatId);
        if (users == null) {
            userSession.clearStates(masterChatId);
            userSession.setCurrentState(masterChatId, "/main_menu");
            messageService.sendMessage(masterChatId, "User not found with chat ID: " + chatId);
            return;
        }

        // Prepare the message for the master using localized strings
        String messageToMaster = messageService.getLocalizedMessage("user_wrote_to_you", masterLanguageCode) + "\n" +
                users.getFirstName() + "\n" + users.getLastName() + "\n\n" +
                messageService.getLocalizedMessage("message", masterLanguageCode) + ":\n" + messageText;

        // Create keyboard with "Reply" button
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText(messageService.getLocalizedMessage("reply", masterLanguageCode));
        replyButton.setCallbackData("/message_client_" + chatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Send the message to the master
        messageService.sendMessageWithInlineKeyboard(masterChatId, messageToMaster, keyboard);

        // Send confirmation to the user that the message was sent
        String confirmationMessage = messageService.getLocalizedMessage("message_sent_to_master", languageCode);
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/main_menu");

        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
    }
}

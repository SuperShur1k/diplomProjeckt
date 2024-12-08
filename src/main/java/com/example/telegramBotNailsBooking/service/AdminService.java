package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.bot.commands.AdminCommandHandler;
import com.example.telegramBotNailsBooking.model.*;
import com.example.telegramBotNailsBooking.repository.*;
import com.example.telegramBotNailsBooking.service.buttons.AdminButtons;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminCommandHandler.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserSession userSession;

    @Autowired
    private AdminButtons adminButtons;

    @Autowired
    private AutUserButtons autUserButtons;

    @Autowired
    private HelpRepository helpRepository;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AvailableDateService availableDateService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AvailableDateRepository availableDateRepository;
    @Autowired
    private ServiceManagementService serviceManagementService;
    @Autowired
    private ServiceRepository serviceRepository;

    public void showAdminPanel(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Локализуем сообщение
        String message;
        if ("ru".equals(languageCode)) {
            message = "Добро пожаловать в панель администратора. Вы можете управлять пользователями и назначать новых администраторов.";
        } else if ("uk".equals(languageCode)) {
            message = "Ласкаво просимо в панель адміністратора. Ви можете керувати користувачами та призначати нових адміністраторів.";
        } else {
            message = "Welcome to the Admin Panel. You can manage users and set new admins.";
        }

        messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getAdminInlineKeyboard(chatId));
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/admin");
        userSession.setPreviousState(chatId, "/main_menu");
    }

    protected void initiateSetAdmin(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Локализуем сообщение
        String message;
        if ("ru".equals(languageCode)) {
            message = "Пожалуйста, введите номер телефона пользователя, которого вы хотите назначить администратором.";
        } else if ("uk".equals(languageCode)) {
            message = "Будь ласка, введіть номер телефону користувача, якого ви хочете призначити адміністратором.";
        } else {
            message = "Please enter the phone number of the user you want to make an admin.";
        }

        messageService.sendMessage(chatId, message);
        userSession.setSettingAdmin(chatId, true);

        if ("ru".equals(languageCode)) {
            message = "Вы можете отменить эту операцию, используя кнопку ниже.";
        } else if ("uk".equals(languageCode)) {
            message = "Ви можете скасувати цю операцію, використовуючи кнопку нижче.";
        } else {
            message = "You can cancel this operation using the button below.";
        }
        messageService.sendMessageWithInlineKeyboard(chatId, message, autUserButtons.getCancelInlineKeyboard(chatId));

    }

    public void setAdmin(Long chatId, String phone) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Находим пользователя по номеру телефона
        Users users = userRepository.findByPhoneNumber(phone);
        if (users == null) {
            String message;
            if ("ru".equals(languageCode)) {
                message = "Пользователь не найден. Пожалуйста, попробуйте снова.";
            } else if ("uk".equals(languageCode)) {
                message = "Користувача не знайдено. Будь ласка, спробуйте ще раз.";
            } else {
                message = "User not found. Please try again.";
            }

            messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getAdminInlineKeyboard(chatId));
            userSession.setSettingAdmin(chatId, false); // Сбрасываем состояние назначения администратора
        } else {
            // Назначаем роль администратора
            users.setRole(Users.Role.ADMIN);

            log.info("Saving user with role: {}", users.getRole());

            userRepository.save(users);

            String message;
            if ("ru".equals(languageCode)) {
                message = "Пользователь " + users.getFirstName() + " " + users.getLastName() + " успешно получил права администратора.";
            } else if ("uk".equals(languageCode)) {
                message = "Користувач " + users.getFirstName() + " " + users.getLastName() + " успішно отримав права адміністратора.";
            } else {
                message = "User " + users.getFirstName() + " " + users.getLastName() + " has been successfully granted admin rights.";
            }

            messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getAdminInlineKeyboard(chatId));
            userSession.setSettingAdmin(chatId, false); // Сбрасываем состояние назначения администратора
        }
    }

    public void initiateDelAdmin(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список всех администраторов
        List<Users> admins = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Users.Role.ADMIN)
                .collect(Collectors.toList());

        if (admins.isEmpty()) {
            String noAdminsMessage = "ru".equals(languageCode)
                    ? "В системе нет администраторов, которых можно снять с роли."
                    : "uk".equals(languageCode)
                    ? "У системі немає адміністраторів, яких можна позбавити ролі."
                    : "There are no administrators in the system to remove the role.";
            messageService.sendMessageWithInlineKeyboard(chatId, noAdminsMessage, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        // Создаем inline-кнопки с именами администраторов
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Users admin : admins) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(admin.getFirstName() + " " + admin.getLastName()); // Имя и фамилия администратора
            button.setCallbackData("/remove_admin_" + admin.getId()); // Устанавливаем ID администратора в callback
            rows.add(List.of(button));
        }

        // Добавляем кнопку "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(
                "ru".equals(languageCode) ? "Отмена" :
                        "uk".equals(languageCode) ? "Скасувати" :
                                "Cancel"
        );
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите администратора, чтобы снять его роль."
                : "uk".equals(languageCode)
                ? "Виберіть адміністратора, щоб зняти його роль."
                : "Select an admin to remove their role.";

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void removeAdminById(Long chatId, Long adminId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Users admin = userRepository.findById(adminId).orElse(null);
        if (admin == null || admin.getRole() != Users.Role.ADMIN) {
            String message = "ru".equals(languageCode)
                    ? "Администратор не найден или он уже не является администратором."
                    : "uk".equals(languageCode)
                    ? "Адміністратора не знайдено або він вже не є адміністратором."
                    : "Admin not found or they are no longer an administrator.";

            messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        admin.setRole(Users.Role.CLIENT);
        userRepository.save(admin);

        String successMessage = "ru".equals(languageCode)
                ? "Роль администратора успешно снята с пользователя " + admin.getFirstName() + " " + admin.getLastName() + "."
                : "uk".equals(languageCode)
                ? "Роль адміністратора успішно знято з користувача " + admin.getFirstName() + " " + admin.getLastName() + "."
                : "Admin role has been successfully removed from user " + admin.getFirstName() + " " + admin.getLastName() + ".";

        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getAdminInlineKeyboard(chatId));
    }

    public void cancelAdminAction(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        userSession.clearSession(chatId);

        String message;
        if ("ru".equals(languageCode)) {
            message = "Текущая операция была отменена.";
        } else if ("uk".equals(languageCode)) {
            message = "Поточну операцію було скасовано.";
        } else {
            message = "Current operation has been cancelled.";
        }

        messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getAdminInlineKeyboard(chatId));
    }

    public void takeAnswerToHelp(Long chatId) {
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
        openRequestsButton.setCallbackData("/open_requests");

        // Кнопка "В процессе"
        InlineKeyboardButton inProgressRequestsButton = new InlineKeyboardButton();
        inProgressRequestsButton.setText("ru".equals(languageCode) ? "В процессе" : "uk".equals(languageCode) ? "У процесі" : "In Progress");
        inProgressRequestsButton.setCallbackData("/progress_requests");

        // Кнопка "Завершенные"
        InlineKeyboardButton closedRequestsButton = new InlineKeyboardButton();
        closedRequestsButton.setText("ru".equals(languageCode) ? "Завершенные" : "uk".equals(languageCode) ? "Завершені" : "Closed");
        closedRequestsButton.setCallbackData("/closed_requests");

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");

        rows.add(List.of(openRequestsButton));
        rows.add(List.of(inProgressRequestsButton));
        rows.add(List.of(closedRequestsButton));
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void openRequest(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        List<Help> openRequests = helpRepository.findByStatus(Help.HelpStatus.WAIT);

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
            button.setCallbackData("/view_open_" + help.getId());
            rows.add(List.of(button));
        }

        // Добавляем кнопки назад
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void viewOpenRequest(Long chatId, Long requestId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        String message = "ru".equals(languageCode)
                ? "Запрос на помощь:\n" + help.getHelpQuestion()
                : "uk".equals(languageCode)
                ? "Запит на допомогу:\n" + help.getHelpQuestion()
                : "Help request:\n" + help.getHelpQuestion();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Дать ответ"
        InlineKeyboardButton answerButton = new InlineKeyboardButton();
        answerButton.setText("ru".equals(languageCode)
                ? "Дать ответ"
                : "uk".equals(languageCode)
                ? "Дати відповідь"
                : "Answer");
        answerButton.setCallbackData("/answer_request_" + requestId);
        rows.add(List.of(answerButton));

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void initialAnswerRequest(Long chatId, Long requestId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Сохраняем состояние для обработки ввода ответа
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/waiting_answer_" + requestId);

        // Сообщение для администратора с запросом ответа
        String message = "ru".equals(languageCode)
                ? "Введите ваш ответ на запрос помощи:"
                : "uk".equals(languageCode)
                ? "Введіть вашу відповідь на запит про допомогу:"
                : "Please enter your response to the help request:";

        messageService.sendMessage(chatId, message);
    }

    public void answerRequest(Long chatId, Long requestId, String answer) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Находим запрос помощи
        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found with id: " + requestId));

        // Устанавливаем данные администратора, ответ и статус
        help.setAdmin(userRepository.findByChatId(chatId));
        help.setAdminResponse(answer);
        help.setStatus(Help.HelpStatus.OPEN);
        helpRepository.save(help);

        // Отправляем подтверждение админу о том, что ответ сохранен
        String confirmationMessage = "ru".equals(languageCode)
                ? "Ваш ответ на запрос помощи сохранен."
                : "uk".equals(languageCode)
                ? "Ваш відповідь на запит про допомогу збережено."
                : "Your response to the help request has been saved.";

        messageService.sendMessage(chatId, confirmationMessage);

        // Очищаем состояние пользователя и возвращаем его к списку открытых запросов
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/open_requests");
        userSession.setPreviousState(chatId, "/reply_to_help");
        openRequest(chatId);
    }

    public void initialProgressRequest(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        List<Help> inProgressRequests = helpRepository.findByAdmin_IdAndStatus(userId, Help.HelpStatus.OPEN);

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
            button.setCallbackData("/in_progress_" + help.getId());
            rows.add(List.of(button));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void progressRequest(Long chatId, Long requestId) {
        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Запрос пользователя:\n" + help.getHelpQuestion() + "\nВаш ответ:\n" + help.getAdminResponse()
                : "uk".equals(languageCode)
                ? "Запит користувача:\n" + help.getHelpQuestion() + "\nВаша відповідь:\n" + help.getAdminResponse()
                : "User request:\n" + help.getHelpQuestion() + "\nYour response:\n" + help.getAdminResponse();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void initialClosedRequest(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        List<Help> closedRequests = helpRepository.findByAdmin_IdAndStatus(userId, Help.HelpStatus.CLOSED);

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
            button.setCallbackData("/request_closed_" + help.getId());
            rows.add(List.of(button));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void closeRequest(Long chatId, Long requestId) {
        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String message = "ru".equals(languageCode)
                ? "Запрос пользователя:\n" + help.getHelpQuestion() + "\nВаш ответ:\n" + help.getAdminResponse()
                : "uk".equals(languageCode)
                ? "Запит користувача:\n" + help.getHelpQuestion() + "\nВаша відповідь:\n" + help.getAdminResponse()
                : "User request:\n" + help.getHelpQuestion() + "\nYour response:\n" + help.getAdminResponse();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    private void chooseRecipient(Long chatId, String recipientType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Подготавливаем текст в зависимости от типа получателя
        String message = "ru".equals(languageCode)
                ? recipientType.equals("master") ? "Выберите мастера, чтобы написать:" : "Выберите пользователя, чтобы написать:"
                : "uk".equals(languageCode)
                ? recipientType.equals("master") ? "Оберіть майстра, щоб написати:" : "Оберіть користувача, щоб написати:"
                : recipientType.equals("master") ? "Choose a master to write to:" : "Choose a user to write to:";

        List<?> recipients = recipientType.equals("master")
                ? masterRepository.findAllByStatus(Master.Status.ACTIVE)
                : userRepository.findAll();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Object recipient : recipients) {
            InlineKeyboardButton button = new InlineKeyboardButton();

            if (recipient instanceof Master) {
                Master master = (Master) recipient;
                button.setText(master.getName());
                button.setCallbackData("/write_master_" + master.getChatId());
            } else if (recipient instanceof Users) {
                Users user = (Users) recipient;
                button.setText(user.getFirstName() + " " + user.getLastName());
                button.setCallbackData("/write_user_" + user.getChatId());
            }

            rows.add(List.of(button));
        }

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void chooseWriteToMaster(Long chatId) {
        userSession.setCurrentState(chatId, "/write_to_master");
        userSession.setPreviousState(chatId, "/other_actions");
        chooseRecipient(chatId, "master");
    }

    public void chooseWriteToUser(Long chatId) {
        userSession.setCurrentState(chatId, "/write_to_client");
        userSession.setPreviousState(chatId, "/other_actions");
        chooseRecipient(chatId, "user");
    }

    public void initialWriteToRecipient(Long chatId, Long recipientChatId, String recipientType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Подготавливаем текст в зависимости от типа получателя
        String message = "ru".equals(languageCode)
                ? recipientType.equals("master") ? "Напишите ваше сообщение мастеру:" : "Напишите ваше сообщение пользователю:"
                : "uk".equals(languageCode)
                ? recipientType.equals("master") ? "Напишіть ваше повідомлення майстру:" : "Напишіть ваше повідомлення користувачу:"
                : recipientType.equals("master") ? "Write your message to the master:" : "Write your message to the user:";

        // Устанавливаем состояние ожидания ввода сообщения
        userSession.setCurrentState(chatId, recipientType.equals("master")
                ? "/writing_to_master_" + recipientChatId
                : "/writing_to_user_" + recipientChatId);

        messageService.sendMessage(chatId, message);
    }

    public void writeToRecipient(Long senderChatId, Long recipientChatId, String messageText, String recipientType) {
        String senderLanguageCode = userRepository.findLanguageCodeByChatId(senderChatId);
        String recipientLanguageCode = userRepository.findLanguageCodeByChatId(recipientChatId);

        // Формируем сообщение для получателя
        String messageToRecipient = "ru".equals(recipientLanguageCode)
                ? "Вам написал админ:\n" + messageText
                : "uk".equals(recipientLanguageCode)
                ? "Вам написав адмін:\n" + messageText
                : "An admin wrote to you:\n" + messageText;

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText("ru".equals(recipientLanguageCode)
                ? "Ответить"
                : "uk".equals(recipientLanguageCode)
                ? "Відповісти"
                : "Reply");
        replyButton.setCallbackData(recipientType.equals("master") ? "/reply_to_admin_master_" + senderChatId
                : "/reply_to_admin_user_" + senderChatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Отправляем сообщение получателю
        messageService.sendMessageWithInlineKeyboard(recipientChatId, messageToRecipient, keyboard);

        // Подтверждаем отправителю
        String confirmationMessage = "ru".equals(senderLanguageCode)
                ? recipientType.equals("master") ? "Ваше сообщение отправлено мастеру." : "Ваше сообщение отправлено пользователю."
                : "uk".equals(senderLanguageCode)
                ? recipientType.equals("master") ? "Ваше повідомлення надіслано майстру." : "Ваше повідомлення надіслано користувачу."
                : recipientType.equals("master") ? "Your message has been sent to the master." : "Your message has been sent to the user.";

        messageService.sendMessage(senderChatId, confirmationMessage);

        // Очищаем состояние отправителя
        userSession.clearStates(senderChatId);
        userSession.setCurrentState(senderChatId, recipientType.equals("master") ? "/write_to_master" : "/write_to_client");
        userSession.setPreviousState(senderChatId, "/other_actions");
        if (recipientType.equals("master")) {
            chooseWriteToMaster(senderChatId);
        } else {
            chooseWriteToUser(senderChatId);
        }
    }

    public void adminChooseClient(Long chatId, String actionType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список уникальных пользователей с CONFIRMED записями
        List<Users> clientsWithAppointments = appointmentRepository.findDistinctUsersByStatus(Appointment.Status.CONFIRMED);

        if (clientsWithAppointments.isEmpty()) {
            String message = "ru".equals(languageCode)
                    ? ("TRANSFER".equals(actionType) ? "Нет клиентов с записями для переноса." : "Нет клиентов с записями для отмены.")
                    : "uk".equals(languageCode)
                    ? ("TRANSFER".equals(actionType) ? "Немає клієнтів із записами для перенесення." : "Немає клієнтів із записами для скасування.")
                    : ("TRANSFER".equals(actionType) ? "No clients with appointments available for transfer." : "No clients with appointments available for cancellation.");
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для каждого клиента
        for (Users client : clientsWithAppointments) {
            InlineKeyboardButton clientButton = new InlineKeyboardButton(client.getFirstName() + " " + client.getLastName());
            clientButton.setCallbackData(("/admin_select_client_" + actionType + "_" + client.getChatId()));
            rows.add(List.of(clientButton));
        }

        // Добавляем кнопку "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Назад" :
                        "uk".equals(languageCode) ? "Назад" : "Back"
        );
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Текст сообщения в зависимости от действия
        String selectClientMessage = "ru".equals(languageCode)
                ? ("TRANSFER".equals(actionType) ? "Выберите клиента для переноса записи:" : "Выберите клиента для отмены записи:")
                : "uk".equals(languageCode)
                ? ("TRANSFER".equals(actionType) ? "Оберіть клієнта для перенесення запису:" : "Оберіть клієнта для скасування запису:")
                : ("TRANSFER".equals(actionType) ? "Select a client to transfer an appointment:" : "Select a client to cancel an appointment:");
        messageService.sendMessageWithInlineKeyboard(chatId, selectClientMessage, keyboard);
    }

    public void adminChooseDateForClient(Long chatId, Long clientChatId, String actionType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        List<Appointment> appointments = appointmentRepository.findByChatIdAndStatus(clientChatId, Appointment.Status.CONFIRMED);

        if (appointments.isEmpty()) {
            String message = "ru".equals(languageCode)
                    ? ("TRANSFER".equals(actionType) ? "Нет записей для переноса." : "Нет записей для отмены.")
                    : "uk".equals(languageCode)
                    ? ("TRANSFER".equals(actionType) ? "Немає записів для перенесення." : "Немає записів для скасування.")
                    : ("TRANSFER".equals(actionType) ? "No appointments available for transfer." : "No appointments available for cancellation.");
            messageService.sendMessage(chatId, message);
            return;
        }

        Map<LocalDate, List<Appointment>> groupedByDate = appointments.stream()
                .sorted(Comparator.comparing(Appointment::getAppointmentDate))
                .collect(Collectors.groupingBy(app -> app.getAppointmentDate().toLocalDate()));

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Appointment>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();

            InlineKeyboardButton dateButton = new InlineKeyboardButton(date.toString());
            dateButton.setCallbackData(("/admin_select_date_" + actionType + "_" + clientChatId + "_" + date));
            rows.add(List.of(dateButton));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Назад" :
                        "uk".equals(languageCode) ? "Назад" : "Back"
        );
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String selectDateMessage = "ru".equals(languageCode)
                ? ("TRANSFER".equals(actionType) ? "Выберите дату записи клиента для переноса:" : "Выберите дату записи клиента для отмены:")
                : "uk".equals(languageCode)
                ? ("TRANSFER".equals(actionType) ? "Оберіть дату запису клієнта для перенесення:" : "Оберіть дату запису клієнта для скасування:")
                : ("TRANSFER".equals(actionType) ? "Select a client's appointment date for transfer:" : "Select a client's appointment date for cancellation:");
        messageService.sendMessageWithInlineKeyboard(chatId, selectDateMessage, keyboard);
    }

    public void adminChooseTimeForDate(Long chatId, Long clientChatId, LocalDate date, String actionType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        List<Appointment> appointments = appointmentRepository.findByChatIdAndStatus(clientChatId, Appointment.Status.CONFIRMED).stream()
                .filter(app -> app.getAppointmentDate().toLocalDate().equals(date))
                .sorted(Comparator.comparing(app -> app.getAppointmentDate().toLocalTime()))
                .collect(Collectors.toList());

        if (appointments.isEmpty()) {
            String message = "ru".equals(languageCode)
                    ? "Нет доступных временных слотов для выбранной даты."
                    : "uk".equals(languageCode)
                    ? "Немає доступних часових слотів для обраної дати."
                    : "No time slots available for the selected date.";
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Appointment appointment : appointments) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton(appointment.getAppointmentDate().toLocalTime().toString());
            timeButton.setCallbackData(("/admin_select_time_" + actionType + "_" + appointment.getId() + "_" + date + "_" + clientChatId));
            rows.add(List.of(timeButton));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Назад" :
                        "uk".equals(languageCode) ? "Назад" : "Back"
        );
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String selectTimeMessage = "ru".equals(languageCode)
                ? ("TRANSFER".equals(actionType) ? "Выберите время записи клиента для переноса:" : "Выберите время записи клиента для отмены:")
                : "uk".equals(languageCode)
                ? ("TRANSFER".equals(actionType) ? "Оберіть час запису клієнта для перенесення:" : "Оберіть час запису клієнта для скасування:")
                : ("TRANSFER".equals(actionType) ? "Select a client's appointment time for transfer:" : "Select a client's appointment time for cancellation:");
        messageService.sendMessageWithInlineKeyboard(chatId, selectTimeMessage, keyboard);
    }

    public void adminConfirmAction(Long chatId, Long appointmentId, String actionType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем запись по ID
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = "ru".equals(languageCode)
                    ? "Запись не найдена."
                    : "uk".equals(languageCode)
                    ? "Запис не знайдено."
                    : "Appointment not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        // Формируем сообщение подтверждения в зависимости от типа действия
        String confirmationMessage = "ru".equals(languageCode)
                ? ("TRANSFER".equals(actionType)
                ? "Вы выбрали запись клиента на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + ". Хотите перенести?"
                : "Вы выбрали запись клиента на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + ". Хотите отменить?")
                : "uk".equals(languageCode)
                ? ("TRANSFER".equals(actionType)
                ? "Ви вибрали запис клієнта на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + ". Бажаєте перенести?"
                : "Ви вибрали запис клієнта на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + ". Бажаєте скасувати?")
                : ("TRANSFER".equals(actionType)
                ? "You selected the client's appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + ". Would you like to transfer?"
                : "You selected the client's appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + ". Would you like to cancel?");

        // Формируем клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        // Кнопка подтверждения
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(
                "ru".equals(languageCode)
                        ? ("TRANSFER".equals(actionType) ? "Да, перенести запись" : "Да, отменить запись")
                        : "uk".equals(languageCode)
                        ? ("TRANSFER".equals(actionType) ? "Так, перенести запис" : "Так, скасувати запис")
                        : ("TRANSFER".equals(actionType) ? "Yes, Transfer Appointment" : "Yes, Cancel Appointment")
        );
        confirmButton.setCallbackData("/admin_confirm_action_" + actionType + "_" + appointmentId +
                "_" + appointment.getAppointmentDate().toLocalDate() + "_" + appointment.getChatId());

        // Кнопка отказа
        InlineKeyboardButton noButton = new InlineKeyboardButton(
                "ru".equals(languageCode)
                        ? "Нет, оставить"
                        : "uk".equals(languageCode)
                        ? "Ні, залишити"
                        : "No, keep"
        );
        noButton.setCallbackData("/admin_select_date_" + actionType + "_" + appointment.getChatId()
                + "_" + appointment.getAppointmentDate().toLocalDate());

        keyboard.setKeyboard(List.of(List.of(confirmButton, noButton)));

        // Сохраняем состояния в зависимости от действия
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/admin_confirm_action_" + actionType + "_" + appointmentId);
        userSession.setPreviousState(chatId, "/admin_select_time_" + actionType + "_" + appointmentId);

        // Отправляем сообщение
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }


    public void adminChooseNewsData(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = "ru".equals(languageCode)
                    ? "Запись не найдена."
                    : "uk".equals(languageCode)
                    ? "Запис не знайдено."
                    : "Appointment not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

// Сохраняем идентификатор записи в сессии пользователя, чтобы использовать его на следующих шагах
        userSession.setAppointmentToTransfer(chatId, appointmentId);

// Запрашиваем выбор новой даты
        String selectDateMessage = "ru".equals(languageCode)
                ? "Пожалуйста, выберите новую дату для записи:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть нову дату для запису:"
                : "Please select a new date for the appointment:";
        messageService.sendMessage(chatId, selectDateMessage);

// Показать доступные даты для этого мастера
        showAvailableDatesAdmin(chatId, appointment.getMaster().getId());
    }

    // Метод для отображения доступных дат
    public void showAvailableDatesAdmin(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        List<AvailableDate> availableDates = availableDateService.getAvailableDatesForMaster(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        availableDates = availableDates.stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now()))  // Исключаем прошедшие и сегодняшние даты
                .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId())
                        .stream().anyMatch(slot -> !slot.isBooked()))  // Оставляем только даты с доступными временными слотами
                .sorted(Comparator.comparing(AvailableDate::getDate))  // Сортируем по возрастанию даты
                .collect(Collectors.toList());

        if (availableDates.isEmpty()) {
            String message = "ru".equals(languageCode)
                    ? "Нет доступных дат для выбранного мастера."
                    : "uk".equals(languageCode)
                    ? "Немає доступних дат для обраного майстра."
                    : "No available dates for the selected master.";
            messageService.sendMessage(chatId, message);
            return;
        }

        for (AvailableDate date : availableDates) {
            InlineKeyboardButton dateButton = new InlineKeyboardButton(date.getDate().toString());
            dateButton.setCallbackData("/admin_select_transfer_date_" + date.getId());
            rows.add(List.of(dateButton));
        }

        keyboard.setKeyboard(rows);

        String selectDateMessage = "ru".equals(languageCode)
                ? "Выберите дату:"
                : "uk".equals(languageCode)
                ? "Оберіть дату:"
                : "Select a date:";
        messageService.sendMessageWithInlineKeyboard(chatId, selectDateMessage, keyboard);
    }

    public void handleTransferDateSelectionAdmin(Long chatId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        userSession.setSelectedDate(chatId, dateId.toString());

        List<TimeSlot> timeSlots = availableDateService.getTimeSlotsForAvailableDate(dateId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (TimeSlot slot : timeSlots) {
            if (!slot.isBooked()) { // Отображаем только доступные слоты
                InlineKeyboardButton timeButton = new InlineKeyboardButton(slot.getTime().toString());
                timeButton.setCallbackData("/admin_select_transfer_time_" + slot.getId() + "_" + dateId);
                rows.add(List.of(timeButton));
            }
        }

        keyboard.setKeyboard(rows);
        String selectTimeMessage = "ru".equals(languageCode)
                ? "Выберите время:"
                : "uk".equals(languageCode)
                ? "Оберіть час:"
                : "Select a time:";
        messageService.sendMessageWithInlineKeyboard(chatId, selectTimeMessage, keyboard);
    }

    // Метод для обработки выбора времени и окончательного подтверждения переноса
    public void handleTransferTimeSelection(Long chatId, Long timeSlotId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Long appointmentId = userSession.getAppointmentToTransfer(chatId);
        if (appointmentId == null) {
            String message = "ru".equals(languageCode)
                    ? "Запись для переноса не выбрана."
                    : "uk".equals(languageCode)
                    ? "Запис для перенесення не обрано."
                    : "No appointment selected for transfer.";
            messageService.sendMessage(chatId, message);
            return;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        TimeSlot newTimeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);

        if (appointment == null || newTimeSlot == null) {
            String message = "ru".equals(languageCode)
                    ? "Произошла ошибка при переносе. Пожалуйста, попробуйте снова."
                    : "uk".equals(languageCode)
                    ? "Сталася помилка під час перенесення. Будь ласка, спробуйте ще раз."
                    : "An error occurred during transfer. Please try again.";
            messageService.sendMessage(chatId, message);
            return;
        }

// Сохраняем выбранный timeSlotId в сессии пользователя
        userSession.setSelectedTimeSlot(chatId, String.valueOf(timeSlotId));

// Подтверждение переноса
        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы переносите запись для клиента" + appointment.getUsers().getFirstName() +
                " " + appointment.getUsers().getLastName() + " на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nВремя: " + newTimeSlot.getTime() + "\n\nПодтвердить перенос?"
                : "uk".equals(languageCode)
                ? "Ви переносите запис для клієнту" + appointment.getUsers().getFirstName() +
                " " + appointment.getUsers().getLastName() + " на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nЧас: " + newTimeSlot.getTime() + "\n\nПідтвердити перенесення?"
                : "You are transferring the appointment for client's" + appointment.getUsers().getFirstName() +
                " " + appointment.getUsers().getLastName() + "to:\nDate: " + newTimeSlot.getAvailableDate().getDate() + "\nTime: " + newTimeSlot.getTime() + "\n\nConfirm transfer?";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Подтвердить перенос" : "uk".equals(languageCode) ? "Підтвердити перенесення" : "Confirm Transfer"
        );
        confirmButton.setCallbackData("/admin_transfer_final_" + appointmentId + "_" + dateId);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Отменить перенос" : "uk".equals(languageCode) ? "Скасувати перенесення" : "Cancel Transfer"
        );
        cancelButton.setCallbackData("/reschedule_appointment");

        List<InlineKeyboardButton> row = List.of(confirmButton, cancelButton);
        keyboard.setKeyboard(List.of(row));

        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }

    // Метод для окончательного переноса
    public void finalizeTransfer(Long chatId, Long appointmentId, Long timeSlotId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        TimeSlot newTimeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);

        if (appointment == null || newTimeSlot == null) {
            String message = "ru".equals(languageCode)
                    ? "Перенос не удался. Пожалуйста, попробуйте снова."
                    : "uk".equals(languageCode)
                    ? "Перенесення не вдалося. Будь ласка, спробуйте ще раз."
                    : "Transfer failed. Please try again.";
            messageService.sendMessage(chatId, message);
            return;
        }

        // Получаем дату и время из текущей записи (старое время)
        LocalTime oldTime = appointment.getAppointmentDate().toLocalTime();

        // Находим доступную дату для старой записи
        AvailableDate oldAvailableDate = availableDateRepository.findByDateAndMasterId(appointment.getAppointmentDate().toLocalDate(), appointment.getMaster().getId()).orElse(null);
        if (oldAvailableDate != null) {
            // Находим старый временной слот и помечаем его как доступный
            List<TimeSlot> oldTimeSlots = timeSlotRepository.findByAvailableDateAndTime(oldAvailableDate, oldTime);
            TimeSlot oldTimeSlot = oldTimeSlots.isEmpty() ? null : oldTimeSlots.get(0);

            if (oldTimeSlot != null) {
                oldTimeSlot.setBooked(false);
                timeSlotRepository.save(oldTimeSlot);
            }
        }

        // Обновляем дату и время в Appointment на основе нового слота
        appointment.setAppointmentDate(LocalDateTime.of(newTimeSlot.getAvailableDate().getDate(), newTimeSlot.getTime()));
        appointmentRepository.save(appointment);

        // Помечаем новый слот как забронированный
        newTimeSlot.setBooked(true);
        timeSlotRepository.save(newTimeSlot);

        // Уведомляем клиента о переносе
        String userNotification = "ru".equals(languageCode)
                ? "Ваша запись на услугу " + appointment.getServices().getNameRu() +
                " перенесена на:\nДата: " + newTimeSlot.getAvailableDate().getDate() +
                "\nВремя: " + newTimeSlot.getTime() + "."
                : "uk".equals(languageCode)
                ? "Ваш запис на послугу " + appointment.getServices().getNameUk() +
                " перенесено на:\nДата: " + newTimeSlot.getAvailableDate().getDate() +
                "\nЧас: " + newTimeSlot.getTime() + "."
                : "Your appointment for " + appointment.getServices().getNameEn() +
                " has been rescheduled to:\nDate: " + newTimeSlot.getAvailableDate().getDate() +
                "\nTime: " + newTimeSlot.getTime() + ".";
        messageService.sendMessage(chatId, userNotification);

        // Уведомляем мастера о переносе
        Long masterChatId = appointment.getMaster().getChatId(); // Предполагаем, что у мастера есть поле с ID чата
        if (masterChatId != null) {
            // Получаем информацию о пользователе
            Users users = userRepository.findByChatId(chatId);
            String clientName = (users != null) ? users.getFirstName() + " " + users.getLastName() : "Unknown";

            String languageCodeMaster = userRepository.findLanguageCodeByChatId(masterChatId);
            String masterNotification = "ru".equals(languageCodeMaster)
                    ? "Запись для клиента " + clientName +
                    " на услугу " + appointment.getServices().getNameRu() +
                    " перенесена на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nВремя: " + newTimeSlot.getTime() + "."
                    : "uk".equals(languageCodeMaster)
                    ? "Запис для клієнта " + clientName +
                    " на послугу " + appointment.getServices().getNameUk() +
                    " перенесено на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nЧас: " + newTimeSlot.getTime() + "."
                    : "The appointment for " + appointment.getServices().getNameEn() +
                    " with client " + clientName +
                    " has been rescheduled to:\nDate: " + newTimeSlot.getAvailableDate().getDate() + "\nTime: " + newTimeSlot.getTime() + ".";

            messageService.sendMessage(masterChatId, masterNotification);
        }

        // Сообщение администратору
        String adminMessage = "ru".equals(languageCode)
                ? "Запись успешно перенесена."
                : "uk".equals(languageCode)
                ? "Запис успішно перенесено."
                : "Appointment successfully transferred.";
        messageService.sendMessageWithInlineKeyboard(chatId, adminMessage, adminButtons.getManageAppointmentsKeyboard(chatId));
    }

    public void cancelAppointment(Long chatId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        Long clientChatId = appointment.getChatId();

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String languageCodeMaster = userRepository.findLanguageCodeByChatId(appointment.getMaster().getChatId());
        String languageCodeClient = userRepository.findLanguageCodeByChatId(clientChatId);

        if (appointment == null) {
            String message = "ru".equals(languageCode)
                    ? "Запись не найдена."
                    : "uk".equals(languageCode)
                    ? "Запис не знайдено."
                    : "Appointment not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        // Устанавливаем статус CANCELLED
        appointment.setStatus(Appointment.Status.CANCELLED);
        appointmentRepository.save(appointment);

        TimeSlot timeSlot = timeSlotRepository.findByTimeAndMasterId(
                appointment.getAppointmentDate().toLocalTime(),
                appointment.getMaster().getId()
        );
        if (timeSlot != null) {
            timeSlot.setBooked(false);
            timeSlotRepository.save(timeSlot);
        }

        // Сообщение об успешной отмене для клиента
        String clientSuccessMessage = "ru".equals(languageCodeClient)
                ? "Ваша запись на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + " была успешно отменена."
                : "uk".equals(languageCodeClient)
                ? "Ваш запис на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + " був успішно скасований."
                : "Your appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + " has been successfully canceled.";
        messageService.sendMessage(clientChatId, clientSuccessMessage);

        // Уведомление для мастера
        Long masterChatId = appointment.getMaster().getChatId();
        if (masterChatId != null) {
            Users client = userRepository.findByChatId(chatId);
            String clientName = client != null ? client.getFirstName() + " " + client.getLastName() : "Unknown";
            String appointmentDate = appointment.getAppointmentDate().toLocalDate().toString();
            String appointmentTime = appointment.getAppointmentDate().toLocalTime().toString();

            String masterNotification = "ru".equals(languageCodeMaster)
                    ? "Клиент " + clientName + " отменил запись на " + appointmentDate + " в " + appointmentTime + "."
                    : "uk".equals(languageCodeMaster)
                    ? "Клієнт " + clientName + " скасував запис на " + appointmentDate + " о " + appointmentTime + "."
                    : "Client " + clientName + " has canceled the appointment on " + appointmentDate + " at " + appointmentTime + ".";

            messageService.sendMessage(masterChatId, masterNotification);
        }

        String successMessage = "ru".equals(languageCode)
                ? "Запись успешно отменена."
                : "uk".equals(languageCode)
                ? "Запис успішно скасовано."
                : "Appointment successfully cancelled.";
        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getManageAppointmentsKeyboard(chatId));
    }

    protected void showMasterSelection(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<Master> masters = masterRepository.findAll();
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Master master : masters) {
            List<AvailableDate> availableDates = availableDateService.getAvailableDatesForMaster(master.getId());

            availableDates = availableDates.stream()
                    .filter(date -> date.getDate().isAfter(LocalDate.now()))  // Исключаем прошлое и сегодняшнюю дату
                    .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId()).stream().anyMatch(slot -> !slot.isBooked()))  // Оставляем только даты с доступными временными слотами
                    .collect(Collectors.toList());

            // Если нет свободных дат, отправляем сообщение с номером телефона мастера
            if (availableDates.isEmpty()) {
                String message = "ru".equals(languageCode)
                        ? "У мастера " + master.getName() + " в настоящее время нет доступных дат. Пожалуйста, свяжитесь с ним по телефону: " + master.getPhoneNumber()
                        : "uk".equals(languageCode)
                        ? "У майстра " + master.getName() + " зараз немає доступних дат. Будь ласка, зв'яжіться з ним за телефоном: " + master.getPhoneNumber()
                        : "Master " + master.getName() + " currently has no available dates. Please contact them at: " + master.getPhoneNumber();
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/appointments_manage");
                userSession.setPreviousState(chatId, "/other_actions");
                userSession.clearSession(chatId);
                userSession.clearTempData(chatId);

                messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getManageAppointmentsKeyboard(chatId));
                return;
            }

            InlineKeyboardButton button = new InlineKeyboardButton(master.getName());
            button.setCallbackData("/admin_select_master_" + master.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Выберите мастера:" :
                "uk".equals(languageCode) ? "Оберіть майстра:" :
                        "Choose a master:", keyboard);
    }

    protected void showServiceSelection(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<Services> services = serviceRepository.findByMasterId(masterId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            InlineKeyboardButton button = new InlineKeyboardButton("ru".equals(languageCode) ? service.getNameRu() :
                    "uk".equals(languageCode) ? service.getNameUk() :
                            service.getNameEn());
            button.setCallbackData("/admin_service_" + service.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Выберите услугу:" :
                "uk".equals(languageCode) ? "Оберіть послугу:" :
                        "Choose a service:", keyboard);
    }

    protected void showDateSelection(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<AvailableDate> dates = availableDateRepository.findByMasterId(masterId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Фильтруем даты, оставляя только те, где есть незанятые временные слоты
        dates = dates.stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now()))  // Исключаем прошлое и сегодняшнюю дату
                .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId()).stream().anyMatch(slot -> !slot.isBooked()))  // Оставляем только даты с доступными временными слотами
                .collect(Collectors.toList());

        if (dates.isEmpty()) {
            String message = "ru".equals(languageCode)
                    ? "Нет доступных дат для выбранного мастера."
                    : "uk".equals(languageCode)
                    ? "Немає доступних дат для обраного майстра."
                    : "No available dates for the selected master.";

            messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getManageAppointmentsKeyboard(chatId));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/appointments_manage");
            userSession.setPreviousState(chatId, "/other_actions");
            userSession.clearSession(chatId);
            userSession.clearTempData(chatId);
            return;
        }

        for (AvailableDate date : dates) {
            InlineKeyboardButton button = new InlineKeyboardButton(date.getDate().toString());
            button.setCallbackData("/admin_date_" + date.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Выберите дату:" :
                "uk".equals(languageCode) ? "Оберіть дату:" :
                        "Choose a date:", keyboard);
    }

    protected void showTimeSelection(Long chatId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<TimeSlot> slots = availableDateService.getTimeSlotsForAvailableDate(dateId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = slots.stream()
                .filter(slot -> !slot.isBooked())
                .map(slot -> {
                    InlineKeyboardButton button = new InlineKeyboardButton(slot.getTime().toString());
                    userSession.setSelectedTimeSlot(chatId, String.valueOf(slot.getId()));
                    button.setCallbackData("/admin_time_" + slot.getId());
                    return List.of(button);
                }).collect(Collectors.toList());

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите время:"
                : "uk".equals(languageCode)
                ? "Оберіть час:"
                : "Select a time:";

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void finalizeAppointment(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = Long.parseLong(userSession.getTempData(chatId, "userId"));
        Long masterId = Long.parseLong(userSession.getTempData(chatId, "masterId"));
        Long serviceId = Long.parseLong(userSession.getTempData(chatId, "serviceId"));
        Long timeSlotId = Long.parseLong(userSession.getTempData(chatId, "timeSlotId"));
        Long userChatId = userRepository.findById(userId).get().getChatId();

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);
        if (timeSlot == null) {
            messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Ошибка: неверное время." :
                    "uk".equals(languageCode) ? "Помилка: невірний час." :
                            "Error: invalid time.", adminButtons.getManageAppointmentsKeyboard(chatId));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/appointments_manage");
            userSession.setPreviousState(chatId, "/other_actions");
            userSession.clearSession(chatId);
            userSession.clearTempData(chatId);
            return;
        }

        Appointment appointment = new Appointment();
        appointment.setUsers(userRepository.findById(userId).orElse(null));
        appointment.setMaster(masterRepository.findById(masterId).orElse(null));
        appointment.setServices(serviceRepository.findById(serviceId).orElse(null));
        appointment.setAppointmentDate(LocalDateTime.of(timeSlot.getAvailableDate().getDate(), timeSlot.getTime()));
        appointment.setStatus(Appointment.Status.CONFIRMED);
        if (userChatId != null) {
            appointment.setChatId(userChatId);
        }

        appointmentRepository.save(appointment);
        timeSlot.setBooked(true);
        timeSlotRepository.save(timeSlot);

        // Сообщение об успешной отмене для клиента
        if (userChatId != null) {
            String languageCodeClient = userRepository.findLanguageCodeByChatId(userChatId);
            String clientSuccessMessage = "ru".equals(languageCodeClient)
                    ? "Ваша запись на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + " была успешно создана."
                    : "uk".equals(languageCodeClient)
                    ? "Ваш запис на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + " був успішно створено."
                    : "Your appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + " has been successfully booked.";
            messageService.sendMessage(userChatId, clientSuccessMessage);
        }

        // Уведомление для мастера
        Long masterChatId = masterRepository.findById(masterId).get().getChatId();
        String languageCodeMaster = userRepository.findLanguageCodeByChatId(masterChatId);
        if (masterChatId != null) {
            Users client = userRepository.findByChatId(chatId);
            String clientName = client != null ? client.getFirstName() + " " + client.getLastName() : "Unknown";
            String appointmentDate = appointment.getAppointmentDate().toLocalDate().toString();
            String appointmentTime = appointment.getAppointmentDate().toLocalTime().toString();
            String serviceName = serviceRepository.findById(serviceId)
                    .map(service -> {
                        if ("ru".equals(languageCode)) {
                            return service.getNameRu();
                        } else if ("uk".equals(languageCode)) {
                            return service.getNameUk();
                        } else {
                            return service.getNameEn();
                        }
                    })
                    .orElse("Unknown");


            String masterNotification = "ru".equals(languageCodeMaster)
                    ? "Новая запись на прием:\n" +
                    "Клиент: " + clientName + "\n" +
                    "Услуга: " + serviceName + "\n" +
                    "Дата: " + appointmentDate + "\n" +
                    "Время: " + appointmentTime
                    : "uk".equals(languageCodeMaster)
                    ? "Нове бронювання:\n" +
                    "Клієнт: " + clientName + "\n" +
                    "Послуга: " + serviceName + "\n" +
                    "Дата: " + appointmentDate + "\n" +
                    "Час: " + appointmentTime
                    : "New appointment booked:\n" +
                    "Client: " + clientName + "\n" +
                    "Service: " + serviceName + "\n" +
                    "Date: " + appointmentDate + "\n" +
                    "Time: " + appointmentTime;
            ;

            messageService.sendMessage(masterChatId, masterNotification);
        }

        messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Запись успешно создана!" :
                "uk".equals(languageCode) ? "Запис успішно створено!" :
                        "Appointment successfully created!", adminButtons.getManageAppointmentsKeyboard(chatId));
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/appointments_manage");
        userSession.setPreviousState(chatId, "/other_actions");
        userSession.clearSession(chatId);
        userSession.clearTempData(chatId);
    }
}


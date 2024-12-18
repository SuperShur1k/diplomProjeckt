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
    private ServiceRepository serviceRepository;

    public void showAdminPanel(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Локализуем сообщение
        messageService.sendLocalizedMessageWithInlineKeyboard(
                chatId,
                "admin.panel.welcome", // Ключ локализации
                languageCode,
                adminButtons.getAdminInlineKeyboard(chatId)
        );

        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/admin");
        userSession.setPreviousState(chatId, "/main_menu");
    }

    public void initiateSetAdmin(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя

        // Локализуем сообщение с ключами из файла messages.properties
        messageService.sendLocalizedMessage(chatId, "set.admin.enter.phone", languageCode);
        userSession.setSettingAdmin(chatId, true);

        // Второе сообщение с кнопкой отмены
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                "set.admin.cancel.operation",
                languageCode,
                autUserButtons.getCancelInlineKeyboard(chatId));
    }

    public void setAdmin(Long chatId, String phone) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Находим пользователя по номеру телефона
        Users users = userRepository.findByPhoneNumber(phone);
        if (users == null) {
            // Локализованное сообщение "Пользователь не найден"
            messageService.sendLocalizedMessageWithInlineKeyboard(
                    chatId,
                    "admin.user.not.found",
                    languageCode,
                    adminButtons.getAdminInlineKeyboard(chatId));
            userSession.setSettingAdmin(chatId, false); // Сбрасываем состояние назначения администратора
        } else {
            // Назначаем роль администратора
            users.setRole(Users.Role.ADMIN);
            log.info("Saving user with role: {}", users.getRole());
            userRepository.save(users);

            // Локализованное сообщение "Пользователь успешно получил права администратора"
            messageService.sendLocalizedMessageWithInlineKeyboard(
                    chatId,
                    "admin.user.successfully.granted",
                    languageCode,
                    adminButtons.getAdminInlineKeyboard(chatId),
                    users.getFirstName(), users.getLastName());
            userSession.setSettingAdmin(chatId, false); // Сбрасываем состояние назначения администратора
        }
    }

    public void initiateDelAdmin(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список всех администраторов
        List<Users> admins = userRepository.findAll().stream()
                .filter(user -> user.getRole().equals(Users.Role.ADMIN))
                .collect(Collectors.toList());

        if (admins.isEmpty()) {
            messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                    "admin.remove.no.admins", languageCode,
                    adminButtons.getAdminInlineKeyboard(chatId));
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
        cancelButton.setText(messageService.getLocalizedMessage("button.cancel", languageCode));
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        keyboard.setKeyboard(rows);

        messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                "admin.remove.choose", languageCode, keyboard);
    }

    public void removeAdminById(Long chatId, Long adminId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Users admin = userRepository.findById(adminId).orElse(null);
        if (admin == null || !admin.getRole().equals(Users.Role.ADMIN)) {
            messageService.sendLocalizedMessageWithInlineKeyboard(
                    chatId,
                    "admin.not.found.or.removed",
                    languageCode,
                    adminButtons.getAdminInlineKeyboard(chatId)
            );
            return;
        }

        admin.setRole(Users.Role.CLIENT);
        userRepository.save(admin);

        messageService.sendLocalizedMessageWithInlineKeyboard(
                chatId,
                "admin.role.removed.success",
                languageCode,
                adminButtons.getAdminInlineKeyboard(chatId),
                admin.getFirstName(), admin.getLastName()
        );
    }

    public void takeAnswerToHelp(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Открытые"
        InlineKeyboardButton openRequestsButton = new InlineKeyboardButton();
        openRequestsButton.setText(messageService.getLocalizedMessage("requests.open", languageCode));
        openRequestsButton.setCallbackData("/open_requests");

        // Кнопка "В процессе"
        InlineKeyboardButton inProgressRequestsButton = new InlineKeyboardButton();
        inProgressRequestsButton.setText(messageService.getLocalizedMessage("requests.in.progress", languageCode));
        inProgressRequestsButton.setCallbackData("/progress_requests");

        // Кнопка "Завершенные"
        InlineKeyboardButton closedRequestsButton = new InlineKeyboardButton();
        closedRequestsButton.setText(messageService.getLocalizedMessage("requests.closed", languageCode));
        closedRequestsButton.setCallbackData("/closed_requests");

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");

        rows.add(List.of(openRequestsButton));
        rows.add(List.of(inProgressRequestsButton));
        rows.add(List.of(closedRequestsButton));
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "requests.choose.category", languageCode, keyboard);
    }

    public void openRequest(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        List<Help> openRequests = helpRepository.findByStatus(Help.HelpStatus.WAIT);

        if (openRequests.isEmpty()) {
            messageService.sendLocalizedMessage(chatId,
                    "open.requests.none",
                    languageCode);
            return;
        }

        // Сообщение с выбором даты запроса
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Help help : openRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString()); // Только дата
            button.setCallbackData("/view_open_" + help.getId());
            rows.add(List.of(button));
        }

        // Добавляем кнопку "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("button.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем локализованное сообщение
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                "open.requests.select.date",
                languageCode,
                keyboard);
    }

    public void viewOpenRequest(Long chatId, Long requestId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Help request not found"));

        // Локализованное сообщение
        String message = messageService.getLocalizedMessage(
                "help.request",
                languageCode,
                help.getHelpQuestion()
        );

        // Создаем inline-клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Дать ответ"
        InlineKeyboardButton answerButton = new InlineKeyboardButton();
        answerButton.setText(messageService.getLocalizedMessage("button.answer", languageCode));
        answerButton.setCallbackData("/answer_request_" + requestId);
        rows.add(List.of(answerButton));

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("button.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "help.request", languageCode, keyboard, help.getHelpQuestion());
    }

    public void initialAnswerRequest(Long chatId, Long requestId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Сохраняем состояние для обработки ввода ответа
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/waiting_answer_" + requestId);

        // Локализованное сообщение для администратора
        messageService.sendLocalizedMessage(chatId,
                "admin.enter.response",
                languageCode);
    }

    public void answerRequest(Long chatId, Long requestId, String answer) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Находим запрос помощи
        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getLocalizedMessage("help.request.not.found", languageCode, requestId)));

        // Устанавливаем данные администратора, ответ и статус
        help.setAdmin(userRepository.findByChatId(chatId));
        help.setAdminResponse(answer);
        help.setStatus(Help.HelpStatus.OPEN);
        helpRepository.save(help);

        // Отправляем подтверждение админу о том, что ответ сохранен
        String confirmationMessage = messageService.getLocalizedMessage("admin.response.saved", languageCode);
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
            String noRequestsMessage = messageService.getLocalizedMessage("requests.in.progress.none", languageCode);
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Help help : inProgressRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString());
            button.setCallbackData("/in_progress_" + help.getId());
            rows.add(List.of(button));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "requests.in.progress.select.date", languageCode, keyboard);
    }

    public void progressRequest(Long chatId, Long requestId) {
        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getLocalizedMessage("help.request.not.found", userRepository.findLanguageCodeByChatId(chatId), requestId)));

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Используем ваш метод для отправки сообщения с инлайн-клавиатурой
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "user.request", languageCode, keyboard, help.getHelpQuestion(), help.getAdminResponse());
    }

    public void initialClosedRequest(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = userRepository.findByChatId(chatId).getId();

        List<Help> closedRequests = helpRepository.findByAdmin_IdAndStatus(userId, Help.HelpStatus.CLOSED);

        if (closedRequests.isEmpty()) {
            String noRequestsMessage = messageService.getLocalizedMessage("requests.closed.none", languageCode);
            messageService.sendMessage(chatId, noRequestsMessage);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Help help : closedRequests) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(help.getCreatedAt().toLocalDate().toString());
            button.setCallbackData("/request_closed_" + help.getId());
            rows.add(List.of(button));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Используем ваш метод для отправки сообщения с инлайн-клавиатурой
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "requests.closed.select.date", languageCode, keyboard);
    }

    public void closeRequest(Long chatId, Long requestId) {
        Help help = helpRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getLocalizedMessage("help.request.not.found", userRepository.findLanguageCodeByChatId(chatId), requestId)));

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Формируем сообщение с локализацией
        String message = messageService.getLocalizedMessage("user.request", languageCode, help.getHelpQuestion(), help.getAdminResponse());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        // Используем ваш метод для отправки сообщения с инлайн-клавишами
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "user.request", languageCode, keyboard, help.getHelpQuestion(), help.getAdminResponse());
    }

    private void chooseRecipient(Long chatId, String recipientType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Локализованное сообщение для выбора получателя (мастера или пользователя)
        String messageKey = recipientType.equals("master") ? "choose.master" : "choose.user";
        String message = messageService.getLocalizedMessage(messageKey, languageCode);

        List<?> recipients = recipientType.equals("master")
                ? masterRepository.findAllByStatus(Master.Status.ACTIVE)
                : userRepository.findAll();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для каждого получателя (мастера или пользователя)
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
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с инлайн-клавишами
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, messageKey, languageCode, keyboard);
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

        // Подготавливаем ключ для локализованного сообщения в зависимости от типа получателя
        String messageKey = recipientType.equals("master") ? "write.message.master" : "write.message.user";

        // Получаем локализованное сообщение
        String message = messageService.getLocalizedMessage(messageKey, languageCode);

        // Устанавливаем состояние ожидания ввода сообщения
        userSession.setCurrentState(chatId, recipientType.equals("master")
                ? "/writing_to_master_" + recipientChatId
                : "/writing_to_user_" + recipientChatId);

        // Отправляем локализованное сообщение
        messageService.sendMessage(chatId, message);
    }

    public void writeToRecipient(Long senderChatId, Long recipientChatId, String messageText, String recipientType) {
        String senderLanguageCode = userRepository.findLanguageCodeByChatId(senderChatId);
        String recipientLanguageCode = userRepository.findLanguageCodeByChatId(recipientChatId);

        // Формируем локализованное сообщение для получателя
        String messageKey = recipientType.equals("master") ? "admin.wrote.master" : "admin.wrote.user";

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText(messageService.getLocalizedMessage(
                recipientType.equals("master") ? "reply.master" : "reply.user", recipientLanguageCode));
        replyButton.setCallbackData(recipientType.equals("master")
                ? "/reply_to_admin_master_" + senderChatId
                : "/reply_to_admin_user_" + senderChatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Отправляем локализованное сообщение получателю с инлайн-клавишами
        messageService.sendLocalizedMessageWithInlineKeyboard(recipientChatId, messageKey, recipientLanguageCode, keyboard, messageText);

        // Подтверждаем отправителю
        String confirmationMessage = messageService.getLocalizedMessage(
                recipientType.equals("master") ? "message.sent.master" : "message.sent.user", senderLanguageCode);
        messageService.sendMessage(senderChatId, confirmationMessage);

        // Очищаем состояние отправителя
        userSession.clearStates(senderChatId);
        userSession.setCurrentState(senderChatId, recipientType.equals("master") ? "/write_to_master" : "/write_to_client");
        userSession.setPreviousState(senderChatId, "/other_actions");

        // Возвращаем пользователя к соответствующему состоянию
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
            String messageKey = "TRANSFER".equals(actionType) ? "no.clients.transfer" : "no.clients.cancel";
            String message = messageService.getLocalizedMessage(messageKey, languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для каждого клиента
        for (Users client : clientsWithAppointments) {
            InlineKeyboardButton clientButton = new InlineKeyboardButton(client.getFirstName() + " " + client.getLastName());
            clientButton.setCallbackData("/admin_select_client_" + actionType + "_" + client.getChatId());
            rows.add(List.of(clientButton));
        }

        // Добавляем кнопку "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Текст сообщения в зависимости от действия
        String selectClientMessageKey = "TRANSFER".equals(actionType) ? "select.client.transfer" : "select.client.cancel";

        // Отправляем сообщение с клавишами
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, selectClientMessageKey, languageCode, keyboard);
    }

    public void adminChooseDateForClient(Long chatId, Long clientChatId, String actionType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        List<Appointment> appointments = appointmentRepository.findByChatIdAndStatus(clientChatId, Appointment.Status.CONFIRMED);

        if (appointments.isEmpty()) {
            String messageKey = "TRANSFER".equals(actionType) ? "no.appointments.transfer" : "no.appointments.cancel";
            String message = messageService.getLocalizedMessage(messageKey, languageCode);
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

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Формируем текст сообщения для выбора даты
        String selectDateMessageKey = "TRANSFER".equals(actionType) ? "select.date.transfer" : "select.date.cancel";

        // Отправляем сообщение с инлайн-клавишами
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, selectDateMessageKey, languageCode, keyboard);
    }

    public void adminChooseTimeForDate(Long chatId, Long clientChatId, LocalDate date, String actionType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        List<Appointment> appointments = appointmentRepository.findByChatIdAndStatus(clientChatId, Appointment.Status.CONFIRMED).stream()
                .filter(app -> app.getAppointmentDate().toLocalDate().equals(date))
                .sorted(Comparator.comparing(app -> app.getAppointmentDate().toLocalTime()))
                .collect(Collectors.toList());

        if (appointments.isEmpty()) {
            String messageKey = "TRANSFER".equals(actionType) ? "no.time.slots.transfer" : "no.time.slots.cancel";
            String message = messageService.getLocalizedMessage(messageKey, languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для каждого временного слота
        for (Appointment appointment : appointments) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton(appointment.getAppointmentDate().toLocalTime().toString());
            timeButton.setCallbackData("/admin_select_time_" + actionType + "_" + appointment.getId() + "_" + date + "_" + clientChatId);
            rows.add(List.of(timeButton));
        }

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("requests.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Текст сообщения для выбора времени
        String selectTimeMessageKey = "TRANSFER".equals(actionType) ? "select.time.transfer" : "select.time.cancel";

        // Отправляем сообщение с инлайн-клавишами
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, selectTimeMessageKey, languageCode, keyboard);
    }

    public void adminConfirmAction(Long chatId, Long appointmentId, String actionType) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем запись по ID
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String messageKey = "appointment.not.found";
            String message = messageService.getLocalizedMessage(messageKey, languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Формируем сообщение подтверждения в зависимости от типа действия
        String confirmationMessageKey = "TRANSFER".equals(actionType)
                ? "confirm.transfer"
                : "confirm.cancel";

        // Формируем клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        // Кнопка подтверждения
        String confirmButtonLabelKey = "TRANSFER".equals(actionType)
                ? "confirm.transfer.yes"
                : "confirm.cancel.yes";
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage(confirmButtonLabelKey, languageCode)
        );
        confirmButton.setCallbackData("/admin_confirm_action_" + actionType + "_" + appointmentId +
                "_" + appointment.getAppointmentDate().toLocalDate() + "_" + appointment.getChatId());

        // Кнопка отказа
        String noButtonLabelKey = "confirm.no";
        InlineKeyboardButton noButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage(noButtonLabelKey, languageCode)
        );
        noButton.setCallbackData("/admin_select_date_" + actionType + "_" + appointment.getChatId()
                + "_" + appointment.getAppointmentDate().toLocalDate());

        keyboard.setKeyboard(List.of(List.of(confirmButton, noButton)));

        // Сохраняем состояния в зависимости от действия
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/admin_confirm_action_" + actionType + "_" + appointmentId);
        userSession.setPreviousState(chatId, "/admin_select_time_" + actionType + "_" + appointmentId);

        // Отправляем сообщение с клавишами
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, confirmationMessageKey, languageCode, keyboard, appointment.getAppointmentDate().toLocalDate(), appointment.getAppointmentDate().toLocalTime());
    }

    public void adminChooseNewsData(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment.not.found", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем идентификатор записи в сессии пользователя, чтобы использовать его на следующих шагах
        userSession.setAppointmentToTransfer(chatId, appointmentId);

        // Запрашиваем выбор новой даты
        String selectDateMessage = messageService.getLocalizedMessage("select.new.date", languageCode);
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
            String message = messageService.getLocalizedMessage("no.available.dates", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        for (AvailableDate date : availableDates) {
            InlineKeyboardButton dateButton = new InlineKeyboardButton(date.getDate().toString());
            dateButton.setCallbackData("/admin_select_transfer_date_" + date.getId());
            rows.add(List.of(dateButton));
        }

        keyboard.setKeyboard(rows);

        // Сообщение для выбора даты
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "select.date", languageCode, keyboard);
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

        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "select.time", languageCode, keyboard);
    }

    // Метод для обработки выбора времени и окончательного подтверждения переноса
    public void handleTransferTimeSelection(Long chatId, Long timeSlotId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Long appointmentId = userSession.getAppointmentToTransfer(chatId);
        if (appointmentId == null) {
            String message = messageService.getLocalizedMessage("appointment.not.selected.for.transfer", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        TimeSlot newTimeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);

        if (appointment == null || newTimeSlot == null) {
            String message = messageService.getLocalizedMessage("error.transfer.failed", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем выбранный timeSlotId в сессии пользователя
        userSession.setSelectedTimeSlot(chatId, String.valueOf(timeSlotId));

        // Подтверждение переноса
        String confirmationMessage = messageService.getLocalizedMessage("transfer.confirm", languageCode,
                appointment.getUsers().getFirstName(), appointment.getUsers().getLastName(),
                newTimeSlot.getAvailableDate().getDate(), newTimeSlot.getTime());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(messageService.getLocalizedMessage("confirm.transfer.button", languageCode));
        confirmButton.setCallbackData("/admin_transfer_final_" + appointmentId + "_" + dateId);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton(messageService.getLocalizedMessage("cancel.transfer.button", languageCode));
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
            String message = messageService.getLocalizedMessage("transfer.failed", languageCode);
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

        // Получаем локализованное имя услуги в зависимости от языка
        String serviceName = getServiceNameByLanguage(appointment.getServices(), languageCode);

        // Уведомляем клиента о переносе
        String userNotification = messageService.getLocalizedMessage("appointment.rescheduled", languageCode, serviceName,
                newTimeSlot.getAvailableDate().getDate(), newTimeSlot.getTime());
        messageService.sendMessage(chatId, userNotification);

        // Уведомляем мастера о переносе
        Long masterChatId = appointment.getMaster().getChatId();
        if (masterChatId != null) {
            Users client = userRepository.findByChatId(chatId);
            String clientName = (client != null) ? client.getFirstName() + " " + client.getLastName() : "Unknown";

            String masterNotification = messageService.getLocalizedMessage("appointment.rescheduled.master", languageCode,
                    clientName, serviceName, newTimeSlot.getAvailableDate().getDate(), newTimeSlot.getTime());
            messageService.sendMessage(masterChatId, masterNotification);
        }

        // Сообщение администратору
        String adminMessage = messageService.getLocalizedMessage("appointment.successfully.transferred", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, adminMessage, adminButtons.getManageAppointmentsKeyboard(chatId));
    }

    // Метод для получения локализованного имени услуги
    private String getServiceNameByLanguage(Services service, String languageCode) {
        switch (languageCode) {
            case "ru":
                return service.getNameRu();
            case "uk":
                return service.getNameUk();
            case "en":
            default:
                return service.getNameEn();
        }
    }

    public void cancelAppointment(Long chatId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        Long clientChatId = appointment.getChatId();

        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String languageCodeMaster = userRepository.findLanguageCodeByChatId(appointment.getMaster().getChatId());
        String languageCodeClient = userRepository.findLanguageCodeByChatId(clientChatId);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment.not.found", languageCode);
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
        String clientSuccessMessage = messageService.getLocalizedMessage("appointment.canceled.client", languageCodeClient,
                appointment.getAppointmentDate().toLocalDate(), appointment.getAppointmentDate().toLocalTime());
        messageService.sendMessage(clientChatId, clientSuccessMessage);

        // Уведомление для мастера
        Long masterChatId = appointment.getMaster().getChatId();
        if (masterChatId != null) {
            Users client = userRepository.findByChatId(chatId);
            String clientName = client != null ? client.getFirstName() + " " + client.getLastName() : "Unknown";
            String appointmentDate = appointment.getAppointmentDate().toLocalDate().toString();
            String appointmentTime = appointment.getAppointmentDate().toLocalTime().toString();

            String masterNotification = messageService.getLocalizedMessage("appointment.canceled.master", languageCodeMaster,
                    clientName, appointmentDate, appointmentTime);
            messageService.sendMessage(masterChatId, masterNotification);
        }

        String successMessage = messageService.getLocalizedMessage("appointment.canceled.success", languageCode);
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
                    .filter(date -> date.getDate().isAfter(LocalDate.now()))  // Exclude past and today’s dates
                    .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId()).stream().anyMatch(slot -> !slot.isBooked()))  // Keep only dates with available slots
                    .collect(Collectors.toList());

            // If there are no available dates, send a message with the master’s phone number
            if (availableDates.isEmpty()) {
                String message = messageService.getLocalizedMessage(languageCode, "no.available.dates", master.getName(), master.getPhoneNumber());
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/appointments_manage");
                userSession.setPreviousState(chatId, "/other_actions");
                userSession.clearSession(chatId);
                userSession.clearTempData(chatId);

                messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getManageAppointmentsKeyboard(chatId));
                return;
            }

            // Create a button for the master
            InlineKeyboardButton button = new InlineKeyboardButton(master.getName());
            button.setCallbackData("/admin_select_master_" + master.getId());
            rows.add(List.of(button));
        }

        // Send the list of masters
        keyboard.setKeyboard(rows);
        String message = messageService.getLocalizedMessage(languageCode, "master.choose");
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }


    protected void showServiceSelection(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<Services> services = serviceRepository.findByMasterId(masterId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Get the localized service name based on the language code
            String serviceName = messageService.getLocalizedServiceName(service, languageCode);

            InlineKeyboardButton button = new InlineKeyboardButton(serviceName);
            button.setCallbackData("/admin_service_" + service.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);

        // Get the localized message for selecting a service
        String message = messageService.getLocalizedMessage(languageCode, "choose.service");

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void showDateSelection(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<AvailableDate> dates = availableDateRepository.findByMasterId(masterId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Filter available dates (those with free time slots and after today's date)
        dates = dates.stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now())) // Exclude past and today
                .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId())
                        .stream().anyMatch(slot -> !slot.isBooked())) // Keep only dates with available time slots
                .collect(Collectors.toList());

        if (dates.isEmpty()) {
            // Use a helper method for localized message
            String message = messageService.getLocalizedMessage(languageCode, "no.available.dates.for.master");
            messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getManageAppointmentsKeyboard(chatId));

            // Clear session data and set current state
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/appointments_manage");
            userSession.setPreviousState(chatId, "/other_actions");
            userSession.clearSession(chatId);
            userSession.clearTempData(chatId);
            return;
        }

        // Create a button for each available date
        for (AvailableDate date : dates) {
            InlineKeyboardButton button = new InlineKeyboardButton(date.getDate().toString());
            button.setCallbackData("/admin_date_" + date.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);

        // Use the helper method to get the localized message for choosing a date
        String message = messageService.getLocalizedMessage(languageCode, "select.date");
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    protected void showTimeSelection(Long chatId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<TimeSlot> slots = availableDateService.getTimeSlotsForAvailableDate(dateId);

        // Create keyboard for available time slots
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = slots.stream()
                .filter(slot -> !slot.isBooked()) // Only show available time slots
                .map(slot -> {
                    InlineKeyboardButton button = new InlineKeyboardButton(slot.getTime().toString());
                    userSession.setSelectedTimeSlot(chatId, String.valueOf(slot.getId())); // Save selected slot in session
                    button.setCallbackData("/admin_time_" + slot.getId());
                    return List.of(button); // Wrap the button in a list (to match the keyboard format)
                })
                .collect(Collectors.toList());

        keyboard.setKeyboard(rows);

        // Use helper method to get localized message for time selection
        String message = messageService.getLocalizedMessage(languageCode, "select.time");
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
            messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "error.invalid_time", languageCode, adminButtons.getManageAppointmentsKeyboard(chatId));
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

        // Сообщение об успешной записи для клиента
        if (userChatId != null) {
            sendClientSuccessMessage(userChatId, appointment, languageCode);
        }

        // Уведомление для мастера
        sendMasterNotification(masterId, appointment, languageCode);

        // Сообщение для администратора
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "appointment.successfully_created", languageCode, adminButtons.getManageAppointmentsKeyboard(chatId));

        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/appointments_manage");
        userSession.setPreviousState(chatId, "/other_actions");
        userSession.clearSession(chatId);
        userSession.clearTempData(chatId);
    }

    private void sendClientSuccessMessage(Long userChatId, Appointment appointment, String languageCode) {
        String clientSuccessMessage = messageService.getLocalizedMessage("appointment.booked_successfully", languageCode, appointment.getAppointmentDate().toLocalDate(), appointment.getAppointmentDate().toLocalTime());
        messageService.sendMessage(userChatId, clientSuccessMessage);
    }

    private void sendMasterNotification(Long masterId, Appointment appointment, String languageCode) {
        Long masterChatId = masterRepository.findById(masterId).get().getChatId();
        Users client = userRepository.findByChatId(appointment.getChatId());
        String clientName = client != null ? client.getFirstName() + " " + client.getLastName() : "Unknown";
        String appointmentDate = appointment.getAppointmentDate().toLocalDate().toString();
        String appointmentTime = appointment.getAppointmentDate().toLocalTime().toString();
        String serviceName = serviceRepository.findById(appointment.getServices().getId())
                .map(service -> messageService.getLocalizedServiceName(service, languageCode))
                .orElse("Unknown");

        String masterNotification = messageService.getLocalizedMessage("master.new_appointment", languageCode, clientName, serviceName, appointmentDate, appointmentTime);
        messageService.sendMessage(masterChatId, masterNotification);
    }
}


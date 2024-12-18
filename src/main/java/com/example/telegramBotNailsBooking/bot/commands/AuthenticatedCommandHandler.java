package com.example.telegramBotNailsBooking.bot.commands;

import com.example.telegramBotNailsBooking.model.Appointment;
import com.example.telegramBotNailsBooking.model.Services;
import com.example.telegramBotNailsBooking.model.UserSession;
import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.repository.AppointmentRepository;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import com.example.telegramBotNailsBooking.repository.ServiceRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.*;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import com.example.telegramBotNailsBooking.service.buttons.MenuService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthenticatedCommandHandler extends UserService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedCommandHandler.class);

    @Autowired
    private UserSession userSession;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AutUserButtons autUserButtons;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceManagementService serviceManagementService;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private MasterService masterService;
    @Autowired
    private AppointmentRepository appointmentRepository;

    public void handleAuthenticatedCommand(Long chatId, String text, Update update) {

        logger.info("Received command: {} for chat ID {}", text, chatId);
        String currentState = userSession.getCurrentState(chatId);
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);



        if (text.equals("/master_record_client")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master_record_client");
            userSession.setPreviousState(chatId, "/master");

            // Локализованная кнопка "Отмена"
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                    messageService.getLocalizedMessage("button.cancel", languageCode)
            );
            cancelButton.setCallbackData("/master_cancel_appointment_master");
            rows.add(List.of(cancelButton));
            keyboard.setKeyboard(rows);

            // Отправка сообщения с кнопкой
            messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "client.phone.prompt", languageCode, keyboard);
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/master_record_client")) {
            String phoneNumber = text.trim();
            Users existingUser = userRepository.findByPhoneNumber(phoneNumber);

            if (existingUser != null) {
                // Клиент найден
                userSession.setCurrentState(chatId, "/master_select_master");
                userSession.setTempData(chatId, "userId", String.valueOf(existingUser.getId()));

                // Локализованное сообщение для найденного клиента
                messageService.sendLocalizedMessage(chatId, "client.found.choose.service", languageCode);
                masterService.showServiceSelection(chatId);
            } else {
                // Клиент не найден
                userSession.setCurrentState(chatId, "/master_add_new_client");
                userSession.setTempData(chatId, "phone", phoneNumber);

                // Локализованная кнопка "Отмена"
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                        messageService.getLocalizedMessage("button.cancel", languageCode)
                );
                cancelButton.setCallbackData("/master_cancel_appointment_master");
                rows.add(List.of(cancelButton));
                keyboard.setKeyboard(rows);

                // Отправка сообщения с кнопкой
                messageService.sendLocalizedMessageWithInlineKeyboard(chatId, "client.not.found.enter.name", languageCode, keyboard);
            }
            return;
        }

// Обработка команды "Отмена"
        if (text.equals("/master_cancel_appointment_master")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master");

            // Локализованное сообщение об отмене процесса записи
            messageService.sendLocalizedMessageWithInlineKeyboard(
                    chatId,
                    "appointment.process.canceled", // ключ для локализации
                    languageCode,
                    autUserButtons.masterPanel(chatId)
            );
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/master_add_new_client")) {
            String firstName = text.trim();
            userSession.setTempData(chatId, "firstName", firstName);

            userSession.setCurrentState(chatId, "/master_add_new_client_last_name");

            // Локализованное сообщение
            messageService.sendLocalizedMessage(chatId, "client.enter.last.name", languageCode);
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/master_add_new_client_last_name")) {
            String lastName = text.trim();
            String phoneNumber = userSession.getTempData(chatId, "phone");

            Users newUser = new Users();
            newUser.setPhoneNumber(phoneNumber);
            newUser.setFirstName(userSession.getTempData(chatId, "firstName"));
            newUser.setLastName(lastName);
            newUser.setLanguage("en"); // Язык по умолчанию
            newUser.setRole(Users.Role.CLIENT);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(newUser);

            userSession.setCurrentState(chatId, "/master_select_master");
            userSession.setTempData(chatId, "userId", String.valueOf(newUser.getId()));

            // Локализованное сообщение
            messageService.sendLocalizedMessage(chatId, "client.added.choose.service", languageCode);
            masterService.showServiceSelection(chatId);
            return;
        }

        if (text.startsWith("/master_service_")) {
            Long serviceId = Long.parseLong(text.replace("/master_service_", ""));
            userSession.setTempData(chatId, "serviceId", String.valueOf(serviceId));
            userSession.setCurrentState(chatId, "/master_date");

            masterService.showDateSelection(chatId);
            return;
        }

        if (text.startsWith("/master_date_master_")) {
            Long dateId = Long.parseLong(text.replace("/master_date_master_", ""));
            userSession.setTempData(chatId, "dateId", String.valueOf(dateId));
            userSession.setCurrentState(chatId, "/master_time");

            masterService.showTimeSelection(chatId, dateId);
            return;
        }

        if (text.startsWith("/master_time_master_")) {
            Long timeSlotId = Long.parseLong(text.replace("/master_time_master_", ""));
            userSession.setTempData(chatId, "timeSlotId", String.valueOf(timeSlotId));

            masterService.finalizeAppointment(chatId);
            return;
        }

        if (text.equals("/master") && masterRepository.existsByChatId(chatId)) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master");
            userSession.setPreviousState(chatId, "/main_menu");

            // Локализованное сообщение
            messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                    "master.panel.welcome",
                    languageCode,
                    autUserButtons.masterPanel(chatId));
            return;
        }

        if (text.equals("/view_appointments") && masterRepository.existsByChatId(chatId)) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/view_appointments");
            userSession.setPreviousState(chatId, "/master");

            masterService.initialCheckAppointments(chatId);
            return;
        }

        if (text.startsWith("/appointments_for_date_") && masterRepository.existsByChatId(chatId)) {
            LocalDate date = LocalDate.parse(text.replace("/appointments_for_date_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/view_appointments");

            masterService.timeCheckAppointments(chatId, date);
            return;
        }

        if (text.startsWith("/master_appointment_details_") && masterRepository.existsByChatId(chatId)) {
            String[] parts = text.replace("/master_appointment_details_", "").split("_");
            LocalDate date = LocalDate.parse(parts[0]);
            LocalTime time = LocalTime.parse(parts[1]);
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/appointments_for_date_" + date);

            masterService.showInfoAppointments(chatId, date, time);
            return;
        }

        if (text.startsWith("/master_cancel_appointment_") && masterRepository.existsByChatId(chatId)) {
            Long appointmentId = Long.parseLong(text.replace("/master_cancel_appointment_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/cancel_appointment_" + appointmentId);
            userSession.setPreviousState(chatId, "/view_appointments");

            masterService.cancelAppointment(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/master_reschedule_appointment_") && masterRepository.existsByChatId(chatId)) {
            Long appointmentId = Long.parseLong(text.replace("/master_reschedule_appointment_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master_reschedule_appointment_" + appointmentId);
            userSession.setPreviousState(chatId, "/view_appointments");

            masterService.selectTransferDate(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/master_select_transfer_date_")){
            String[] parts = text.replace("/master_select_transfer_date_", "").split("_");
            Long dateId = Long.parseLong(parts[0]);
            Long appointmentId = Long.parseLong(parts[1]);
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master_select_transfer_date_" + dateId);
            userSession.setPreviousState(chatId, "/view_appointments");
            masterService.selectTransferTime(chatId, dateId, appointmentId);
            return;
        }

        if (text.startsWith("/master_select_transfer_time_")){
            String[] parts = text.replace("/master_select_transfer_time_", "").split("_");
            Long slotId = Long.parseLong(parts[0]);
            Long appointmentId = Long.parseLong(parts[1]);
            Long dateId = Long.parseLong(parts[2]);
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master_select_transfer_time_" + slotId);
            userSession.setPreviousState(chatId, "/view_appointments");

            masterService.finalizeTransfer(chatId, appointmentId, slotId, dateId);
            return;
        }

        if (text.equals("/master_write_client")){
            masterService.initialWriteToClient(chatId);
            return;
        }

        if (text.startsWith("/message_client_")){
            Long clientChatId = Long.parseLong(text.replace("/message_client_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/message_client_" + clientChatId);
            userSession.setPreviousState(chatId, "/view_appointments");

            masterService.writeToClient(chatId, clientChatId);
            return;
        }

        if (currentState.startsWith("/master_write_to_client_")){
            Long clientChatId = Long.parseLong(currentState.replace("/master_write_to_client_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master_write_to_client_" + clientChatId);
            userSession.setPreviousState(chatId, "/view_appointments");
            masterService.finishWriteToClient(chatId, clientChatId, text);
            return;
        }

        if (text.startsWith("/reply_to_master_")){
            Long masterChatId = Long.parseLong(text.replace("/reply_to_master_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/reply_to_master_" + masterChatId);
            userSession.setPreviousState(chatId, "/main_menu");

            initialWriteToMaster(chatId, masterChatId);
            return;
        }

        if (currentState.startsWith("/writing_to_master_from_user_")){
            Long masterChatId = Long.parseLong(currentState.replace("/writing_to_master_from_user_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/writing_to_master_from_user_" + masterChatId);
            userSession.setPreviousState(chatId, "/main_menu");

            writeToMaster(chatId, masterChatId, text);
            return;
        }

        if (currentState == (null)) {
            currentState = "/main_menu"; // Устанавливаем значение по умолчанию
            userSession.setCurrentState(chatId, currentState);
        }

        if (text.startsWith("/show_reviews_")) {
            Long masterId = Long.parseLong(text.split("_")[2]);
            reviewService.showLastFiveReviewsForMaster(chatId, masterId);
            return;
        }

        if (text.startsWith("/select_master_")) {
            Long masterId = Long.parseLong(text.split("_")[2]);

            serviceManagementService.showServicesForSelectedMaster(chatId, masterId);
            return;
        }

        if (text.startsWith("/description_")) {
            Long serviceId = Long.parseLong(text.split("_")[1]);
            Services service = serviceRepository.findById(serviceId).orElse(null);

            if (service != null) {
                String serviceName = "ru".equals(languageCode)
                        ? service.getNameRu()
                        : "uk".equals(languageCode)
                        ? service.getNameUk()
                        : service.getNameEn();

                String serviceDescription = "ru".equals(languageCode)
                        ? service.getDescriptionRu()
                        : "uk".equals(languageCode)
                        ? service.getDescriptionUk()
                        : service.getDescriptionEn();

                messageService.sendMessage(chatId, serviceName + ": " + serviceDescription);
            } else {
                messageService.sendLocalizedMessage(chatId,
                        "service.description.not.found",
                        languageCode);
            }
            return;
        }

        if (currentState.startsWith("/awaiting_phone_")) {
            String phoneNumber = update.getMessage().getText();
            String language = currentState.split("_")[2];

            if (isValidPhoneNumber(phoneNumber)) {
                String[] userInfo = userSession.getUserInfo(chatId);

                // Проверяем, есть ли номер телефона уже в базе
                if (userRepository.existsByPhoneNumber(phoneNumber)) {
                    messageService.sendLocalizedMessage(chatId,
                            "error.phone.already.registered",
                            language);
                    return;
                } else {
                    // Создаем и сохраняем нового пользователя
                    Users user = createUser(chatId, userInfo, phoneNumber, language);
                    userRepository.save(user);

                    sendSuccessMessage(chatId, language);
                    userSession.clearStates(chatId);
                    return;
                }
            } else {
                // Номер телефона некорректный
                messageService.sendLocalizedMessage(chatId,
                        "error.phone.invalid",
                        language);
                return;
            }
        }

        if (currentState.startsWith("/awaiting_first_name_")) {
            String language = currentState.split("_")[3];
            processFirstName(chatId, language, update);
            return;
        }

        if (currentState.startsWith("/awaiting_last_name_")) {
            String language = currentState.split("_")[3];
            processLastName(chatId, language, update);
            return;
        }

        if (currentState.equals("/select_master")) {
            handleAuthenticatedCommand(chatId, "/select_master", update);
            return;
        }

        if (text.startsWith("/select_date_")) {
            // Извлекаем ID мастера из коллбэка
            String masterId = text.split("_")[2];
            userSession.setSelectedMaster(chatId, masterId); // Сохраняем ID мастера после нажатия на кнопку

            // Переход к выбору даты
            appointmentService.selectDate(chatId);
            return;
        }

        if (text.startsWith("/appointment_details_")) {
            Long appointmentId = Long.parseLong(text.split("_")[2]);
            appointmentService.showBookingDetails(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/select_service_")) {
            Long serviceId = Long.parseLong(text.split("_")[2]);
            appointmentService.handleServiceSelection(chatId, serviceId);
            return;
        }

        if (text.startsWith("/show_description_")) {
            Long serviceId = Long.parseLong(text.split("_")[2]);
            Services service = serviceRepository.findById(serviceId).orElse(null);

            if (service != null) {
                String serviceName = "ru".equals(languageCode)
                        ? service.getNameRu()
                        : "uk".equals(languageCode)
                        ? service.getNameUk()
                        : service.getNameEn();

                String serviceDescription = "ru".equals(languageCode)
                        ? service.getDescriptionRu()
                        : "uk".equals(languageCode)
                        ? service.getDescriptionUk()
                        : service.getDescriptionEn();

                messageService.sendMessage(chatId, serviceName + ": " + serviceDescription);
            } else {
                messageService.sendLocalizedMessage(chatId,
                        "error.service.description.not.found",
                        languageCode);
            }
            return;
        }

        if (text.startsWith("/show_times_")) {
            try {
                // Извлекаем дату и статус из команды, предполагая формат: "/show_times_YYYY-MM-DD_STATUS"
                String[] parts = text.replace("/show_times_", "").split("_");

                if (parts.length < 2) {
                    messageService.sendLocalizedMessage(chatId,
                            "error.command.invalid.format",
                            languageCode);
                    return;
                }

                LocalDate date = LocalDate.parse(parts[0]); // Получаем дату
                Appointment.Status status = Appointment.Status.valueOf(parts[1]); // Получаем статус

                // Вызываем метод для отображения слотов времени на указанную дату и статус
                appointmentService.showTimeSlotsForDate(chatId, date, status);
                return;
            } catch (DateTimeParseException e) {
                messageService.sendLocalizedMessage(chatId,
                        "error.date.invalid.format",
                        languageCode);
                return;
            } catch (IllegalArgumentException e) {
                messageService.sendLocalizedMessage(chatId,
                        "error.status.invalid.format",
                        languageCode);
                return;
            }
        }

        if (text.startsWith("/confirm_cancel_")) {
            Long appointmentId = Long.parseLong(text.split("_")[2]);
            userSession.setCurrentState(chatId, "/confirm_cancel_" + appointmentId);
            appointmentService.confirmCancelAppointment(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/confirm_transfer_")) {
            Long appointmentId = Long.parseLong(text.split("_")[2]);
            userSession.setCurrentState(chatId, "/confirm_transfer_" + appointmentId);
            appointmentService.confirmTransferAppointment(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/cancel_appointment_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_cancel_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                appointmentService.cancelAppointment(chatId, appointmentId);
                return;
            } else {
                messageService.sendLocalizedMessage(chatId,
                        "appointment.cancel.prompt",
                        languageCode);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
        }

        if (text.startsWith("/transfer_appointment_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_transfer_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                appointmentService.transferAppointment(chatId, appointmentId);
                return;
            } else {
                messageService.sendLocalizedMessage(chatId,
                        "appointment.transfer.select.date.time",
                        languageCode);

                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
        }

        if (text.startsWith("/select_transfer_date_")) {
            Long appointmentId = Long.parseLong(text.split("_")[3]);
            appointmentService.handleTransferDateSelection(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/select_transfer_time_")) {
            Long appointmentId = Long.parseLong(text.split("_")[3]);
            appointmentService.handleTransferTimeSelection(chatId, appointmentId);
            return;
        }

        if (text.equals("/keep_appointment")) {

            if (userSession.getCurrentState(chatId).startsWith("/confirm_transfer_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);

                messageService.sendLocalizedMessage(chatId, "appointment.not.transferred", languageCode);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_cancel_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);

                messageService.sendLocalizedMessage(chatId, "appointment.not.canceled", languageCode);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_delete_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);

                messageService.sendLocalizedMessage(chatId, "appointment.not.deleted", languageCode);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_review_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);

                messageService.sendLocalizedMessage(chatId, "review.waiting.future", languageCode);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
            return;
        }

        if (text.startsWith("/transfer_final_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_transfer_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                Long timeSlotId = Long.valueOf(userSession.getSelectedTimeSlot(chatId));

                appointmentService.finalizeTransfer(chatId, appointmentId, timeSlotId);
                return;
            } else {
                // Использование локализации
                messageService.sendLocalizedMessage(chatId,
                        "appointment.transfer.choose.datetime",
                        languageCode);

                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
        }

        if (text.startsWith("/confirm_delete_")) {
            Long appointmentId = Long.parseLong(text.split("_")[2]);
            userSession.setCurrentState(chatId, "/confirm_delete_" + appointmentId);

            appointmentService.confirmDeleteAppointment(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/delete_appointment_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_delete_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);

                appointmentService.deleteAppointment(chatId, appointmentId);
                return;
            } else {
                messageService.sendLocalizedMessage(chatId,
                        "appointment.delete.select.date.time",
                        languageCode);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
        }

        if (text.startsWith("/confirm_review_")) {
            Long appointmentId = Long.parseLong(text.split("_")[2]);
            userSession.setCurrentState(chatId, "/confirm_review_" + appointmentId);

            reviewService.clientFeedback(chatId, appointmentId);
            return;
        }

        if (text.startsWith("/review_appointment_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_review_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                reviewService.askMarkAndFeedback(chatId, appointmentId);
                return;
            } else {
                messageService.sendLocalizedMessage(chatId,
                        "review.appointment.choose.date.time",
                        languageCode);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
        }

        if (userSession.getCurrentState(chatId).equals("/waiting_for_rating_")) {
            int rating = Integer.parseInt(text);
            reviewService.requestComment(chatId, rating);
            return;
        }

        if (userSession.getCurrentState(chatId).startsWith("/waiting_for_comment_")) {
            Integer rating = Math.toIntExact(Long.parseLong(userSession.getCurrentState(chatId).split("_")[3]));
            String comment = text;
            Long appointmentId = userSession.getRequestingFeedback(chatId);

            reviewService.handleCommentResponse(chatId, rating, comment, appointmentId);
            return;
        }

        if (text.startsWith("/consent_yes_") && userSession.getCurrentState(chatId).equals("/start")) {
            String language = text.split("_")[2];
            register(chatId, language, update);
            return;
        }

        if (text.startsWith("/consent_no_") && userSession.getCurrentState(chatId).equals("/start")) {
            String language = text.split("_")[2];
            personalData(chatId, language);
            return;
        }

        if (text.equals("/list_commands")) {
            listCommands(chatId);
            return;
        }

        if (text.equals("/contact_admin")) {
            contactAdmin(chatId);
            return;
        }

        if (text.equals("/ask_new_question")) {
            askNewQuestion(chatId);
            return;
        }

        if (text.equals("/view_requests")) {
            viewRequests(chatId);
            return;
        }

        if (text.equals("/view_open_requests")) {
            handleOpenRequests(chatId);
            return;
        }

        if (text.startsWith("/view_request_")) {
            Long helpID = Long.parseLong(text.split("_")[2]);

            handleViewRequest(chatId, helpID);
            return;
        }

        if (text.equals("/view_in_progress_requests")) {
            handleInProgressRequests(chatId);
            return;
        }

        if (text.startsWith("/progress_request_")) {
            Long helpID = Long.parseLong(text.split("_")[2]);

            handleViewInProgressRequest(chatId, helpID);
            return;
        }

        if (text.startsWith("/close_request_")) {
            Long helpID = Long.parseLong(text.split("_")[2]);

            handleCloseRequest(chatId, helpID);
            return;
        }

        if (text.startsWith("/new_question_")) {
            Long helpID = Long.parseLong(text.split("_")[2]);

            handleAskNewQuestion(chatId, helpID);
            return;
        }

        if (text.equals("/view_closed_requests")) {
            handleClosedRequests(chatId);
            return;
        }

        if (text.startsWith("/closed_request_")) {
            Long helpID = Long.parseLong(text.split("_")[2]);

            handleViewClosedRequest(chatId, helpID);
            return;
        }

        if (text.startsWith("/delete_request_")) {
            Long helpID = Long.parseLong(text.split("_")[2]);

            deleteRequests(chatId, helpID);
            return;
        }

        if (text.equals("/contact_master")) {
            masterRequests(chatId);
            return;
        }

        if (text.startsWith("/contact_master_")) {
            Long masterID = Long.parseLong(text.split("_")[2]);
            masterContactRequests(chatId, masterID);
            return;
        }

        if (text.equals("/change_name")) {
            initialChangeNameNLastName(chatId);
            return;
        }

        if (text.equals("/change_first_name")) {
            changeFirstName(chatId);
            return;
        }

        if (currentState.equals("/waiting_for_first_name")) {
            handleNewFirstName(chatId, text);
            return;
        }

        if (text.startsWith("/confirm_change_first_name_")) {
            String newFirstName = text.replace("/confirm_change_first_name_", "");
            confirmChangeFirstName(chatId, newFirstName);
            return;
        }

        if (text.equals("/cancel_change_first_name")) {
            cancelChangeFirstName(chatId);
            return;
        }

        if (text.equals("/change_last_name")) {
            changeLastName(chatId);
            return;
        }

        if (currentState.equals("/waiting_for_last_name")) {
            handleNewLastName(chatId, text);
            return;
        }

        if (text.startsWith("/confirm_change_last_name_")) {
            String newLastName = text.replace("/confirm_change_last_name_", "");
            confirmChangeLastName(chatId, newLastName);
            return;
        }

        if (text.equals("/cancel_change_last_name")) {
            cancelChangeLastName(chatId);
            return;
        }

        if (text.equals("/change_language")) {
            initialChangeLanguage(chatId);
            return;
        }

        if (text.equals("/change_phone")) {
            initialChangePhoneNumber(chatId);
            return;
        }

        if (currentState.equals("/waiting_for_phone_number")) {
            handleNewPhoneNumber(chatId, text);
            return;
        }

        if (text.startsWith("/confirm_change_phone_number_")) {
            String newPhoneNumber = text.replace("/confirm_change_phone_number_", "");
            confirmChangePhoneNumber(chatId, newPhoneNumber);
            return;
        }

        if (text.equals("/cancel_change_phone_number")) {
            cancelChangePhoneNumber(chatId);
            return;
        }

        if (text.startsWith("/reply_to_admin_master_")) {
            Long adminChatID = Long.valueOf(text.replace("/reply_to_admin_master_", ""));
            masterService.initialWriteToAdmin(chatId, adminChatID);
            return;
        }


        if (text.startsWith("/reply_to_admin_user_")) {
            Long adminChatID = Long.valueOf(text.replace("/reply_to_admin_user_", ""));
            initialWriteToAdmin(chatId, adminChatID);
            return;
        }

        if (currentState.startsWith("/writing_to_admin_from_master_")) {
            // Извлекаем adminChatID из currentState
            Long adminChatID = Long.valueOf(currentState.replace("/writing_to_admin_from_master_", ""));

            // Передаем текст сообщения в метод writeToAdmin
            masterService.writeToAdmin(chatId, adminChatID, text);
            return;
        }

        if (currentState.startsWith("/writing_to_admin_from_user_")) {
            Long adminChatId = Long.valueOf(currentState.replace("/writing_to_admin_from_user_", ""));
            writeToAdmin(chatId, adminChatId, text);
            return;
        }

        switch (text) {
            case "/lang_ru":
                if (currentState.equals("/start")) {
                    messageService.sendLocalizedMessage(chatId, "welcome.message.ru", "ru");
                    personalData(chatId, "ru");
                } else if (currentState.equals("/choose_lang")) {
                    changeLanguage(chatId, "ru");
                } else {
                    messageService.sendLocalizedMessage(chatId, "command.not.recognized", "ru");

                    if (currentState != null) {
                        handleAuthenticatedCommand(chatId, currentState, update);
                    } else {
                        messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                                "previous.state.not.found", "ru", autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                    }
                }
                break;

            case "/lang_en":
                if (currentState.equals("/start")) {
                    messageService.sendLocalizedMessage(chatId, "welcome.message.en", "en");
                    personalData(chatId, "en");
                } else if (currentState.equals("/choose_lang")) {
                    changeLanguage(chatId, "en");
                } else {
                    messageService.sendLocalizedMessage(chatId, "command.not.recognized", "en");

                    if (currentState != null) {
                        handleAuthenticatedCommand(chatId, currentState, update);
                    } else {
                        messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                                "previous.state.not.found", "en", autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                    }
                }
                break;

            case "/lang_uk":
                if (currentState.equals("/start")) {
                    messageService.sendLocalizedMessage(chatId, "welcome.message.uk", "uk");
                    personalData(chatId, "uk");
                } else if (currentState.equals("/choose_lang")) {
                    changeLanguage(chatId, "uk");
                } else {
                    messageService.sendLocalizedMessage(chatId, "command.not.recognized", "uk");

                    if (currentState != null) {
                        handleAuthenticatedCommand(chatId, currentState, update);
                    } else {
                        messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                                "previous.state.not.found", "uk", autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                    }
                }
                break;
            case "/services":
                serviceManagementService.showMasterListForServiceSelection(chatId);
                break;
            case "/review":
                reviewService.showMasterListForReview(chatId);
                break;
            case "/start":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/main_menu");
                startCommand(chatId, update);
                break;
            case "/help":
                userSession.clearStates(chatId);
                userSession.setPreviousState(chatId, "/main_menu");
                userSession.setCurrentState(chatId, "/help");
                initialHelp(chatId);
                break;
            case "/ignore":
                break;
            case "/book_info":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                autUserButtons.showBookingInfoMenu(chatId);
                break;
            case "/book_confirmed":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_confirmed");
                userSession.setPreviousState(chatId, "/book_info");
                appointmentService.showBookingsByStatus(chatId, Appointment.Status.CONFIRMED);
                break;
            case "/book_cancelled":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_cancelled");
                userSession.setPreviousState(chatId, "/book_info");
                appointmentService.showBookingsByStatus(chatId, Appointment.Status.CANCELLED);
                break;
            case "/book_completed":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_completed");
                userSession.setPreviousState(chatId, "/book_info");
                appointmentService.showBookingsByStatus(chatId, Appointment.Status.COMPLETED);
                break;
            case "/confirm_appointment":
                // Proceed with the actual booking creation
                appointmentService.finalizeBooking(chatId);
                break;

            case "/cancel_appointment":
                userSession.clearBookingInfo(chatId);
                messageService.sendLocalizedMessage(chatId,
                        "appointment.canceled",
                        languageCode);
                menuService.bookingManagerButton(chatId);
                break;
            case "/select_master":
                appointmentService.selectMaster(chatId);
                break;

            case "/select_date":
                appointmentService.selectDate(chatId);
                break;

            case "/select_time":
                appointmentService.selectTime(chatId);
                break;

            case "/select_service":
                appointmentService.selectService(chatId);
                break;

            case "/confirm_booking":
                appointmentService.confirmBooking(chatId);
                break;

            case "/main_menu":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/main_menu");
                mainMenu(chatId);
                break;

            case "/back":
                goBack(chatId);
                break;
            case "/menu":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/menu");
                userSession.setPreviousState(chatId, "/main_menu");
                messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                        "menu.title",
                        languageCode,
                        menuService.getMenuInlineKeyboard(chatId));
                break;
            case "/settings":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/settings");
                userSession.setPreviousState(chatId, "/menu");
                menuService.handleSettingsCommand(chatId);
                break;
            case "/cancel":
                cancel(chatId);
                break;
            case "/book_service":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_service");
                userSession.setPreviousState(chatId, "/menu");
                menuService.bookingManagerButton(chatId);
                break;
            case "/book":
                appointmentService.startBooking(chatId);
                messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                        "appointment.start.prompt",
                        languageCode,
                        autUserButtons.getCancelInlineKeyboard(chatId));
                break;
            default:
                if (text.contains("/")) {
                    messageService.sendLocalizedMessage(chatId,
                            "command.not.recognized",
                            languageCode);
                    break;
                }

                if (userSession.getPreviousState(chatId) != null) {
                    // Переходим к предыдущему состоянию
                    handleAuthenticatedCommand(chatId, userSession.getPreviousState(chatId), update);
                    break;
                } else {
                    userSession.clearStates(chatId);
                    userSession.setPreviousState(chatId, "/main_menu");
                    userSession.setCurrentState(chatId, "/main_menu");

                    // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
                    messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                            "previous.state.not.found",
                            languageCode,
                            autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                }
                break;
        }
    }

    protected void cancel(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String currentState = userSession.getCurrentState(chatId); // Получаем предыдущее состояние
        messageService.sendLocalizedMessage(chatId, "operation.cancelled", languageCode);

        if (currentState != null) {
            // Если предыдущее состояние существует, возвращаемся к нему и устанавливаем его как текущее
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, currentState);
            handleAuthenticatedCommand(chatId, currentState, new Update());
        } else {
            // Если предыдущее состояние отсутствует, возвращаемся в главное меню
            userSession.clearSession(chatId);
            mainMenu(chatId);
        }

        // После возврата очищаем текущие данные сессии, чтобы сбросить состояние
        userSession.clearSession(chatId);
    }

    protected void goBack(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем предыдущее состояние пользователя
        String previousState = userSession.getPreviousState(chatId);
        logger.info("Attempting to go back to previous state: {} for chat ID {}", previousState, chatId);

        if (previousState != null) {
            // Переходим к предыдущему состоянию
            handleAuthenticatedCommand(chatId, previousState, new Update());
        } else {
            // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
            messageService.sendLocalizedMessageWithInlineKeyboard(
                    chatId,
                    "previous.state.not.found",
                    languageCode,
                    autUserButtons.getAuthenticatedInlineKeyboard(chatId)
            );
        }
    }

    protected void mainMenu(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/main_menu");
        messageService.sendLocalizedMessageWithInlineKeyboard(chatId,
                "main.menu",
                languageCode,
                autUserButtons.getAuthenticatedInlineKeyboard(chatId));
    }
}

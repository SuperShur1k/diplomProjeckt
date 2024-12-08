package com.example.telegramBotNailsBooking.bot.commands;

import com.example.telegramBotNailsBooking.model.*;
import com.example.telegramBotNailsBooking.repository.AppointmentRepository;
import com.example.telegramBotNailsBooking.repository.ServiceRepository;
import com.example.telegramBotNailsBooking.repository.TimeSlotRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.*;
import com.example.telegramBotNailsBooking.service.buttons.AdminButtons;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import com.example.telegramBotNailsBooking.service.buttons.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminCommandHandler extends AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminCommandHandler.class);

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AutUserButtons autUserButtons;

    @Autowired
    private AdminButtons adminButtons;

    @Autowired
    private UserService userService;

    @Autowired
    private MasterService masterService;

    @Autowired
    private ServiceManagementService serviceManagementService;

    @Autowired
    private AvailableDateService availableDateService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public void handleAdminCommand(Long chatId, String text) {

        logger.info("Received command: {} for chat ID {}", text, chatId);
        String currentState = userSession.getCurrentState(chatId);
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        if (text.startsWith("/select_service_")) {
            Long serviceId = Long.parseLong(text.split("_")[2]);
            appointmentService.handleServiceSelection(chatId, serviceId);
            return;
        }

        if (text.startsWith("/appointment_details_")) {
            Long appointmentId = Long.parseLong(text.split("_")[2]);
            appointmentService.showBookingDetails(chatId, appointmentId);
            return;
        }


        if (currentState == "/select_master") {
            handleAdminCommand(chatId, "/select_master");
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

        if (userSession.isSettingAdmin(chatId)) {
            setAdmin(chatId, text);
            return;
        }

        if (userSession.isSettingMaster(chatId)) {
            masterService.addMaster(chatId, text);
            return;
        }

        if (userSession.getDateInfo(chatId) != null) {
            if (userSession.getPreviousState(chatId) == "/add_date") {
                availableDateService.handleAddDateInput(chatId, text);
                return;
            } else {
                availableDateService.handleDeleteDateInput(chatId, text);
                return;
            }
        }

        if (userSession.getTimeInfo(chatId) != null) {
            if (userSession.getPreviousState(chatId) == "/add_time") {
                availableDateService.handleAddTimeInput(chatId, text);
                return;
            } else {
                availableDateService.handleDeleteTimeInput(chatId, text);
                return;
            }
        }

        if (userSession.getServiceInfo(chatId) != null) {
            if (userSession.getPreviousState(chatId) == "/add_service") {
                serviceManagementService.handleAddService(chatId, text);
                return;
            } else {
                serviceManagementService.handleRemoveService(chatId, text);
                return;
            }
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
                return;
            } else {
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Описание услуги не найдено."
                                : "uk".equals(languageCode)
                                ? "Опис послуги не знайдено."
                                : "Service description not found.");
                return;
            }
        }

        if (text.startsWith("/show_times_")) {
            try {
                // Извлекаем дату и статус из команды, предполагая формат: "/show_times_YYYY-MM-DD_STATUS"
                String[] parts = text.replace("/show_times_", "").split("_");

                if (parts.length < 2) {
                    messageService.sendMessage(chatId,
                            "ru".equals(languageCode)
                                    ? "Неверный формат команды. Пожалуйста, попробуйте снова."
                                    : "uk".equals(languageCode)
                                    ? "Неправильний формат команди. Будь ласка, спробуйте ще раз."
                                    : "Invalid command format. Please try again.");
                    return;
                }

                LocalDate date = LocalDate.parse(parts[0]); // Получаем дату
                Appointment.Status status = Appointment.Status.valueOf(parts[1]); // Получаем статус

                // Вызываем метод для отображения слотов времени на указанную дату и статус
                appointmentService.showTimeSlotsForDate(chatId, date, status);
                return;
            } catch (DateTimeParseException e) {
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Неверный формат даты. Пожалуйста, попробуйте снова."
                                : "uk".equals(languageCode)
                                ? "Неправильний формат дати. Будь ласка, спробуйте ще раз."
                                : "Invalid date format. Please try again.");
                return;
            } catch (IllegalArgumentException e) {
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Неверный формат статуса. Пожалуйста, попробуйте снова."
                                : "uk".equals(languageCode)
                                ? "Неправильний формат статусу. Будь ласка, спробуйте ще раз."
                                : "Invalid status format. Please try again.");
                return;
            }
        }

        if (text.startsWith("/cancel_appointment_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_cancel_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                appointmentService.cancelAppointment(chatId, appointmentId);
                return;
            } else {
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Пожалуйста, выберите дату и время для отмены записи."
                                : "uk".equals(languageCode)
                                ? "Будь ласка, оберіть дату та час для скасування запису."
                                : "Please choose date and time for canceling the appointment.");
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
        }

        if (text.equals("/keep_appointment")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_transfer_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Ваша запись не была перенесена."
                                : "uk".equals(languageCode)
                                ? "Ваш запис не було перенесено."
                                : "Your appointment was not transferred.");
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_cancel_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Ваша запись не была отменена."
                                : "uk".equals(languageCode)
                                ? "Ваш запис не було скасовано."
                                : "Your appointment was not canceled.");
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_delete_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Ваша запись не была удалена."
                                : "uk".equals(languageCode)
                                ? "Ваш запис не було видалено."
                                : "Your appointment was not deleted.");
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_review_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Мы с нетерпением ждем вашего отзыва в будущем."
                                : "uk".equals(languageCode)
                                ? "Ми з нетерпінням чекаємо на ваш відгук у майбутньому."
                                : "We look forward to your review in the future.");
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
            return;
        }

        if (text.startsWith("/transfer_appointment_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_transfer_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                appointmentService.transferAppointment(chatId, appointmentId);
                return;
            } else {
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Пожалуйста, выберите дату и время для переноса записи."
                                : "uk".equals(languageCode)
                                ? "Будь ласка, оберіть дату та час для перенесення запису."
                                : "Please choose a date and time for transferring the appointment.");
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

        if (text.startsWith("/transfer_final_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_transfer_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                Long timeSlotId = Long.valueOf(userSession.getSelectedTimeSlot(chatId));

                appointmentService.finalizeTransfer(chatId, appointmentId, timeSlotId);
                return;
            } else {
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Пожалуйста, выберите дату и время для переноса записи."
                                : "uk".equals(languageCode)
                                ? "Будь ласка, оберіть дату та час для перенесення запису."
                                : "Please choose date and time for transferring the appointment.");
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
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Пожалуйста, выберите дату и время для удаления записи."
                                : "uk".equals(languageCode)
                                ? "Будь ласка, оберіть дату та час для видалення запису."
                                : "Please choose date and time for deleting the appointment.");
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
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Пожалуйста, выберите дату и время для просмотра записи."
                                : "uk".equals(languageCode)
                                ? "Будь ласка, оберіть дату та час для перегляду запису."
                                : "Please choose date and time for reviewing the appointment.");
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            }
        }

        if (userSession.getCurrentState(chatId) == "/waiting_for_rating_") {
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

        if (text.startsWith("/remove_admin_")) {
            Long adminId = Long.parseLong(text.split("_")[2]);
            removeAdminById(chatId, adminId);
            return;
        }

        if (text.startsWith("/manage_master_")) {
            Long masterId = Long.parseLong(text.split("_")[2]);
            masterService.showMasterSettings(chatId, masterId);
            return;
        }

        if (text.startsWith("/set_master_active_")) {
            Long masterId = Long.parseLong(text.split("_")[3]);
            masterService.setMasterStatus(chatId, masterId, Master.Status.ACTIVE);
            return;
        }

        if (text.startsWith("/set_master_inactive_")) {
            Long masterId = Long.parseLong(text.split("_")[3]);
            masterService.setMasterStatus(chatId, masterId, Master.Status.INACTIVE);
            return;
        }

        if (text.startsWith("/delete_master_")) {
            Long masterId = Long.parseLong(text.split("_")[2]);
            masterService.deleteMaster(chatId, masterId);
            return;
        }

        if (currentState.equals("/waiting_for_first_name")) {
            userService.handleNewFirstName(chatId, text);
            return;
        }

        if (currentState.equals("/waiting_for_last_name")) {
            userService.handleNewLastName(chatId, text);
            return;
        }

        if (currentState.equals("/waiting_for_phone_number")) {
            userService.handleNewPhoneNumber(chatId, text);
            return;
        }

        if (text.equals("/reply_to_help")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/reply_to_help");
            userSession.setPreviousState(chatId, "/other_actions");
            takeAnswerToHelp(chatId);
            return;
        }

        if (text.equals("/open_requests")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/open_requests");
            userSession.setPreviousState(chatId, "/reply_to_help");
            openRequest(chatId);
            return;
        }

        if (text.startsWith("/view_open_")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/view_open_");
            userSession.setPreviousState(chatId, "/open_requests");

            Long requestId = Long.parseLong(text.split("_")[2]);

            viewOpenRequest(chatId, requestId);
            return;
        }

        if (currentState.startsWith("/waiting_answer_")) {
            Long answerId = Long.parseLong(currentState.split("_")[2]);
            answerRequest(chatId, answerId, text);
            return;
        }

        if (text.startsWith("/answer_request_")) {
            Long requestId = Long.parseLong(text.split("_")[2]);

            initialAnswerRequest(chatId, requestId);
            return;
        }

        if (text.equals("/progress_requests")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/progress_requests");
            userSession.setPreviousState(chatId, "/reply_to_help");
            initialProgressRequest(chatId);
            return;
        }

        if (text.startsWith("/in_progress_")) {
            Long requestId = Long.parseLong(text.split("_")[2]);
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/in_progress_");
            userSession.setPreviousState(chatId, "/progress_requests");
            progressRequest(chatId, requestId);
            return;
        }

        if (text.equals("/closed_requests")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/closed_requests");
            userSession.setPreviousState(chatId, "/reply_to_help");
            initialClosedRequest(chatId);
            return;
        }

        if (text.startsWith("/request_closed_")) {
            Long requestId = Long.parseLong(text.split("_")[2]);
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/request_closed_");
            userSession.setPreviousState(chatId, "/closed_requests");
            closeRequest(chatId, requestId);
            return;
        }

        if (text.equals("/write_to_master")) {
            chooseWriteToMaster(chatId);
            return;
        }

        if (text.equals("/write_to_client")) {
            chooseWriteToUser(chatId);
            return;
        }

        if (text.startsWith("/write_master_")) {
            Long masterChatId = Long.parseLong(text.replace("/write_master_", ""));
            initialWriteToRecipient(chatId, masterChatId, "master");
            return;
        }

        if (text.startsWith("/write_user_")) {
            Long userChatId = Long.parseLong(text.replace("/write_user_", ""));
            initialWriteToRecipient(chatId, userChatId, "user");
            return;
        }

        if (currentState.startsWith("/writing_to_master_")) {
            Long masterChatId = Long.parseLong(currentState.replace("/writing_to_master_", ""));
            writeToRecipient(chatId, masterChatId, text, "master");
            return;
        }

        if (currentState.startsWith("/writing_to_user_")) {
            Long userChatId = Long.parseLong(currentState.replace("/writing_to_user_", ""));
            writeToRecipient(chatId, userChatId, text, "user");
            return;
        }

        if (text.equals("/reschedule_appointment")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/reschedule_appointment");
            userSession.setPreviousState(chatId, "/appointments_manage");

            adminChooseClient(chatId, "TRANSFER");
            return;
        }

        if (text.equals("/cancel_appointment")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/cancel_appointment");
            userSession.setPreviousState(chatId, "/appointments_manage");

            adminChooseClient(chatId, "CANCEL");
            return;
        }

        if (text.startsWith("/admin_select_client_")) {
            String[] parts = text.replace("/admin_select_client_", "").split("_");

            String actionType = parts[0];
            Long clientChatId = Long.parseLong(parts[1]);

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/reschedule_appointment");

            adminChooseDateForClient(chatId, clientChatId, actionType);
            return;
        }

        if (text.startsWith("/admin_select_date_")) {
            String[] parts = text.replace("/admin_select_date_", "").split("_");

            String actionType = parts[0];
            Long clientChatId = Long.parseLong(parts[1]);
            LocalDate date = LocalDate.parse(parts[2]);

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/admin_select_client_" + actionType + "_" + clientChatId);

            adminChooseTimeForDate(chatId, clientChatId, date, actionType);
            return;
        }

        if (text.startsWith("/admin_select_time_")) {
            String[] parts = text.replace("/admin_select_time_", "").split("_");

            String actionType = parts[0];
            Long appointmentId = Long.parseLong(parts[1]);
            LocalDate date = LocalDate.parse(parts[2]);
            Long clientChatId = Long.parseLong(parts[3]);

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/admin_select_date_" + actionType + "_" + clientChatId + "_" + date);

            adminConfirmAction(chatId, appointmentId, actionType);
            return;
        }

        if (text.startsWith("/admin_confirm_action_")) {
            String[] parts = text.replace("/admin_confirm_action_", "").split("_");
            String actionType = parts[0]; // "TRANSFER" или "CANCEL"
            Long appointmentId = Long.parseLong(parts[1]);
            LocalDate date = LocalDate.parse(parts[2]);
            Long clientChatId = Long.parseLong(parts[3]);

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/admin_select_time_" + actionType + "_" + appointmentId + "_"
                    + date + "_" + clientChatId);

            if ("TRANSFER".equals(actionType)) {
                // Логика переноса
                adminChooseNewsData(chatId, appointmentId);
            } else if ("CANCEL".equals(actionType)) {
                // Логика отмены
                cancelAppointment(chatId, appointmentId);
            }
            return;
        }

        if (text.startsWith("/admin_select_transfer_date_")) {
            Long dateId = Long.parseLong(text.replace("/admin_select_transfer_date_", ""));

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/admin_transfer_appointment_" + userSession.getAppointmentToTransfer(chatId));

            handleTransferDateSelectionAdmin(chatId, dateId);
            return;
        }

        if (text.startsWith("/admin_select_transfer_time_")) {
            String[] parts = text.replace("/admin_select_transfer_time_", "").split("_");
            Long timeSlot = Long.parseLong(parts[0]);
            Long dateId = Long.parseLong(parts[1]);

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/admin_select_transfer_date_" + dateId);

            handleTransferTimeSelection(chatId, timeSlot, dateId);
            return;
        }

        if (text.startsWith("/admin_transfer_final_")) {
            String[] parts = text.replace("/admin_transfer_final_", "").split("_");
            Long appointmentId = Long.parseLong(parts[0]);
            Long date = Long.parseLong(parts[1]);
            Long timeSlotId = Long.valueOf(userSession.getSelectedTimeSlot(chatId));

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/admin_select_transfer_time_" + timeSlotId + "_" + date);

            finalizeTransfer(chatId, appointmentId, timeSlotId);
            return;
        }

        if (text.equals("/add_appointment")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/add_appointment");
            userSession.setPreviousState(chatId, "/other_actions");

            String message = "ru".equals(languageCode)
                    ? "Введите номер телефона клиента:" :
                    "uk".equals(languageCode)
                            ? "Введіть номер телефону клієнта:" :
                            "Enter the client's phone number:";

            // Отправляем сообщение с кнопкой "Отмена"
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                    "ru".equals(languageCode) ? "Отмена" :
                            "uk".equals(languageCode) ? "Скасувати" :
                                    "Cancel"
            );
            cancelButton.setCallbackData("/admin_cancel_appointment");
            rows.add(List.of(cancelButton));
            keyboard.setKeyboard(rows);

            messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/add_appointment")) {
            String phoneNumber = text.trim();
            Users existingUser = userRepository.findByPhoneNumber(phoneNumber);

            if (existingUser != null) {
                // Клиент найден
                userSession.setCurrentState(chatId, "/admin_select_master");
                userSession.setTempData(chatId, "userId", String.valueOf(existingUser.getId()));

                String message = "ru".equals(languageCode)
                        ? "Клиент найден. Выберите мастера для записи:" :
                        "uk".equals(languageCode)
                                ? "Клієнт знайдений. Оберіть майстра для запису:" :
                                "Client found. Choose a master for the appointment:";
                messageService.sendMessage(chatId, message);
                showMasterSelection(chatId);
            } else {
                // Клиент не найден
                userSession.setCurrentState(chatId, "/add_new_client");
                userSession.setTempData(chatId, "phone", phoneNumber);

                String message = "ru".equals(languageCode)
                        ? "Клиент не найден. Введите имя клиента:" :
                        "uk".equals(languageCode)
                                ? "Клієнт не знайдений. Введіть ім'я клієнта:" :
                                "Client not found. Enter the client's first name:";

                // Отправляем сообщение с кнопкой "Отмена"
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                        "ru".equals(languageCode) ? "Отмена" :
                                "uk".equals(languageCode) ? "Скасувати" :
                                        "Cancel"
                );
                cancelButton.setCallbackData("/admin_cancel_appointment");
                rows.add(List.of(cancelButton));
                keyboard.setKeyboard(rows);

                messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
            }
            return;
        }

// Обработка команды "Отмена"
        if (text.equals("/admin_cancel_appointment")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/other_actions");

            String message = "ru".equals(languageCode)
                    ? "Процесс записи отменен." :
                    "uk".equals(languageCode)
                            ? "Процес запису скасовано." :
                            "Appointment process canceled.";

            // Возвращаемся в меню управления записями
            messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getManageAppointmentsKeyboard(chatId));
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/add_new_client")) {
            String firstName = text.trim();
            userSession.setTempData(chatId, "firstName", firstName);

            userSession.setCurrentState(chatId, "/add_new_client_last_name");
            String message = "ru".equals(languageCode)
                    ? "Введите фамилию клиента:" :
                    "uk".equals(languageCode)
                            ? "Введіть прізвище клієнта:" :
                            "Enter the client's last name:";
            messageService.sendMessage(chatId, message);
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/add_new_client_last_name")) {
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

            userSession.setCurrentState(chatId, "/admin_select_master");
            userSession.setTempData(chatId, "userId", String.valueOf(newUser.getId()));

            String message = "ru".equals(languageCode)
                    ? "Новый клиент добавлен" :
                    "uk".equals(languageCode)
                            ? "Новий клієнт доданий." :
                            "New client added.";
            messageService.sendMessage(chatId, message);
            showMasterSelection(chatId);
            return;
        }

        if (text.startsWith("/admin_select_master_")) {
            Long masterId = Long.parseLong(text.replace("/admin_select_master_", ""));
            userSession.setTempData(chatId, "masterId", String.valueOf(masterId));
            userSession.setCurrentState(chatId, "/admin_service");

            showServiceSelection(chatId, masterId);
            return;
        }

        if (text.startsWith("/admin_service_")) {
            Long serviceId = Long.parseLong(text.replace("/admin_service_", ""));
            userSession.setTempData(chatId, "serviceId", String.valueOf(serviceId));
            userSession.setCurrentState(chatId, "/admin_date");

            showDateSelection(chatId, Long.parseLong(userSession.getTempData(chatId, "masterId")));
            return;
        }

        if (text.startsWith("/admin_date_")) {
            Long dateId = Long.parseLong(text.replace("/admin_date_", ""));
            userSession.setTempData(chatId, "dateId", String.valueOf(dateId));
            userSession.setCurrentState(chatId, "/admin_time");

            showTimeSelection(chatId, dateId);
            return;
        }

        if (text.startsWith("/admin_time_")) {
            Long timeSlotId = Long.parseLong(text.replace("/admin_time_", ""));
            userSession.setTempData(chatId, "timeSlotId", String.valueOf(timeSlotId));

            finalizeAppointment(chatId);
            return;
        }

        if (text.startsWith("/change_cost_service_master_")){
            Long masterId = Long.parseLong(text.replace("/change_cost_service_master_", ""));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/change_price");

            serviceManagementService.selectServiceChangeCost(chatId, masterId);
            return;
        }

        if (text.startsWith("/admin_select_service_change_cost_")){
            String[] parts = text.replace("/admin_select_service_change_cost_", "").split("_");
            Long serviceId = Long.parseLong(parts[0]);
            Long masterId = Long.parseLong(parts[1]);

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, text);
            userSession.setPreviousState(chatId, "/change_cost_service_master_" + masterId);
            serviceManagementService.changeServiceCost(chatId, serviceId);
            return;
        }

        if (currentState.equals("/enter_new_service_cost")){
            serviceManagementService.finalChangeCost(chatId, text);
            return;
        }

        switch (text) {
            case "/change_price":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/change_price");
                userSession.setPreviousState(chatId, "/service");
                serviceManagementService.initialChangeCost(chatId);
                break;
            case "/appointments_manage":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/manage_appointments");
                userSession.setPreviousState(chatId, "/other_actions");
                messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode)
                                ? "Выберите действие:"
                                : "uk".equals(languageCode)
                                ? "Оберіть дію:"
                                : "Choose an action:",
                        adminButtons.getManageAppointmentsKeyboard(chatId));
                break;
            case "/other_actions":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/other_actions");
                userSession.setPreviousState(chatId, "/admin");
                messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode)
                                ? "Выберите действие:"
                                : "uk".equals(languageCode)
                                ? "Оберіть дію:"
                                : "Choose an action:",
                        adminButtons.getOtherActionsInlineKeyboard(chatId));
                break;
            case "/manage_masters":
                masterService.showMasterManagementMenu(chatId);
                break;
            case "/del_admin":
                initiateDelAdmin(chatId);
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
            case "/book_service":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_service");
                userSession.setPreviousState(chatId, "/menu");
                menuService.bookingManagerButton(chatId, messageService);
                break;
            case "/book":
                appointmentService.startBooking(chatId);
                messageService.sendMessageWithInlineKeyboard(chatId,
                        "ru".equals(languageCode)
                                ? "Вы можете отменить эту операцию, используя кнопку ниже."
                                : "uk".equals(languageCode)
                                ? "Ви можете скасувати цю операцію, використовуючи кнопку нижче."
                                : "You can cancel this operation using the button below.",
                        autUserButtons.getCancelInlineKeyboard(chatId));
                break;
            case "/confirm_appointment":
                // Proceed with the actual booking creation
                appointmentService.finalizeBooking(chatId);
                break;

            case "/cancel_appointment":
                userSession.clearBookingInfo(chatId);
                messageService.sendMessage(chatId,
                        "ru".equals(languageCode)
                                ? "Бронирование было отменено."
                                : "uk".equals(languageCode)
                                ? "Бронювання було скасовано."
                                : "Booking has been canceled.");
                menuService.bookingManagerButton(chatId, messageService);
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
                mainMenu(chatId);
                break;
            case "/admin":
                userSession.setCurrentState(chatId, "/admin");
                userSession.setPreviousState(chatId, "/main_menu");
                showAdminPanel(chatId);
                break;
            case "/setadmin":
                initiateSetAdmin(chatId);
                break;
            case "/back":
                goBack(chatId);
                break;
            case "/cancel":
                cancel(chatId);
                break;
            case "/add_master":
                masterService.initiateAddMaster(chatId);
                break;
            case "/menu":
                userSession.setCurrentState(chatId, "/menu");
                userSession.setPreviousState(chatId, "/main_menu");
                messageService.sendMessageWithInlineKeyboard(chatId,
                        "ru".equals(languageCode)
                                ? "Вот ваше меню:"
                                : "uk".equals(languageCode)
                                ? "Ось ваше меню:"
                                : "Here is your menu:",
                        menuService.getMenuInlineKeyboard(chatId));
                break;
            case "/settings":
                userSession.setCurrentState(chatId, "/settings");
                userSession.setPreviousState(chatId, "/menu");
                menuService.handleSettingsCommand(chatId, messageService);
                break;
            case "/date":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/date");
                userSession.setPreviousState(chatId, "/admin");
                adminButtons.getDateInlineKeyboard(chatId, messageService);
                break;
            case "/role":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/role");
                userSession.setPreviousState(chatId, "/admin");
                adminButtons.getRoleInlineKeyboard(chatId, messageService);
                break;
            case "/add_date":
                availableDateService.initiateAddDate(chatId);
                break;
            case "/add_time":
                availableDateService.initiateAddTime(chatId);
                break;
            case "/del_date":
                availableDateService.initiateDeleteDate(chatId);
                break;
            case "/del_time":
                availableDateService.initiateDeleteTime(chatId);
                break;
            case "/add_service":
                serviceManagementService.initiateAddService(chatId);
                break;
            case "/del_service":
                serviceManagementService.initiateRemoveService(chatId);
                break;
            case "/service":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/service");
                userSession.setPreviousState(chatId, "/admin");
                adminButtons.getServiceInlineKeyboard(chatId, messageService);
                break;
            default:
                if (text.contains("/")) {
                    messageService.sendMessage(chatId,
                            "ru".equals(languageCode)
                                    ? "Извините, команда не распознана. Введите /help для просмотра доступных команд."
                                    : "uk".equals(languageCode)
                                    ? "Вибачте, команду не розпізнано. Введіть /help для перегляду доступних команд."
                                    : "Sorry, command was not recognized. Type /help for available commands.");
                    break;
                }

                if (userSession.getPreviousState(chatId) != null) {
                    // Переходим к предыдущему состоянию
                    handleAdminCommand(chatId, userSession.getPreviousState(chatId));
                } else {
                    // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
                    messageService.sendMessageWithInlineKeyboard(chatId,
                            "ru".equals(languageCode)
                                    ? "Предыдущего состояния не найдено. Возвращение в главное меню."
                                    : "uk".equals(languageCode)
                                    ? "Попередній стан не знайдено. Повернення до головного меню."
                                    : "No previous state found. Returning to the main menu.",
                            autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                }
                break;
        }
    }

    void mainMenu(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/main_menu");
        messageService.sendMessageWithInlineKeyboard(chatId,
                "ru".equals(languageCode)
                        ? "Главное меню"
                        : "uk".equals(languageCode)
                        ? "Головне меню"
                        : "Main Menu",
                autUserButtons.getAuthenticatedInlineKeyboard(chatId));
    }

    private void goBack(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        // Получаем предыдущее состояние пользователя
        String previousState = userSession.getPreviousState(chatId);
        logger.info("Attempting to go back to previous state: {} for chat ID {}", previousState, chatId);

        if (previousState != null) {
            // Переходим к предыдущему состоянию
            handleAdminCommand(chatId, previousState);
        } else {
            // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
            messageService.sendMessageWithInlineKeyboard(chatId,
                    "ru".equals(languageCode)
                            ? "Предыдущее состояние не найдено. Возвращаемся в главное меню."
                            : "uk".equals(languageCode)
                            ? "Попередній стан не знайдено. Повертаємося до головного меню."
                            : "No previous state found. Returning to the main menu.",
                    autUserButtons.getAuthenticatedInlineKeyboard(chatId));
        }
    }

    public void cancel(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String currentState = userSession.getCurrentState(chatId); // Получаем предыдущее состояние
        messageService.sendMessage(chatId,
                "ru".equals(languageCode)
                        ? "Текущая операция была отменена."
                        : "uk".equals(languageCode)
                        ? "Поточну операцію було скасовано."
                        : "Current operation has been cancelled.");

        if (currentState != null) {
            // Если предыдущее состояние существует, возвращаемся к нему и устанавливаем его как текущее
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, currentState);
            handleAdminCommand(chatId, currentState);
        } else {
            // Если предыдущее состояние отсутствует, возвращаемся в главное меню
            userSession.clearSession(chatId);
            mainMenu(chatId);
        }

        // После возврата очищаем текущие данные сессии, чтобы сбросить состояние
        userSession.clearSession(chatId);
    }

}

package com.example.telegramBotNailsBooking.bot.commands;

import com.example.telegramBotNailsBooking.model.Appointment;
import com.example.telegramBotNailsBooking.model.Services;
import com.example.telegramBotNailsBooking.model.UserSession;
import com.example.telegramBotNailsBooking.model.Users;
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

    public void handleAuthenticatedCommand(Long chatId, String text, Update update) {

        logger.info("Received command: {} for chat ID {}", text, chatId);
        String currentState = userSession.getCurrentState(chatId);
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);



        if (text.equals("/master_record_client")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master_record_client");
            userSession.setPreviousState(chatId, "/master");

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
            cancelButton.setCallbackData("/master_cancel_appointment_master");
            rows.add(List.of(cancelButton));
            keyboard.setKeyboard(rows);

            messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/master_record_client")) {
            String phoneNumber = text.trim();
            Users existingUser = userRepository.findByPhoneNumber(phoneNumber);

            if (existingUser != null) {
                // Клиент найден
                userSession.setCurrentState(chatId, "/master_select_master");
                userSession.setTempData(chatId, "userId", String.valueOf(existingUser.getId()));

                String message = "ru".equals(languageCode)
                        ? "Клиент найден. Выберите услугу для записи:" :
                        "uk".equals(languageCode)
                                ? "Клієнт знайдений. Оберіть послугу для запису:" :
                                "Client found. Choose a service for the appointment:";
                messageService.sendMessage(chatId, message);
                masterService.showServiceSelection(chatId);
            } else {
                // Клиент не найден
                userSession.setCurrentState(chatId, "/master_add_new_client");
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
                cancelButton.setCallbackData("/master_cancel_appointment_master");
                rows.add(List.of(cancelButton));
                keyboard.setKeyboard(rows);

                messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
            }
            return;
        }

// Обработка команды "Отмена"
        if (text.equals("/master_cancel_appointment_master")) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master");

            String message = "ru".equals(languageCode)
                    ? "Процесс записи отменен." :
                    "uk".equals(languageCode)
                            ? "Процес запису скасовано." :
                            "Appointment process canceled.";

            // Возвращаемся в меню управления записями
            messageService.sendMessageWithInlineKeyboard(chatId, message, autUserButtons.masterPanel(chatId));
            return;
        }

        if (userSession.getCurrentState(chatId).equals("/master_add_new_client")) {
            String firstName = text.trim();
            userSession.setTempData(chatId, "firstName", firstName);

            userSession.setCurrentState(chatId, "/master_add_new_client_last_name");
            String message = "ru".equals(languageCode)
                    ? "Введите фамилию клиента:" :
                    "uk".equals(languageCode)
                            ? "Введіть прізвище клієнта:" :
                            "Enter the client's last name:";
            messageService.sendMessage(chatId, message);
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

            String message = "ru".equals(languageCode)
                    ? "Новый клиент добавлен. Выберите услугу:" :
                    "uk".equals(languageCode)
                            ? "Новий клієнт доданий. Оберіть послугу:" :
                            "New client added. Choose a service:";
            messageService.sendMessage(chatId, message);
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

            String message = "ru".equals(languageCode)
                    ? "Добро пожаловать в панель мастера. Выберите действие:"
                    : "uk".equals(languageCode)
                    ? "Ласкаво просимо до панелі майстра. Оберіть дію:"
                    : "Welcome to the master panel. Please select an action:";

            // Отправляем сообщение с панелью мастера
            messageService.sendMessageWithInlineKeyboard(chatId, message, autUserButtons.masterPanel(chatId));
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

        if (text.startsWith("/appointment_details_") && masterRepository.existsByChatId(chatId)) {
            String[] parts = text.replace("/appointment_details_", "").split("_");
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

        if (currentState == null) {
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
                String message;
                if ("ru".equals(languageCode)) {
                    message = "Описание услуги не найдено.";
                } else if ("uk".equals(languageCode)) {
                    message = "Опис послуги не знайдено.";
                } else {
                    message = "Service description not found.";
                }
                messageService.sendMessage(chatId, message);
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
                    String message;
                    if ("ru".equals(language)) {
                        message = "Ошибка: этот номер телефона уже зарегистрирован.";
                    } else if ("uk".equals(language)) {
                        message = "Помилка: цей номер телефону вже зареєстрований.";
                    } else {
                        message = "Error: this phone number is already registered.";
                    }
                    messageService.sendMessage(chatId, message);
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
                String message;
                if ("ru".equals(language)) {
                    message = "Ошибка: введите корректный номер телефона. Номер должен начинаться с '+', содержать только цифры и быть длиной от 10 до 15 символов.";
                } else if ("uk".equals(language)) {
                    message = "Помилка: введіть правильний номер телефону. Номер має починатися з '+', містити лише цифри та мати довжину від 10 до 15 символів.";
                } else {
                    message = "Error: please enter a valid phone number. The number must start with '+', contain only digits, and be 10 to 15 characters long.";
                }
                messageService.sendMessage(chatId, message);
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

        if (currentState == "/select_master") {
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
                String message;
                if ("ru".equals(languageCode)) {
                    message = "Описание услуги не найдено.";
                } else if ("uk".equals(languageCode)) {
                    message = "Опис послуги не знайдено.";
                } else {
                    message = "Service description not found.";
                }
                messageService.sendMessage(chatId, message);
            }
            return;
        }

        if (text.startsWith("/show_times_")) {
            try {
                // Извлекаем дату и статус из команды, предполагая формат: "/show_times_YYYY-MM-DD_STATUS"
                String[] parts = text.replace("/show_times_", "").split("_");

                if (parts.length < 2) {
                    String message;
                    if ("ru".equals(languageCode)) {
                        message = "Неверный формат команды. Пожалуйста, попробуйте снова.";
                    } else if ("uk".equals(languageCode)) {
                        message = "Невірний формат команди. Будь ласка, спробуйте ще раз.";
                    } else {
                        message = "Invalid command format. Please try again.";
                    }
                    messageService.sendMessage(chatId, message);
                    return;
                }

                LocalDate date = LocalDate.parse(parts[0]); // Получаем дату
                Appointment.Status status = Appointment.Status.valueOf(parts[1]); // Получаем статус

                // Вызываем метод для отображения слотов времени на указанную дату и статус
                appointmentService.showTimeSlotsForDate(chatId, date, status);
                return;
            } catch (DateTimeParseException e) {
                String message;
                if ("ru".equals(languageCode)) {
                    message = "Неверный формат даты. Пожалуйста, попробуйте снова.";
                } else if ("uk".equals(languageCode)) {
                    message = "Невірний формат дати. Будь ласка, спробуйте ще раз.";
                } else {
                    message = "Invalid date format. Please try again.";
                }
                messageService.sendMessage(chatId, message);
                return;
            } catch (IllegalArgumentException e) {
                String message;
                if ("ru".equals(languageCode)) {
                    message = "Неверный формат статуса. Пожалуйста, попробуйте снова.";
                } else if ("uk".equals(languageCode)) {
                    message = "Невірний формат статусу. Будь ласка, спробуйте ще раз.";
                } else {
                    message = "Invalid status format. Please try again.";
                }
                messageService.sendMessage(chatId, message);
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

        if (text.startsWith("/transfer_appointment_")) {
            if (userSession.getCurrentState(chatId).startsWith("/confirm_transfer_")) {
                Long appointmentId = Long.parseLong(text.split("_")[2]);
                appointmentService.transferAppointment(chatId, appointmentId);
                return;
            } else {

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Пожалуйста, выберите дату и время для переноса записи.";
                } else if ("uk".equals(languageCode)) {
                    message = "Будь ласка, виберіть дату та час для переносу запису.";
                } else {
                    message = "Please choose date and time for transfer appointment.";
                }

                messageService.sendMessage(chatId, message);
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

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Ваша запись не была перенесена.";
                } else if ("uk".equals(languageCode)) {
                    message = "Ваша запис не була перенесена.";
                } else {
                    message = "Your appointment was not transferred.";
                }
                messageService.sendMessage(chatId, message);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_cancel_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Ваша запись не была отменена.";
                } else if ("uk".equals(languageCode)) {
                    message = "Ваша запис не була скасована.";
                } else {
                    message = "Your appointment was not canceled.";
                }
                messageService.sendMessage(chatId, message);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_delete_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Ваша запись не была удалена.";
                } else if ("uk".equals(languageCode)) {
                    message = "Ваша запис не була видалена.";
                } else {
                    message = "Your appointment was not deleted.";
                }
                messageService.sendMessage(chatId, message);
                autUserButtons.showBookingInfoMenu(chatId);
                return;
            } else if (userSession.getCurrentState(chatId).startsWith("/confirm_review_")) {
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_info");
                userSession.setPreviousState(chatId, "/book_service");
                userSession.clearSession(chatId);

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Мы ждем ваш отзыв в будущем.";
                } else if ("uk".equals(languageCode)) {
                    message = "Ми чекаємо на ваш відгук в майбутньому.";
                } else {
                    message = "We look forward to your review in the future.";
                }
                messageService.sendMessage(chatId, message);
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

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Пожалуйста, выберите дату и время для переноса записи.";
                } else if ("uk".equals(languageCode)) {
                    message = "Будь ласка, виберіть дату та час для переносу запису.";
                } else {
                    message = "Please choose date and time for transfer appointment.";
                }

                messageService.sendMessage(chatId, message);
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

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Пожалуйста, выберите дату и время для удаления записи.";
                } else if ("uk".equals(languageCode)) {
                    message = "Будь ласка, виберіть дату та час для видалення запису.";
                } else {
                    message = "Please choose date and time for delete appointment.";
                }

                messageService.sendMessage(chatId, message);
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

                String message;
                if ("ru".equals(languageCode)) {
                    message = "Пожалуйста, выберите дату и время для оставления отзыва о записи.";
                } else if ("uk".equals(languageCode)) {
                    message = "Будь ласка, виберіть дату та час для відгуку про запис.";
                } else {
                    message = "Please choose date and time for review appointment.";
                }

                messageService.sendMessage(chatId, message);
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
                if (currentState == "/start") {
                    messageService.sendMessage(chatId, "Здравствуйте! Добро пожаловать в наш бот для записи на процедуры." +
                            " Здесь вы можете легко записаться на нужную услугу." +
                            " Следуйте подсказкам, и мы поможем вам пройти процесс записи. Начнем!");
                    personalData(chatId, "ru");
                    break;
                } else if (currentState == "/choose_lang") {
                    changeLanguage(chatId, "ru");
                    break;
                } else {
                    messageService.sendMessage(chatId, "Извините, команда не распознана. Введите /help, чтобы увидеть доступные команды.");

                    if (currentState != null) {
                        // Переходим к предыдущему состоянию
                        handleAuthenticatedCommand(chatId, currentState, update);
                    } else {
                        // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
                        messageService.sendMessageWithInlineKeyboard(chatId, "Предыдущее состояние не найдено. Возвращение в главное меню.", autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                    }
                }
                break;

            case "/lang_en":
                if (currentState == "/start") {
                    messageService.sendMessage(chatId, "Hello! Welcome to our appointment booking bot. " +
                            "Here, you can easily schedule an appointment for your desired service. " +
                            "Simply follow the prompts, and we’ll guide you through the booking process. Let's get started!");
                    personalData(chatId, "en");
                    break;
                } else if (currentState == "/choose_lang") {
                    changeLanguage(chatId, "en");
                    break;
                } else {
                    messageService.sendMessage(chatId, "Sorry, command was not recognized. Type /help for available commands.");

                    if (currentState != null) {
                        // Переходим к предыдущему состоянию
                        handleAuthenticatedCommand(chatId, currentState, update);
                    } else {
                        // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
                        messageService.sendMessageWithInlineKeyboard(chatId, "No previous state found. Returning to the main menu.", autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                    }
                }
                break;

            case "/lang_uk":
                if (currentState.equals("/start")) {
                    messageService.sendMessage(chatId, "Привіт! Вітаємо у нашому боті для запису на процедури." +
                            " Тут ви зможете легко записатися на бажану послугу." +
                            " Просто дотримуйтесь підказок, і ми допоможемо вам пройти процес запису. Почнемо!");
                    personalData(chatId, "uk");
                    break;
                } else if (currentState == "/choose_lang") {
                    changeLanguage(chatId, "uk");
                    break;
                } else {
                    messageService.sendMessage(chatId, "Вибачте, команду не розпізнано. Введіть /help, щоб побачити доступні команди.");

                    if (currentState != null) {
                        // Переходим к предыдущему состоянию
                        handleAuthenticatedCommand(chatId, currentState, update);
                    } else {
                        // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
                        messageService.sendMessageWithInlineKeyboard(chatId, "Попередній стан не знайдено. Повернення в головне меню.", autUserButtons.getAuthenticatedInlineKeyboard(chatId));
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
                messageService.sendMessage(chatId, "ru".equals(languageCode) ? "Запись была отменена." :
                        "uk".equals(languageCode) ? "Запис було скасовано." :
                                "Booking has been canceled.");
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
                messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Вот ваше меню:" :
                                "uk".equals(languageCode) ? "Ось ваше меню:" :
                                        "Here is your menu:",
                        menuService.getMenuInlineKeyboard(chatId));
                break;

            case "/settings":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/settings");
                userSession.setPreviousState(chatId, "/menu");
                menuService.handleSettingsCommand(chatId, messageService);
                break;

            case "/cancel":
                cancel(chatId);
                break;

            case "/book_service":
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/book_service");
                userSession.setPreviousState(chatId, "/menu");
                menuService.bookingManagerButton(chatId, messageService);
                break;

            case "/book":
                appointmentService.startBooking(chatId);
                messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Вы можете отменить эту операцию, используя кнопку ниже." :
                                "uk".equals(languageCode) ? "Ви можете скасувати цю операцію, використовуючи кнопку нижче." :
                                        "You can cancel this operation using the button below.",
                        autUserButtons.getCancelInlineKeyboard(chatId));
                break;
            default:
                if (text.contains("/")) {
                    messageService.sendMessage(chatId, "ru".equals(languageCode) ? "Извините, команда не распознана. Введите /help для доступных команд." :
                            "uk".equals(languageCode) ? "Вибачте, команда не розпізнана. Введіть /help для доступних команд." :
                                    "Sorry, command was not recognized. Type /help for available commands.");
                    break;
                }

                if (userSession.getPreviousState(chatId) != null) {
                    // Переходим к предыдущему состоянию
                    handleAuthenticatedCommand(chatId, userSession.getPreviousState(chatId), update);
                } else {
                    // Если предыдущее состояние отсутствует, возвращаем пользователя в главное меню
                    messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Предыдущего состояния не найдено. Возвращаем в главное меню." :
                                    "uk".equals(languageCode) ? "Попереднього стану не знайдено. Повертаємо до головного меню." :
                                            "No previous state found. Returning to the main menu.",
                            autUserButtons.getAuthenticatedInlineKeyboard(chatId));
                }
                break;
        }
    }

    protected void cancel(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String currentState = userSession.getCurrentState(chatId); // Получаем предыдущее состояние
        messageService.sendMessage(chatId, "ru".equals(languageCode) ? "Текущая операция была отменена." :
                "uk".equals(languageCode) ? "Поточна операція була скасована." :
                        "Current operation has been cancelled.");

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
            messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Предыдущего состояния не найдено. Возвращаем в главное меню." :
                            "uk".equals(languageCode) ? "Попереднього стану не знайдено. Повертаємо до головного меню." :
                                    "No previous state found. Returning to the main menu.",
                    autUserButtons.getAuthenticatedInlineKeyboard(chatId));
        }
    }

    protected void mainMenu(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/main_menu");
        messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Главное меню" :
                        "uk".equals(languageCode) ? "Головне меню" :
                                "Main Menu",
                autUserButtons.getAuthenticatedInlineKeyboard(chatId));
    }
}

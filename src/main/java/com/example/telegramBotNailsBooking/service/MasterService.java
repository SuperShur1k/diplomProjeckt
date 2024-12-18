package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.model.*;
import com.example.telegramBotNailsBooking.repository.*;
import com.example.telegramBotNailsBooking.service.buttons.AdminButtons;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MasterService {

    @Autowired
    private MasterRepository masterRepository;

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
    private AvailableDateRepository availableDateRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AvailableDateService availableDateService;

    public void initiateAddMaster(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Формируем сообщение с использованием локализованных ключей
        String message = messageService.getLocalizedMessage("initiate_add_master", languageCode);

        messageService.sendMessage(chatId, message);
        userSession.setSettingMaster(chatId, true);

        // Сообщение о кнопке отмены операции
        String cancelMessage = messageService.getLocalizedMessage("cancel_add_master", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, cancelMessage, autUserButtons.getCancelInlineKeyboard(chatId));
    }

    public void addMaster(Long chatId, String text) {
        // Получаем язык пользователя
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Проверяем, на каком этапе находится процесс добавления мастера
        String[] masterInfo = userSession.getMasterInfo(chatId);

        if (masterInfo == null) {
            // Инициализация процесса добавления мастера, ожидаем ввода имени пользователя
            Users users = userRepository.findByPhoneNumber(text);
            String message = messageService.getLocalizedMessage("user_not_found", languageCode);

            if (users == null) {
                messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getAdminInlineKeyboard(chatId));
                userSession.setSettingMaster(chatId, false);
            } else {
                // Сохраняем начальные данные мастера и запрашиваем описание
                masterInfo = new String[3];
                masterInfo[0] = users.getPhoneNumber();  // Сохраняем имя пользователя
                masterInfo[1] = users.getFirstName() + " " + users.getLastName();  // Имя мастера
                userSession.setMasterInfo(chatId, masterInfo);
                String descriptionMessage = messageService.getLocalizedMessage("enter_description", languageCode);
                messageService.sendMessage(chatId, descriptionMessage);
            }
        } else if (masterInfo[2] == null) {
            // Второй этап - получение описания
            masterInfo[2] = text;  // Сохраняем описание мастера
            String socialLinkMessage = messageService.getLocalizedMessage("enter_social_link", languageCode);
            messageService.sendMessage(chatId, socialLinkMessage);
            userSession.setMasterInfo(chatId, masterInfo);  // Обновляем информацию
        } else {
            // Третий этап - получение ссылки и сохранение мастера
            Users users = userRepository.findByPhoneNumber(masterInfo[0]);
            Master master = new Master();
            master.setName(masterInfo[1]);
            master.setDescription(masterInfo[2]);
            master.setSocialLink(text);  // Устанавливаем ссылку, введенную пользователем
            master.setStatus(Master.Status.ACTIVE);
            master.setChatId(users.getChatId());
            master.setPhoneNumber(users.getPhoneNumber());

            // Сохраняем нового мастера в БД
            masterRepository.save(master);

            String successMessage = messageService.getLocalizedMessage("master_added_successfully", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getAdminInlineKeyboard(chatId));
            userSession.setSettingMaster(chatId, false);  // Сбрасываем состояние назначения мастера
            userSession.removeMasterInfo(chatId);  // Очищаем промежуточные данные
        }
    }

    public void showMasterManagementMenu(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список всех мастеров
        List<Master> masters = masterRepository.findAll();

        if (masters.isEmpty()) {
            // Локализуем сообщение о том, что нет мастеров
            String noMastersMessage = messageService.getLocalizedMessage("no_masters_for_management", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, noMastersMessage, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        // Создаем inline-кнопки с именами мастеров
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName() + " (" + master.getStatus() + ")");
            button.setCallbackData("/manage_master_" + master.getId());
            rows.add(List.of(button));
        }

        // Добавляем кнопку "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(messageService.getLocalizedMessage("cancel_button", languageCode));
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        keyboard.setKeyboard(rows);

        // Локализуем сообщение для выбора мастера
        String message = messageService.getLocalizedMessage("select_master_to_manage", languageCode);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showMasterSettings(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Master master = masterRepository.findById(masterId).orElse(null);
        if (master == null) {
            String notFoundMessage = messageService.getLocalizedMessage("master_not_found", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, notFoundMessage, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        // Создаем inline-кнопки для управления мастером
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки для изменения статуса мастера
        InlineKeyboardButton activateButton = new InlineKeyboardButton();
        activateButton.setText(messageService.getLocalizedMessage("activate_master", languageCode));
        activateButton.setCallbackData("/set_master_active_" + masterId);

        InlineKeyboardButton deactivateButton = new InlineKeyboardButton();
        deactivateButton.setText(messageService.getLocalizedMessage("deactivate_master", languageCode));
        deactivateButton.setCallbackData("/set_master_inactive_" + masterId);

        rows.add(List.of(activateButton, deactivateButton));

        // Кнопка для удаления мастера
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(messageService.getLocalizedMessage("delete_master", languageCode));
        deleteButton.setCallbackData("/delete_master_" + masterId);

        rows.add(List.of(deleteButton));

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/manage_masters");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String message = messageService.getLocalizedMessage("master_settings_title", languageCode, master.getName());
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void setMasterStatus(Long chatId, Long masterId, Master.Status status) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Master master = masterRepository.findById(masterId).orElse(null);
        if (master == null) {
            // Используем локализованное сообщение для мастера не найден
            String notFoundMessage = messageService.getLocalizedMessage("master_not_found", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, notFoundMessage, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        // Устанавливаем новый статус мастера
        master.setStatus(status);
        masterRepository.save(master);

        // Используем локализованное сообщение для успешного изменения статуса
        String successMessage = messageService.getLocalizedMessage("master_status_changed", languageCode, master.getName(), status);
        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getAdminInlineKeyboard(chatId));
    }

    public void deleteMaster(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Master master = masterRepository.findById(masterId).orElse(null);
        if (master == null) {
            // Локализованное сообщение для мастера не найдено
            String notFoundMessage = messageService.getLocalizedMessage("master_not_found", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, notFoundMessage, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        // Удаляем все записи о встречах, связанные с мастером
        List<Appointment> appointments = appointmentRepository.findByMasterId(masterId);
        for (Appointment appointment : appointments) {
            appointmentRepository.delete(appointment);
        }

        // Удаляем все связанные доступные даты и временные слоты
        List<AvailableDate> dates = availableDateRepository.findByMasterId(masterId);
        for (AvailableDate date : dates) {
            timeSlotRepository.deleteAllByAvailableDateId(date.getId());
            availableDateRepository.delete(date);
        }

        // Удаляем все услуги, связанные с мастером
        List<Services> services = serviceRepository.findByMasterId(masterId);
        serviceRepository.deleteAll(services);

        // Удаляем самого мастера
        masterRepository.delete(master);

        // Локализованное сообщение для успешного удаления мастера
        String successMessage = messageService.getLocalizedMessage("master_deleted", languageCode, master.getName());
        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getAdminInlineKeyboard(chatId));
    }

    public void initialWriteToAdmin(Long chatId, Long adminChatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем информацию об администраторе
        Users admin = userRepository.findByChatId(adminChatId);
        if (admin == null) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/main_menu");
            String errorMessage = messageService.getLocalizedMessage("admin_not_found", languageCode, adminChatId);
            messageService.sendMessageWithInlineKeyboard(chatId, errorMessage, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        // Локализованное сообщение пользователю
        String message = messageService.getLocalizedMessage("write_to_admin_message", languageCode, admin.getFirstName());
        messageService.sendMessage(chatId, message);

        // Устанавливаем состояние для чата
        userSession.setCurrentState(chatId, "/writing_to_admin_from_master_" + adminChatId);
        userSession.setPreviousState(chatId, "/main_menu");
    }

    public void writeToAdmin(Long masterChatId, Long adminChatId, String messageText) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);
        String adminLanguageCode = userRepository.findLanguageCodeByChatId(adminChatId);

        // Проверяем, что мастер существует
        Master master = masterRepository.findByChatId(masterChatId);
        if (master == null) {
            userSession.clearStates(masterChatId);
            userSession.setCurrentState(masterChatId, "/main_menu");
            messageService.sendMessage(masterChatId, messageService.getLocalizedMessage("master_not_found", languageCode));
            return;
        }

        // Формируем сообщение для администратора
        String messageToAdmin = messageService.getLocalizedMessage("message_from_master", adminLanguageCode, master.getName(), messageText);

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText(messageService.getLocalizedMessage("reply_button", adminLanguageCode));
        replyButton.setCallbackData("/write_master_" + masterChatId);

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

    public void initialCheckAppointments(Long masterChatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);

        // Получаем мастера по chatId
        Master master = masterRepository.findByChatId(masterChatId);
        if (master == null) {
            String noMasterMessage = messageService.getLocalizedMessage("master_not_found", languageCode);
            messageService.sendMessage(masterChatId, noMasterMessage);
            return;
        }

        // Получаем уникальные даты записей мастера
        List<LocalDate> appointmentDates = appointmentRepository.findConfirmedAppointmentsByMaster(master.getId())
                .stream()
                .map(appointment -> appointment.getAppointmentDate().toLocalDate())
                .distinct() // Убираем повторяющиеся даты
                .sorted() // Сортируем даты
                .collect(Collectors.toList());

        // Проверяем, есть ли записи
        if (appointmentDates.isEmpty()) {
            String noAppointmentsMessage = messageService.getLocalizedMessage("no_appointments", languageCode);
            messageService.sendMessage(masterChatId, noAppointmentsMessage);
            return;
        }

        // Создаём кнопки для уникальных дат
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (LocalDate date : appointmentDates) {
            InlineKeyboardButton dateButton = new InlineKeyboardButton();
            dateButton.setText(date.toString()); // Отображаем дату как текст
            dateButton.setCallbackData("/appointments_for_date_" + date); // Устанавливаем callback с датой
            rows.add(List.of(dateButton));
        }

        // Добавляем кнопку "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/master");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        String message = messageService.getLocalizedMessage("select_date_message", languageCode);
        messageService.sendMessageWithInlineKeyboard(masterChatId, message, keyboard);
    }

    public void timeCheckAppointments(Long masterChatId, LocalDate date) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);

        // Получаем мастера по chatId
        Master master = masterRepository.findByChatId(masterChatId);
        if (master == null) {
            String noMasterMessage = messageService.getLocalizedMessage("master_not_found", languageCode);
            messageService.sendMessage(masterChatId, noMasterMessage);
            return;
        }

        // Получаем записи для мастера на указанную дату со статусом CONFIRMED
        List<LocalTime> appointmentTimes = appointmentRepository.findConfirmedAppointmentsByMaster(master.getId())
                .stream()
                .filter(appointment -> appointment.getAppointmentDate().toLocalDate().equals(date)) // Фильтруем по дате
                .map(appointment -> appointment.getAppointmentDate().toLocalTime()) // Получаем время
                .sorted() // Сортируем по времени
                .collect(Collectors.toList());

        // Проверяем, есть ли записи
        if (appointmentTimes.isEmpty()) {
            String noAppointmentsMessage = messageService.getLocalizedMessage("no_appointments_for_date", languageCode, date);
            messageService.sendMessage(masterChatId, noAppointmentsMessage);
            return;
        }

        // Генерируем кнопки для каждого времени
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (LocalTime time : appointmentTimes) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton();
            timeButton.setText(time.toString()); // Отображаем время как текст
            timeButton.setCallbackData("/master_appointment_details_" + date + "_" + time); // Устанавливаем callback с датой и временем
            rows.add(List.of(timeButton));
        }

        // Добавляем кнопку "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/view_appointments");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        String message = messageService.getLocalizedMessage("select_time_message", languageCode);
        messageService.sendMessageWithInlineKeyboard(masterChatId, message, keyboard);
    }

    public void showInfoAppointments(Long masterChatId, LocalDate date, LocalTime time) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);

        // Получаем мастера по chatId
        Master master = masterRepository.findByChatId(masterChatId);
        if (master == null) {
            String noMasterMessage = messageService.getLocalizedMessage("master_not_found", languageCode);
            messageService.sendMessage(masterChatId, noMasterMessage);
            return;
        }

        // Ищем запись на указанную дату и время для мастера
        Appointment appointment = appointmentRepository.findConfirmedAppointmentsByMaster(master.getId())
                .stream()
                .filter(app -> app.getAppointmentDate().toLocalDate().equals(date) && app.getAppointmentDate().toLocalTime().equals(time))
                .findFirst()
                .orElse(null);

        if (appointment == null) {
            String noAppointmentMessage = messageService.getLocalizedMessage("appointment_not_found", languageCode, date, time);
            messageService.sendMessage(masterChatId, noAppointmentMessage);
            return;
        }

        // Получаем информацию о клиенте
        Users client = appointment.getUsers();
        if (client == null) {
            String noClientMessage = messageService.getLocalizedMessage("client_not_found", languageCode);
            messageService.sendMessage(masterChatId, noClientMessage);
            return;
        }

        // Формируем сообщение с информацией о записи
        String appointmentInfo = messageService.getLocalizedMessage("appointment_info", languageCode, client.getFirstName(), client.getLastName(), client.getPhoneNumber(), client.getLanguage(), date, time);

        // Создаём кнопки
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Написать клиенту"
        InlineKeyboardButton messageClientButton = new InlineKeyboardButton();
        messageClientButton.setText(messageService.getLocalizedMessage("message_client_button", languageCode));
        messageClientButton.setCallbackData("/message_client_" + client.getChatId());
        rows.add(List.of(messageClientButton));

        // Кнопка "Отменить запись"
        InlineKeyboardButton cancelAppointmentButton = new InlineKeyboardButton();
        cancelAppointmentButton.setText(messageService.getLocalizedMessage("cancel_appointment_button", languageCode));
        cancelAppointmentButton.setCallbackData("/master_cancel_appointment_" + appointment.getId());
        rows.add(List.of(cancelAppointmentButton));

        // Кнопка "Перенести запись"
        InlineKeyboardButton rescheduleAppointmentButton = new InlineKeyboardButton();
        rescheduleAppointmentButton.setText(messageService.getLocalizedMessage("reschedule_appointment_button", languageCode));
        rescheduleAppointmentButton.setCallbackData("/master_reschedule_appointment_" + appointment.getId());
        rows.add(List.of(rescheduleAppointmentButton));

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("back_button", languageCode));
        backButton.setCallbackData("/appointments_for_date_" + date);
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с информацией о записи и кнопками
        messageService.sendMessageWithInlineKeyboard(masterChatId, appointmentInfo, keyboard);
    }

    public void cancelAppointment(Long chatId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment_dont_found", userRepository.findLanguageCodeByChatId(chatId));
            messageService.sendMessage(chatId, message);
            return;
        }

        // Update appointment status to CANCELLED
        appointment.setStatus(Appointment.Status.CANCELLED);
        appointmentRepository.save(appointment);

        // Free up the associated time slot
        Optional<AvailableDate> availableDate = availableDateRepository.findByDateAndMasterId(
                appointment.getAppointmentDate().toLocalDate(),
                appointment.getMaster().getId()
        );

        if (availableDate.isPresent()) {
            Optional<TimeSlot> timeSlot = timeSlotRepository.findByMasterDateAndTime(
                    appointment.getAppointmentDate().toLocalTime(),
                    availableDate.get().getId(),
                    appointment.getMaster().getId()
            );

            timeSlot.ifPresent(slot -> {
                slot.setBooked(false);
                timeSlotRepository.save(slot);
            });
        } else {
            System.out.println("AvailableDate not found for master and date");
        }

        // Notify the client
        String clientMessage = messageService.getLocalizedMessage("appointment_cancelled_for_client",
                userRepository.findLanguageCodeByChatId(appointment.getChatId()),
                appointment.getAppointmentDate().toLocalDate(),
                appointment.getAppointmentDate().toLocalTime());
        messageService.sendMessage(appointment.getChatId(), clientMessage);

        // Notify the master
        String masterMessage = messageService.getLocalizedMessage("appointment_cancelled_for_master",
                userRepository.findLanguageCodeByChatId(appointment.getMaster().getChatId()),
                appointment.getAppointmentDate().toLocalDate(),
                appointment.getAppointmentDate().toLocalTime());
        messageService.sendMessage(appointment.getMaster().getChatId(), masterMessage);

        // Notify the admin
        String adminMessage = messageService.getLocalizedMessage("appointment_cancelled_for_admin",
                userRepository.findLanguageCodeByChatId(chatId));
        messageService.sendMessageWithInlineKeyboard(chatId, adminMessage, autUserButtons.masterPanel(chatId));
    }

    public void selectTransferDate(Long chatId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment_dont_found", userRepository.findLanguageCodeByChatId(chatId));
            messageService.sendMessage(chatId, message);
            return;
        }

        List<AvailableDate> availableDates = availableDateService.getAvailableDatesForMaster(appointment.getMaster().getId())
                .stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now())) // Исключаем прошлые даты
                .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId())
                        .stream().anyMatch(slot -> !slot.isBooked())) // Оставляем только даты с доступными слотами
                .sorted(Comparator.comparing(AvailableDate::getDate)) // Сортируем по возрастанию даты
                .collect(Collectors.toList());

        if (availableDates.isEmpty()) {
            String message = messageService.getLocalizedMessage("no_available_dates_for_transfer", userRepository.findLanguageCodeByChatId(chatId));
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (AvailableDate date : availableDates) {
            InlineKeyboardButton dateButton = new InlineKeyboardButton(date.getDate().toString());
            dateButton.setCallbackData("/master_select_transfer_date_" + date.getId() + "_" + appointmentId);
            rows.add(List.of(dateButton));
        }

        keyboard.setKeyboard(rows);
        String message = messageService.getLocalizedMessage("select_date_for_transfer", userRepository.findLanguageCodeByChatId(chatId));
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void selectTransferTime(Long chatId, Long dateId, Long appointmentId) {
        List<TimeSlot> timeSlots = availableDateService.getTimeSlotsForAvailableDate(dateId)
                .stream()
                .filter(slot -> !slot.isBooked())
                .sorted(Comparator.comparing(TimeSlot::getTime))
                .collect(Collectors.toList());

        if (timeSlots.isEmpty()) {
            String message = messageService.getLocalizedMessage("no_available_time_slots", userRepository.findLanguageCodeByChatId(chatId));
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (TimeSlot slot : timeSlots) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton(slot.getTime().toString());
            timeButton.setCallbackData("/master_select_transfer_time_" + slot.getId() + "_" + appointmentId + "_" + dateId);
            rows.add(List.of(timeButton));
        }

        keyboard.setKeyboard(rows);
        String message = messageService.getLocalizedMessage("select_time_for_transfer", userRepository.findLanguageCodeByChatId(chatId));
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void finalizeTransfer(Long chatId, Long appointmentId, Long timeSlotId, Long dateId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        TimeSlot newTimeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);
        AvailableDate newDate = availableDateRepository.findById(dateId).orElse(null);

        if (appointment == null || newTimeSlot == null) {
            String message = messageService.getLocalizedMessage("transfer_failed", userRepository.findLanguageCodeByChatId(chatId));
            messageService.sendMessage(chatId, message);
            return;
        }

        // Release the old time slot
        TimeSlot oldTimeSlot = timeSlotRepository.findByTimeAndMasterId(
                appointment.getAppointmentDate().toLocalTime(),
                appointment.getMaster().getId()
        );
        if (oldTimeSlot != null) {
            oldTimeSlot.setBooked(false);
            timeSlotRepository.save(oldTimeSlot);
        }

        // Update the appointment
        appointment.setAppointmentDate(LocalDateTime.of(newDate.getDate(), newTimeSlot.getTime()));
        appointmentRepository.save(appointment);

        // Mark the new time slot as booked
        newTimeSlot.setBooked(true);
        timeSlotRepository.save(newTimeSlot);

        // Success message to admin
        String successMessage = messageService.getLocalizedMessage("appointment_successfully_transferred", userRepository.findLanguageCodeByChatId(chatId));
        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, autUserButtons.masterPanel(chatId));

        // Notify the client
        String clientMessage = messageService.getLocalizedMessage(
                "appointment_rescheduled",
                userRepository.findLanguageCodeByChatId(appointment.getChatId()),
                newTimeSlot.getAvailableDate().getDate(),
                newTimeSlot.getTime()
        );
        messageService.sendMessage(appointment.getChatId(), clientMessage);
    }

    public void initialWriteToClient(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<Users> users = userRepository.findAll();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Users user : users) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton(user.getFirstName() + " " + user.getLastName());
            timeButton.setCallbackData("/message_client_" + user.getChatId());
            rows.add(List.of(timeButton));
        }

        keyboard.setKeyboard(rows);

        // Получаем локализованное сообщение для выбора клиента
        String message = messageService.getLocalizedMessage("select_client", languageCode);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void writeToClient(Long chatId, Long clientChatId) {
        // Проверяем, существует ли клиент с данным chatId
        if (!userRepository.existsByChatId(clientChatId)) {
            // Получаем локализованное сообщение об ошибке
            String message = messageService.getLocalizedMessage("client_not_found", userRepository.findLanguageCodeByChatId(chatId));
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем состояние в сессии мастера
        userSession.setCurrentState(chatId, "/master_write_to_client_" + clientChatId);

        // Получаем локализованное сообщение для запроса ввода сообщения
        String messagePrompt = messageService.getLocalizedMessage("enter_message_for_client", userRepository.findLanguageCodeByChatId(chatId));
        messageService.sendMessage(chatId, messagePrompt);
    }

    public void finishWriteToClient(Long chatId, Long clientChatId, String text) {
        // Получаем языковой код для отправителя и получателя
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String clientLanguageCode = userRepository.findLanguageCodeByChatId(clientChatId);

        // Формируем сообщение для получателя с использованием локализованных строк
        String messageToRecipient = messageService.getLocalizedMessage("master_wrote_to_you", clientLanguageCode, text);

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText(messageService.getLocalizedMessage("reply_button_text", clientLanguageCode));
        replyButton.setCallbackData("/reply_to_master_" + chatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Отправляем сообщение получателю
        messageService.sendMessageWithInlineKeyboard(clientChatId, messageToRecipient, keyboard);

        // Подтверждаем отправителю
        String confirmationMessage = messageService.getLocalizedMessage("message_sent_confirmation_user", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, autUserButtons.masterPanel(chatId));

        // Очищаем состояние отправителя
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/view_appointments");
        userSession.setPreviousState(chatId, "/master");
    }

    public void showServiceSelection(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<Services> services = serviceRepository.findAll();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Получаем название услуги на нужном языке
            String serviceName = messageService.getLocalizedServiceName(service, languageCode);
            InlineKeyboardButton button = new InlineKeyboardButton(serviceName);
            button.setCallbackData("/master_service_" + service.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);

        // Получаем локализованное сообщение для выбора услуги
        String message = messageService.getLocalizedMessage("select_service_message", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showDateSelection(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long masterId = masterRepository.findByChatId(chatId).getId();
        List<AvailableDate> dates = availableDateRepository.findByMasterId(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        dates = dates.stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now())) // Только будущие даты
                .collect(Collectors.toList());

        for (AvailableDate date : dates) {
            InlineKeyboardButton button = new InlineKeyboardButton(date.getDate().toString());
            button.setCallbackData("/master_date_master_" + date.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);

        // Получаем локализованное сообщение для выбора даты
        String message = messageService.getLocalizedMessage("select_date_message", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showTimeSelection(Long chatId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<TimeSlot> slots = availableDateService.getTimeSlotsForAvailableDate(dateId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = slots.stream()
                .filter(slot -> !slot.isBooked()) // Отбираем только доступные слоты
                .map(slot -> {
                    InlineKeyboardButton button = new InlineKeyboardButton(slot.getTime().toString());
                    button.setCallbackData("/master_time_master_" + slot.getId());
                    return List.of(button);
                }).collect(Collectors.toList());

        keyboard.setKeyboard(rows);

        // Получаем локализованное сообщение для выбора времени
        String message = messageService.getLocalizedMessage("select_time_message", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void finalizeAppointment(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        Long userId = Long.parseLong(userSession.getTempData(chatId, "userId"));
        Long serviceId = Long.parseLong(userSession.getTempData(chatId, "serviceId"));
        Long timeSlotId = Long.parseLong(userSession.getTempData(chatId, "timeSlotId"));
        Long userChatId = userRepository.findById(userId).get().getChatId();

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);
        if (timeSlot == null) {
            String errorMessage = messageService.getLocalizedMessage("error_invalid_time", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, errorMessage, autUserButtons.masterPanel(chatId));
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/master");
            userSession.setPreviousState(chatId, "/main_menu");
            userSession.clearSession(chatId);
            userSession.clearTempData(chatId);
            return;
        }

        Appointment appointment = new Appointment();
        appointment.setUsers(userRepository.findById(userId).orElse(null));
        appointment.setMaster(masterRepository.findByChatId(chatId));
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
            String clientSuccessMessage = messageService.getLocalizedMessage("client_success_booking", languageCodeClient, appointment.getAppointmentDate().toLocalDate(), appointment.getAppointmentDate().toLocalTime());
            messageService.sendMessage(userChatId, clientSuccessMessage);
        }

        String successMessage = messageService.getLocalizedMessage("appointment_successfully_created", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getManageAppointmentsKeyboard(chatId));

        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/appointments_manage");
        userSession.setPreviousState(chatId, "/other_actions");
        userSession.clearSession(chatId);
        userSession.clearTempData(chatId);
    }
}
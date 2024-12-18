package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.model.*;
import com.example.telegramBotNailsBooking.repository.*;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import com.example.telegramBotNailsBooking.service.buttons.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private AvailableDateService availableDateService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserSession userSession;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvailableDateRepository availableDateRepository;

    @Autowired
    private MenuService menuService;

    @Autowired
    private AutUserButtons autUserButtons;

    public void startBooking(Long chatId) {
        userSession.setCurrentState(chatId, "/select_master");
        selectMaster(chatId);
    }

    public void selectMaster(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Локализуем сообщение о выборе мастера
        String message = messageService.getLocalizedMessage("master.select", languageCode);
        messageService.sendMessage(chatId, message);

        for (Master master : masterRepository.findAllByStatus(Master.Status.ACTIVE)) {
            // Получаем все доступные даты для мастера
            List<AvailableDate> availableDates = availableDateService.getAvailableDatesForMaster(master.getId());

            // Фильтруем даты, чтобы оставить только те, где есть незанятые временные слоты
            availableDates = availableDates.stream()
                    .filter(date -> date.getDate().isAfter(LocalDate.now()))  // Исключаем прошлое и сегодняшнюю дату
                    .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId()).stream().anyMatch(slot -> !slot.isBooked()))  // Оставляем только даты с доступными временными слотами
                    .collect(Collectors.toList());

            // Если нет свободных дат, отправляем сообщение с номером телефона мастера
            if (availableDates.isEmpty()) {
                String noDatesMessage = messageService.getLocalizedMessage("master.no_available_dates", languageCode, master.getName(), master.getPhoneNumber());
                messageService.sendMessage(chatId, noDatesMessage);
                continue;
            }

            // Создаем клавиатуру с кнопками для мастера
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            List<InlineKeyboardButton> buttons = new ArrayList<>();

            // Кнопка с именем мастера и сохранением ID мастера в коллбэк-данных
            InlineKeyboardButton masterButton = new InlineKeyboardButton();
            masterButton.setText(master.getName());
            masterButton.setCallbackData("/select_date_" + master.getId());
            buttons.add(masterButton);

            // Кнопка для ссылки на соцсети мастера
            InlineKeyboardButton socialButton = new InlineKeyboardButton();
            socialButton.setText(messageService.getLocalizedMessage("master.social_link", languageCode));
            socialButton.setUrl(master.getSocialLink());
            buttons.add(socialButton);

            rowsInline.add(buttons);
            keyboardMarkup.setKeyboard(rowsInline);

            // Отправляем описание мастера вместе с клавиатурой
            messageService.sendMessageWithInlineKeyboard(chatId, master.getDescription(), keyboardMarkup);
        }

        // Обновляем сессию пользователя
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/book_service");
        userSession.setPreviousState(chatId, "/menu");
    }

    public void selectDate(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String masterIdString = userSession.getSelectedMaster(chatId);
        if (masterIdString == null) {
            // Локализуем сообщение, если мастер не выбран
            String message = messageService.getLocalizedMessage("date.select_master_first", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        Long masterId = Long.valueOf(masterIdString);
        List<AvailableDate> dates = availableDateService.getAvailableDatesForMaster(masterId);

        // Фильтруем даты, оставляя только те, где есть незанятые временные слоты
        dates = dates.stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now()))  // Исключаем прошлое и сегодняшнюю дату
                .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId()).stream().anyMatch(slot -> !slot.isBooked()))  // Оставляем только даты с доступными временными слотами
                .collect(Collectors.toList());

        if (dates.isEmpty()) {
            // Локализуем сообщение, если нет доступных дат
            String message = messageService.getLocalizedMessage("date.no_available_dates", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Для каждой доступной даты создаём кнопку
        for (AvailableDate date : dates) {
            InlineKeyboardButton button = new InlineKeyboardButton(date.getDate().toString());

            // Устанавливаем стандартный коллбэк "/select_time"
            button.setCallbackData("/select_time");

            // Сохраняем ID выбранной даты в UserSession при нажатии
            userSession.setSelectedDate(chatId, String.valueOf(date.getId()));

            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);

        // Локализуем сообщение для выбора даты
        String message = messageService.getLocalizedMessage("date.select_date", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void selectTime(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String dateIdString = userSession.getSelectedDate(chatId);
        if (dateIdString == null) {
            // Локализуем сообщение, если дата не выбрана
            String message = messageService.getLocalizedMessage("time.select_date_first", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        Long dateId = Long.valueOf(dateIdString);
        List<TimeSlot> slots = availableDateService.getTimeSlotsForAvailableDate(dateId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = slots.stream()
                .filter(slot -> !slot.isBooked())
                .map(slot -> {
                    InlineKeyboardButton button = new InlineKeyboardButton(slot.getTime().toString());
                    userSession.setSelectedTimeSlot(chatId, String.valueOf(slot.getId()));
                    button.setCallbackData("/select_service");
                    return List.of(button);
                }).collect(Collectors.toList());

        keyboard.setKeyboard(rows);

        // Локализуем сообщение для выбора времени
        String message = messageService.getLocalizedMessage("time.select_time", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void selectService(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String masterIdString = userSession.getSelectedMaster(chatId);
        if (masterIdString == null) {
            // Локализуем сообщение, если мастер не выбран
            String message = messageService.getLocalizedMessage("service.select_master_first", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        Long masterId = Long.valueOf(masterIdString);
        List<Services> services = serviceRepository.findByMasterId(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Локализуем название услуги с ценой
            InlineKeyboardButton selectButton = new InlineKeyboardButton(messageService.getLocalizedServiceName(service, languageCode));
            selectButton.setCallbackData("/select_service_" + service.getId());

            // Кнопка для отображения цены
            InlineKeyboardButton priceButton = new InlineKeyboardButton(service.getPrice() + "€ ↑ ↑");
            priceButton.setCallbackData("/select_service_" + service.getId());

            // Локализуем текст кнопки "Описание услуги"
            InlineKeyboardButton descriptionButton = new InlineKeyboardButton(
                    messageService.getLocalizedMessage("service.description", languageCode)
            );
            descriptionButton.setCallbackData("/show_description_" + service.getId());

            // Добавляем кнопки на одну строку
            rows.add(List.of(selectButton));
            rows.add(List.of(descriptionButton, priceButton));
        }

        keyboard.setKeyboard(rows);

        // Локализуем сообщение о выборе услуги
        String message = messageService.getLocalizedMessage("service.select_service", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void handleServiceSelection(Long chatId, Long serviceId) {
        userSession.setSelectedService(chatId, String.valueOf(serviceId)); // Сохраняем ID выбранной услуги
        confirmBooking(chatId); // Переходим к подтверждению бронирования
    }

    public void confirmBooking(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Long masterId = Long.valueOf(userSession.getSelectedMaster(chatId));
        Long dateId = Long.valueOf(userSession.getSelectedDate(chatId));
        Long timeSlotId = Long.valueOf(userSession.getSelectedTimeSlot(chatId));
        Long serviceId = Long.valueOf(userSession.getSelectedService(chatId));

        Master master = masterRepository.findById(masterId).orElse(null);
        AvailableDate date = availableDateRepository.findById(dateId).orElse(null);
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);
        Services service = serviceRepository.findById(serviceId).orElse(null);

        if (master == null || date == null || timeSlot == null || service == null) {
            // Локализуем сообщение об ошибке при получении данных
            String errorMessage = messageService.getLocalizedMessage("booking.error", languageCode);
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        // Локализуем сообщение с деталями бронирования
        String confirmationMessage = messageService.getLocalizedMessage(
                "booking.confirmation", languageCode,
                master.getName(),
                messageService.getLocalizedServiceName(service, languageCode),
                service.getPrice(),
                date.getDate(),
                timeSlot.getTime()
        );

        // Создаем клавиатуру с кнопками "Да" и "Нет"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("booking.yes", languageCode)
        );
        yesButton.setCallbackData("/confirm_appointment"); // callback для подтверждения

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("booking.no", languageCode)
        );
        noButton.setCallbackData("/cancel_appointment"); // callback для отмены

        List<InlineKeyboardButton> row = List.of(yesButton, noButton);
        keyboard.setKeyboard(List.of(row));

        // Отправляем сообщение с подтверждением и клавиатурой
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }

    public void finalizeBooking(Long chatId) {
        Long masterId = Long.valueOf(userSession.getSelectedMaster(chatId));
        Long dateId = Long.valueOf(userSession.getSelectedDate(chatId));
        Long timeSlotId = Long.valueOf(userSession.getSelectedTimeSlot(chatId));
        Long serviceId = Long.valueOf(userSession.getSelectedService(chatId));

        // Создаем запись
        createAppointment(chatId, masterId, dateId, timeSlotId, serviceId);

        // Уведомление для мастера
        Master master = masterRepository.findById(masterId).orElse(null);
        if (master != null && master.getChatId() != null) {
            String languageCodeMaster = userRepository.findLanguageCodeByChatId(master.getChatId());

            Users client = userRepository.findByChatId(chatId);
            String clientName = client != null ? client.getFirstName() + " " + client.getLastName() : "Unknown";

            Services service = serviceRepository.findById(serviceId).orElse(null);
            String serviceName = service != null ? messageService.getLocalizedServiceName(service, languageCodeMaster) : "Unknown";

            LocalDate appointmentDate = availableDateRepository.findById(dateId).map(AvailableDate::getDate).orElse(LocalDate.MIN);
            LocalTime appointmentTime = timeSlotRepository.findById(timeSlotId).map(TimeSlot::getTime).orElse(LocalTime.MIDNIGHT);

            // Локализуем уведомление
            String notification = messageService.getLocalizedMessage("booking.master.notification", languageCodeMaster,
                    clientName, serviceName, appointmentDate, appointmentTime);

            // Отправляем уведомление мастеру
            messageService.sendMessage(master.getChatId(), notification);
        } else {
            log.warn("Failed to notify master: Master ID {} does not have a chat ID", masterId);
        }
    }

    public void createAppointment(Long chatId, Long masterId, Long dateId, Long timeSlotId, Long serviceId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Users users = userRepository.findByChatId(chatId);
        Master master = masterRepository.findById(masterId).orElse(null);
        Services service = serviceRepository.findById(serviceId).orElse(null);
        AvailableDate date = availableDateRepository.findById(dateId).orElse(null);
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);

        if (users == null || master == null || service == null || date == null || timeSlot == null) {
            String errorMessage = messageService.getLocalizedMessage("appointment.create.error", languageCode);
            messageService.sendMessage(chatId, errorMessage);
            menuService.bookingManagerButton(chatId);
            return;
        }

        Appointment appointment = new Appointment();
        appointment.setUsers(users);
        appointment.setMaster(master);
        appointment.setServices(service);
        appointment.setAppointmentDate(LocalDateTime.of(date.getDate(), timeSlot.getTime()));
        appointment.setStatus(Appointment.Status.CONFIRMED);
        appointment.setChatId(chatId);

        appointmentRepository.save(appointment);
        timeSlot.setBooked(true);
        timeSlotRepository.save(timeSlot);

        String successMessage = messageService.getLocalizedMessage("appointment.create.success", languageCode);
        messageService.sendMessage(chatId, successMessage);
        menuService.bookingManagerButton(chatId);
    }

    public void showBookingsByStatus(Long chatId, Appointment.Status status) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список бронирований по chatId пользователя и статусу
        List<Appointment> appointments = appointmentRepository.findByChatIdAndStatus(chatId, status);

        if (appointments.isEmpty()) {
            String message = messageService.getLocalizedMessage("booking.status.notFound", languageCode, status);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сортировка и группировка по дате
        Map<LocalDate, List<Appointment>> groupedByDate = appointments.stream()
                .sorted(Comparator.comparing(Appointment::getAppointmentDate))
                .collect(Collectors.groupingBy(appointment -> appointment.getAppointmentDate().toLocalDate()));

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Отображение уникальных дат
        for (Map.Entry<LocalDate, List<Appointment>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();

            // Кнопка для уникальной даты
            InlineKeyboardButton dateButton = new InlineKeyboardButton(date.toString());
            dateButton.setCallbackData("/show_times_" + date + "_" + status); // Коллбэк для отображения слотов времени с учетом статуса
            rows.add(List.of(dateButton));
        }

        // Добавляем кнопки навигации
        List<InlineKeyboardButton> navigationButtons = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("button.back", languageCode)
        );
        backButton.setCallbackData("/back");
        navigationButtons.add(backButton);

        rows.add(navigationButtons);

        keyboard.setKeyboard(rows);

        String message = messageService.getLocalizedMessage("booking.selectDate", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showTimeSlotsForDate(Long chatId, LocalDate date, Appointment.Status status) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список всех бронирований на указанную дату и статус
        List<Appointment> appointments = appointmentRepository.findByChatIdAndStatus(chatId, status).stream()
                .filter(app -> app.getAppointmentDate().toLocalDate().equals(date))
                .sorted(Comparator.comparing(app -> app.getAppointmentDate().toLocalTime()))
                .collect(Collectors.toList());

        if (appointments.isEmpty()) {
            String message = messageService.getLocalizedMessage("timeSlots.noAvailable", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для каждого временного слота
        for (Appointment appointment : appointments) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton(appointment.getAppointmentDate().toLocalTime().toString());
            timeButton.setCallbackData("/appointment_details_" + appointment.getId()); // Коллбэк для деталей бронирования
            rows.add(List.of(timeButton));
        }

        // Добавляем кнопку "Назад"
        List<InlineKeyboardButton> navigationButtons = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("button.back", languageCode)
        );
        backButton.setCallbackData("/back");
        navigationButtons.add(backButton);
        rows.add(navigationButtons);

        userSession.setCurrentState(chatId, "/show_times_" + date + "_" + status);
        keyboard.setKeyboard(rows);

        String message = messageService.getLocalizedMessage("timeSlots.selectSlot", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showBookingDetails(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("booking.notFound", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        if (appointment.getServices() == null || appointment.getMaster() == null) {
            String message = messageService.getLocalizedMessage("booking.incomplete", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        String serviceName = messageService.getLocalizedServiceName(appointment.getServices(), languageCode);

        StringBuilder details = new StringBuilder();
        details.append(messageService.getLocalizedMessage("booking.service", languageCode)).append(serviceName).append("\n")
                .append(messageService.getLocalizedMessage("booking.master", languageCode)).append(appointment.getMaster().getName()).append("\n")
                .append(messageService.getLocalizedMessage("booking.price", languageCode)).append(appointment.getServices().getPrice()).append("€\n")
                .append(messageService.getLocalizedMessage("booking.date", languageCode)).append(appointment.getAppointmentDate().toLocalDate()).append("\n")
                .append(messageService.getLocalizedMessage("booking.time", languageCode)).append(appointment.getAppointmentDate().toLocalTime());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Если статус записи - CONFIRMED, добавляем кнопки отмены и переноса
        if (appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
            InlineKeyboardButton cancelButton = new InlineKeyboardButton(messageService.getLocalizedMessage("booking.cancel", languageCode));
            cancelButton.setCallbackData("/confirm_cancel_" + appointmentId);
            rows.add(List.of(cancelButton));

            InlineKeyboardButton transferButton = new InlineKeyboardButton(messageService.getLocalizedMessage("booking.transfer", languageCode));
            transferButton.setCallbackData("/confirm_transfer_" + appointmentId);
            rows.add(List.of(transferButton));
        }

        // Если статус записи не CONFIRMED, добавляем кнопку удаления
        if (!appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
            InlineKeyboardButton deleteButton = new InlineKeyboardButton(messageService.getLocalizedMessage("booking.delete", languageCode));
            deleteButton.setCallbackData("/confirm_delete_" + appointmentId);
            rows.add(List.of(deleteButton));
        }

        // Если статус записи - COMPLETED, добавляем кнопку для отзыва
        if (appointment.getStatus().equals(Appointment.Status.COMPLETED)) {
            InlineKeyboardButton reviewButton = new InlineKeyboardButton(messageService.getLocalizedMessage("booking.review", languageCode));
            reviewButton.setCallbackData("/confirm_review_" + appointmentId);
            rows.add(List.of(reviewButton));
        }

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton(messageService.getLocalizedMessage("button.back", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, details.toString(), keyboard);
    }

    public void confirmDeleteAppointment(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment.notFound", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        String confirmationMessage = messageService.getLocalizedMessage("appointment.confirmDelete", languageCode,
                appointment.getAppointmentDate().toLocalDate(), appointment.getAppointmentDate().toLocalTime());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(messageService.getLocalizedMessage("appointment.deleteYes", languageCode));
        yesButton.setCallbackData("/delete_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(messageService.getLocalizedMessage("appointment.deleteNo", languageCode));
        noButton.setCallbackData("/keep_appointment");

        List<InlineKeyboardButton> row = List.of(yesButton, noButton);
        keyboard.setKeyboard(List.of(row));

        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }

    public void deleteAppointment(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment.notFound", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        } else if (appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
            String message = messageService.getLocalizedMessage("appointment.deleteNotAllowed", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        LocalDateTime appointmentEndTime = appointment.getAppointmentDate();

        // Check if appointment or related dates are older than 30 days
        if (ChronoUnit.DAYS.between(appointmentEndTime, LocalDateTime.now()) >= 30) {
            AvailableDate availableDate = availableDateRepository.findByDate(appointmentEndTime.toLocalDate()).orElse(null);

            if (availableDate != null) {
                List<TimeSlot> timeSlots = availableDateService.getTimeSlotsForAvailableDate(availableDate.getId());

                // Check if all slots are unbooked and the date is 30+ days old
                boolean allUnbookedAndOld = timeSlots.stream()
                        .allMatch(ts -> !ts.isBooked()
                                && ChronoUnit.DAYS.between(availableDate.getDate().atStartOfDay(), LocalDateTime.now()) >= 30);

                if (allUnbookedAndOld) {
                    timeSlots.forEach(timeSlotRepository::delete);
                    availableDateRepository.delete(availableDate);
                } else {
                    // Additional logic for deletion based on booking status
                    handleTimeSlotDeletion(availableDate, timeSlots);
                }
            }
        }

        appointmentRepository.delete(appointment);

        String deleteMessage = messageService.getLocalizedMessage("appointment.deleted", languageCode);
        messageService.sendMessage(chatId, deleteMessage);

        clearSessionAndShowMenu(chatId);
    }

    // Метод для очистки сессии и отображения основного меню после удаления записи
    private void clearSessionAndShowMenu(Long chatId) {
        userSession.clearSession(chatId);
        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/book_info");
        userSession.setPreviousState(chatId, "/book_service");
        autUserButtons.showBookingInfoMenu(chatId);
    }

    // Вспомогательный метод для удаления временных слотов на основе статуса
    private void handleTimeSlotDeletion(AvailableDate availableDate, List<TimeSlot> timeSlots) {
        if (timeSlots.size() == 1) {
            TimeSlot singleTimeSlot = timeSlots.get(0);
            if (singleTimeSlot.isBooked()) {
                timeSlotRepository.delete(singleTimeSlot);
                availableDateRepository.delete(availableDate);
            }
        } else {
            boolean allBooked = timeSlots.stream().allMatch(TimeSlot::isBooked);

            if (allBooked) {
                timeSlots.stream()
                        .filter(ts -> ts.isBooked() && ts.getAvailableDate().equals(availableDate))
                        .findFirst()
                        .ifPresent(timeSlotRepository::delete);
            } else {
                boolean onlyUserBooked = timeSlots.stream().allMatch(ts ->
                        !ts.isBooked() || ts.getAvailableDate().equals(availableDate));

                if (onlyUserBooked) {
                    timeSlots.forEach(timeSlotRepository::delete);
                    availableDateRepository.delete(availableDate);
                }
            }
        }
    }

    public void confirmTransferAppointment(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment.notFound", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        String confirmationMessage = messageService.getLocalizedMessage("appointment.transferConfirmation", languageCode, appointment.getAppointmentDate().toLocalDate(), appointment.getAppointmentDate().toLocalTime());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.transferYes", languageCode)
        );
        yesButton.setCallbackData("/transfer_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.transferNo", languageCode)
        );
        noButton.setCallbackData("/keep_appointment");

        List<InlineKeyboardButton> row = List.of(yesButton, noButton);
        keyboard.setKeyboard(List.of(row));

        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }

    public void transferAppointment(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment.notFound", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем идентификатор записи в сессии пользователя, чтобы использовать его на следующих шагах
        userSession.setAppointmentToTransfer(chatId, appointmentId);

        // Запрашиваем выбор новой даты
        String selectDateMessage = messageService.getLocalizedMessage("appointment.selectNewDate", languageCode);
        messageService.sendMessage(chatId, selectDateMessage);

        // Показать доступные даты для этого мастера
        showAvailableDates(chatId, appointment.getMaster().getId());
    }

    // Метод для отображения доступных дат
    public void showAvailableDates(Long chatId, Long masterId) {
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
            String message = messageService.getLocalizedMessage("appointment.noAvailableDates", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        for (AvailableDate date : availableDates) {
            InlineKeyboardButton dateButton = new InlineKeyboardButton(date.getDate().toString());
            dateButton.setCallbackData("/select_transfer_date_" + date.getId());
            rows.add(List.of(dateButton));
        }

        keyboard.setKeyboard(rows);

        String selectDateMessage = messageService.getLocalizedMessage("appointment.selectDate", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, selectDateMessage, keyboard);
    }

    // Метод для обработки выбора даты и отображения доступных временных слотов
    public void handleTransferDateSelection(Long chatId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        userSession.setSelectedDate(chatId, dateId.toString());

        List<TimeSlot> timeSlots = availableDateService.getTimeSlotsForAvailableDate(dateId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (TimeSlot slot : timeSlots) {
            if (!slot.isBooked()) { // Отображаем только доступные слоты
                InlineKeyboardButton timeButton = new InlineKeyboardButton(slot.getTime().toString());
                timeButton.setCallbackData("/select_transfer_time_" + slot.getId());
                rows.add(List.of(timeButton));
            }
        }

        keyboard.setKeyboard(rows);

        // Получение локализованного сообщения
        String selectTimeMessage = messageService.getLocalizedMessage("appointment.selectTime", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, selectTimeMessage, keyboard);
    }

    // Метод для обработки выбора времени и окончательного подтверждения переноса
    public void handleTransferTimeSelection(Long chatId, Long timeSlotId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Long appointmentId = userSession.getAppointmentToTransfer(chatId);
        if (appointmentId == null) {
            String message = messageService.getLocalizedMessage("appointment.transfer.notSelected", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        TimeSlot newTimeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);

        if (appointment == null || newTimeSlot == null) {
            String message = messageService.getLocalizedMessage("appointment.transfer.error", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем выбранный timeSlotId в сессии пользователя
        userSession.setSelectedTimeSlot(chatId, String.valueOf(timeSlotId));

        // Подтверждение переноса
        String confirmationMessage = messageService.getLocalizedMessage(
                "appointment.transfer.confirmation",
                languageCode,
                newTimeSlot.getAvailableDate().getDate(),
                newTimeSlot.getTime()
        );

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.transfer.confirm", languageCode)
        );
        confirmButton.setCallbackData("/transfer_final_" + appointmentId);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.transfer.cancel", languageCode)
        );
        cancelButton.setCallbackData("/keep_appointment");

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
            String message = messageService.getLocalizedMessage("appointment.transfer.failed", languageCode);
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

        // Уведомляем пользователя о переносе
        String message = messageService.getLocalizedMessage("appointment.transfer.success", languageCode);
        messageService.sendMessage(chatId, message);
        autUserButtons.showBookingInfoMenu(chatId);

        // Уведомляем мастера о переносе
        Long masterChatId = appointment.getMaster().getChatId(); // Предполагаем, что у мастера есть поле с ID чата
        if (masterChatId != null) {
            // Получаем информацию о пользователе
            Users users = userRepository.findByChatId(chatId);
            String clientName = (users != null) ? users.getFirstName() + " " + users.getLastName() : "Unknown";

            String languageCodeMaster = userRepository.findLanguageCodeByChatId(masterChatId);
            String masterNotification = messageService.getLocalizedMessage(
                    "appointment.transfer.masterNotification",
                    languageCodeMaster,
                    clientName,
                    appointment.getServices(),
                    newTimeSlot.getAvailableDate().getDate(),
                    newTimeSlot.getTime()
            );

            messageService.sendMessage(masterChatId, masterNotification);
        }
    }

    public void confirmCancelAppointment(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null || !appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
            String message = messageService.getLocalizedMessage("appointment.cancel.notAvailable", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сообщение с подтверждением отмены
        String confirmationMessage = messageService.getLocalizedMessage(
                "appointment.cancel.confirmation",
                languageCode,
                appointment.getAppointmentDate().toLocalDate(),
                appointment.getAppointmentDate().toLocalTime()
        );

        // Создание inline-кнопок для подтверждения или отмены
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.cancel.yes", languageCode)
        );
        yesButton.setCallbackData("/cancel_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.cancel.no", languageCode)
        );
        noButton.setCallbackData("/keep_appointment");

        List<InlineKeyboardButton> row = List.of(yesButton, noButton);
        keyboard.setKeyboard(List.of(row));

        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }

    public void cancelAppointment(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null || !appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
            String message = messageService.getLocalizedMessage("appointment.cancel.notAvailable", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Обновляем статус записи на "CANCELLED"
        appointment.setStatus(Appointment.Status.CANCELLED);
        appointmentRepository.save(appointment);

        // Обновляем статус слота времени
        TimeSlot timeSlot = timeSlotRepository.findByTimeAndMasterId(
                appointment.getAppointmentDate().toLocalTime(),
                appointment.getMaster().getId()
        );
        if (timeSlot != null) {
            timeSlot.setBooked(false);
            timeSlotRepository.save(timeSlot);
        }

        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/book_info");
        userSession.setPreviousState(chatId, "/book_service");

        String successMessage = messageService.getLocalizedMessage("appointment.cancel.success", languageCode);
        messageService.sendMessage(chatId, successMessage);

        autUserButtons.showBookingInfoMenu(chatId);

        // Отправка уведомления мастеру
        Long masterChatId = appointment.getMaster().getChatId();
        if (masterChatId != null) {
            String clientName = userRepository.findByChatId(chatId).getFirstName();
            String appointmentDate = appointment.getAppointmentDate().toLocalDate().toString();
            String appointmentTime = appointment.getAppointmentDate().toLocalTime().toString();

            String languageCodeMaster = userRepository.findLanguageCodeByChatId(masterChatId);
            String masterNotification = messageService.getLocalizedMessage("appointment.cancel.masterNotification", languageCodeMaster, clientName, appointmentDate, appointmentTime);

            messageService.sendMessage(masterChatId, masterNotification);
        }
    }
}
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
        messageService.sendMessage(chatId, "ru".equals(languageCode)
                ? "Выберите мастера для записи на прием."
                : "uk".equals(languageCode)
                ? "Оберіть майстра для запису на прийом."
                : "Choose a master to book an appointment.");

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
                String message = "ru".equals(languageCode)
                        ? "У мастера " + master.getName() + " в настоящее время нет доступных дат. Пожалуйста, свяжитесь с ним по телефону: " + master.getPhoneNumber()
                        : "uk".equals(languageCode)
                        ? "У майстра " + master.getName() + " зараз немає доступних дат. Будь ласка, зв'яжіться з ним за телефоном: " + master.getPhoneNumber()
                        : "Master " + master.getName() + " currently has no available dates. Please contact them at: " + master.getPhoneNumber();

                messageService.sendMessage(chatId, message);
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
            socialButton.setText("ru".equals((languageCode)) ? "Социальная сеть"
                    : "uk".equals(languageCode) ? "Соціальна мережа" : "Social Link");
            socialButton.setUrl(master.getSocialLink());
            buttons.add(socialButton);

            rowsInline.add(buttons);
            keyboardMarkup.setKeyboard(rowsInline);

            // Отправляем описание мастера вместе с клавиатурой
            messageService.sendMessageWithInlineKeyboard(chatId, master.getDescription(), keyboardMarkup);
        }

        userSession.clearStates(chatId);
        userSession.setCurrentState(chatId, "/book_service");
        userSession.setPreviousState(chatId, "/menu");
    }

    public void selectDate(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String masterIdString = userSession.getSelectedMaster(chatId);
        if (masterIdString == null) {
            messageService.sendMessage(chatId,
                    "ru".equals(languageCode)
                            ? "Сначала вам нужно выбрать мастера."
                            : "uk".equals(languageCode)
                            ? "Спочатку вам потрібно обрати майстра."
                            : "You need to select a master first."
            );
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
            String message = "ru".equals(languageCode)
                    ? "Нет доступных дат для выбранного мастера."
                    : "uk".equals(languageCode)
                    ? "Немає доступних дат для обраного майстра."
                    : "No available dates for the selected master.";

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

        String message = "ru".equals(languageCode)
                ? "Выберите дату:"
                : "uk".equals(languageCode)
                ? "Оберіть дату:"
                : "Select a date:";

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

    }

    public void selectTime(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String dateIdString = userSession.getSelectedDate(chatId);
        if (dateIdString == null) {
            String message = "ru".equals(languageCode)
                    ? "Сначала нужно выбрать дату."
                    : "uk".equals(languageCode)
                    ? "Спочатку потрібно обрати дату."
                    : "You need to select a date first.";

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

        String message = "ru".equals(languageCode)
                ? "Выберите время:"
                : "uk".equals(languageCode)
                ? "Оберіть час:"
                : "Select a time:";

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void selectService(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        String masterIdString = userSession.getSelectedMaster(chatId);
        if (masterIdString == null) {
            String message = "ru".equals(languageCode)
                    ? "Сначала нужно выбрать мастера."
                    : "uk".equals(languageCode)
                    ? "Спочатку потрібно обрати майстра."
                    : "You need to select a master first.";

            messageService.sendMessage(chatId, message);
            return;
        }

        Long masterId = Long.valueOf(masterIdString);
        List<Services> services = serviceRepository.findByMasterId(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Кнопка для выбора услуги с названием и ценой
            InlineKeyboardButton selectButton = new InlineKeyboardButton("ru".equals(languageCode) ? service.getNameRu() :
                    "uk".equals(languageCode) ? service.getNameUk() :
                            service.getNameEn());
            selectButton.setCallbackData("/select_service_" + service.getId());

            InlineKeyboardButton priceButton = new InlineKeyboardButton(service.getPrice() + "€ ↑ ↑");
            priceButton.setCallbackData("/select_service_" + service.getId());

            // Кнопка для отображения описания услуги
            InlineKeyboardButton descriptionButton = new InlineKeyboardButton(
                    "ru".equals(languageCode)
                            ? "↑ ↑ Описание"
                            : "uk".equals(languageCode)
                            ? "↑ ↑ Опис"
                            : "↑ ↑ Description"
            );
            descriptionButton.setCallbackData("/show_description_" + service.getId());

            // Добавляем обе кнопки на одну строку
            rows.add(List.of(selectButton));
            rows.add(List.of(descriptionButton, priceButton));
        }

        keyboard.setKeyboard(rows);
        String message = "ru".equals(languageCode)
                ? "Выберите услугу:"
                : "uk".equals(languageCode)
                ? "Оберіть послугу:"
                : "Select a service:";

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
            String message = "ru".equals(languageCode)
                    ? "Произошла ошибка при получении данных бронирования. Пожалуйста, попробуйте еще раз."
                    : "uk".equals(languageCode)
                    ? "Виникла помилка під час отримання даних для бронювання. Будь ласка, спробуйте ще раз."
                    : "An error occurred while retrieving booking details. Please try again.";

            messageService.sendMessage(chatId, message);
            return;
        }

        // Show booking details for confirmation
        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы собираетесь записаться на прием к:\n" +
                "Мастер: " + master.getName() + "\n" +
                "Услуга: " + service.getNameRu() + " - " + service.getPrice() + "€\n" +
                "Дата: " + date.getDate() + "\n" +
                "Время: " + timeSlot.getTime() + "\n\n" +
                "Вы уверены, что хотите подтвердить эту запись?"
                : "uk".equals(languageCode)
                ? "Ви збираєтеся записатися на прийом до:\n" +
                "Майстер: " + master.getName() + "\n" +
                "Послуга: " + service.getNameUk() + " - " + service.getPrice() + "€\n" +
                "Дата: " + date.getDate() + "\n" +
                "Час: " + timeSlot.getTime() + "\n\n" +
                "Ви впевнені, що хочете підтвердити цей запис?"
                : "You are about to book an appointment with:\n" +
                "Master: " + master.getName() + "\n" +
                "Service: " + service.getNameEn() + " - " + service.getPrice() + "€\n" +
                "Date: " + date.getDate() + "\n" +
                "Time: " + timeSlot.getTime() + "\n\n" +
                "Are you sure you want to confirm this booking?";

        // Inline keyboard with Yes and No options
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton("ru".equals(languageCode)
                ? "Да"
                : "uk".equals(languageCode)
                ? "Так"
                : "Yes");
        yesButton.setCallbackData("/confirm_appointment"); // callback for confirmation

        InlineKeyboardButton noButton = new InlineKeyboardButton("ru".equals(languageCode)
                ? "Нет"
                : "uk".equals(languageCode)
                ? "Ні"
                : "No");
        noButton.setCallbackData("/cancel_appointment"); // callback for cancellation

        List<InlineKeyboardButton> row = List.of(yesButton, noButton);
        keyboard.setKeyboard(List.of(row));

        // Send confirmation message with inline keyboard
        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }

    public void finalizeBooking(Long chatId) {
        Long masterId = Long.valueOf(userSession.getSelectedMaster(chatId));
        Long dateId = Long.valueOf(userSession.getSelectedDate(chatId));
        Long timeSlotId = Long.valueOf(userSession.getSelectedTimeSlot(chatId));
        Long serviceId = Long.valueOf(userSession.getSelectedService(chatId));

        createAppointment(chatId, masterId, dateId, timeSlotId, serviceId);

        // Отправка уведомления мастеру
        Master master = masterRepository.findById(masterId).orElse(null);
        if (master != null && master.getChatId() != null) {
            String languageCodeMaster = userRepository.findLanguageCodeByChatId(master.getChatId());

            Users users = userRepository.findByChatId(chatId);
            String clientName = users != null ? users.getFirstName() + " " + users.getLastName() : "Unknown";
            String serviceName = serviceRepository.findById(serviceId)
                    .map(service -> {
                        if ("ru".equals(languageCodeMaster)) {
                            return service.getNameRu();
                        } else if ("uk".equals(languageCodeMaster)) {
                            return service.getNameUk();
                        } else {
                            return service.getNameEn();
                        }
                    })
                    .orElse("Unknown");
            LocalDate appointmentDate = availableDateRepository.findById(dateId).map(AvailableDate::getDate).orElse(LocalDate.MIN);
            LocalTime appointmentTime = timeSlotRepository.findById(timeSlotId).map(TimeSlot::getTime).orElse(LocalTime.MIDNIGHT);

            String notification = "ru".equals(languageCodeMaster)
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
            String message = "ru".equals(languageCode)
                    ? "Не удалось создать запись. Недействительные данные."
                    : "uk".equals(languageCode)
                    ? "Не вдалося створити запис. Недійсні дані."
                    : "Failed to create appointment. Invalid data.";

            messageService.sendMessage(chatId, message);
            menuService.bookingManagerButton(chatId, messageService);
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

        String message = "ru".equals(languageCode)
                ? "Запись успешно создана!"
                : "uk".equals(languageCode)
                ? "Запис успішно створено!"
                : "Appointment successfully booked!";

        messageService.sendMessage(chatId, message);
        menuService.bookingManagerButton(chatId, messageService);
    }

    public void showBookingsByStatus(Long chatId, Appointment.Status status) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем список бронирований по chatId пользователя и статусу
        List<Appointment> appointments = appointmentRepository.findByChatIdAndStatus(chatId, status);

        if (appointments.isEmpty()) {
            String message = "ru".equals(languageCode)
                    ? "Бронирования со статусом: " + status + " не найдены."
                    : "uk".equals(languageCode)
                    ? "Бронювання зі статусом: " + status + " не знайдено."
                    : "No bookings found with status: " + status;

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
                "ru".equals(languageCode)
                        ? "Назад"
                        : "uk".equals(languageCode)
                        ? "Назад"
                        : "Back"
        );
        backButton.setCallbackData("/back");
        navigationButtons.add(backButton);

        rows.add(navigationButtons);

        keyboard.setKeyboard(rows);
        String message = "ru".equals(languageCode)
                ? "Выберите дату, чтобы просмотреть доступные временные слоты:"
                : "uk".equals(languageCode)
                ? "Оберіть дату, щоб переглянути доступні часові слоти:"
                : "Select a date to view available time slots:";

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

        // Создаем кнопки для каждого временного слота
        for (Appointment appointment : appointments) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton(appointment.getAppointmentDate().toLocalTime().toString());
            timeButton.setCallbackData("/appointment_details_" + appointment.getId()); // Коллбэк для деталей бронирования
            rows.add(List.of(timeButton));
        }

        List<InlineKeyboardButton> navigationButtons = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton("ru".equals(languageCode)
                ? "Назад"
                : "uk".equals(languageCode)
                ? "Назад"
                : "Back");
        backButton.setCallbackData("/back");
        navigationButtons.add(backButton);
        rows.add(navigationButtons);


        userSession.setCurrentState(chatId, "/show_times_" + date + "_" + status);
        keyboard.setKeyboard(rows);
        String message = "ru".equals(languageCode)
                ? "Выберите временной слот, чтобы просмотреть детали бронирования:"
                : "uk".equals(languageCode)
                ? "Оберіть часовий слот, щоб переглянути деталі бронювання:"
                : "Select a time slot to view booking details:";

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showBookingDetails(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = "ru".equals(languageCode)
                    ? "Бронирование не найдено."
                    : "uk".equals(languageCode)
                    ? "Бронювання не знайдено."
                    : "Booking not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        if (appointment.getServices() == null || appointment.getMaster() == null) {
            String message = "ru".equals(languageCode)
                    ? "Неполные данные бронирования."
                    : "uk".equals(languageCode)
                    ? "Неповні дані бронювання."
                    : "Incomplete booking details.";
            messageService.sendMessage(chatId, message);
            return;
        }

        String serviceName = "ru".equals(languageCode)
                ? appointment.getServices().getNameRu()
                : "uk".equals(languageCode)
                ? appointment.getServices().getNameUk()
                : appointment.getServices().getNameEn();

        StringBuilder details = new StringBuilder();
        details.append("ru".equals(languageCode)
                        ? "Услуга: "
                        : "uk".equals(languageCode)
                        ? "Послуга: "
                        : "Service: ")
                .append(serviceName).append("\n")
                .append("ru".equals(languageCode)
                        ? "Мастер: "
                        : "uk".equals(languageCode)
                        ? "Майстер: "
                        : "Master: ")
                .append(appointment.getMaster().getName()).append("\n")
                .append("ru".equals(languageCode)
                        ? "Цена: "
                        : "uk".equals(languageCode)
                        ? "Ціна: "
                        : "Price: ")
                .append(appointment.getServices().getPrice()).append("€\n")
                .append("ru".equals(languageCode)
                        ? "Дата: "
                        : "uk".equals(languageCode)
                        ? "Дата: "
                        : "Date: ")
                .append(appointment.getAppointmentDate().toLocalDate()).append("\n")
                .append("ru".equals(languageCode)
                        ? "Время: "
                        : "uk".equals(languageCode)
                        ? "Час: "
                        : "Time: ")
                .append(appointment.getAppointmentDate().toLocalTime());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

// Если статус записи - CONFIRMED, добавляем кнопки отмены и переноса
        if (appointment.getStatus() == Appointment.Status.CONFIRMED) {
            InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                    "ru".equals(languageCode)
                            ? "Отменить запись"
                            : "uk".equals(languageCode)
                            ? "Скасувати запис"
                            : "Cancel Appointment"
            );
            cancelButton.setCallbackData("/confirm_cancel_" + appointmentId);
            rows.add(List.of(cancelButton));

            InlineKeyboardButton transferButton = new InlineKeyboardButton(
                    "ru".equals(languageCode)
                            ? "Перенести запись"
                            : "uk".equals(languageCode)
                            ? "Перенести запис"
                            : "Transfer Appointment"
            );
            transferButton.setCallbackData("/confirm_transfer_" + appointmentId);
            rows.add(List.of(transferButton));
        }

// Если статус записи не CONFIRMED, добавляем кнопку удаления
        if (appointment.getStatus() != Appointment.Status.CONFIRMED) {
            InlineKeyboardButton deleteButton = new InlineKeyboardButton(
                    "ru".equals(languageCode)
                            ? "Удалить запись"
                            : "uk".equals(languageCode)
                            ? "Видалити запис"
                            : "Delete Appointment"
            );
            deleteButton.setCallbackData("/confirm_delete_" + appointmentId);
            rows.add(List.of(deleteButton));
        }

// Если статус записи - COMPLETED, добавляем кнопку для отзыва
        if (appointment.getStatus() == Appointment.Status.COMPLETED) {
            InlineKeyboardButton reviewButton = new InlineKeyboardButton(
                    "ru".equals(languageCode)
                            ? "Оставить отзыв"
                            : "uk".equals(languageCode)
                            ? "Залишити відгук"
                            : "Review Appointment"
            );
            reviewButton.setCallbackData("/confirm_review_" + appointmentId);
            rows.add(List.of(reviewButton));
        }

// Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                "ru".equals(languageCode)
                        ? "Назад"
                        : "uk".equals(languageCode)
                        ? "Назад"
                        : "Back"
        );
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, details.toString(), keyboard);
    }

    public void confirmDeleteAppointment(Long chatId, Long appointmentId) {
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

        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы уверены, что хотите удалить эту запись: "
                + appointment.getAppointmentDate().toLocalDate() + " "
                + appointment.getAppointmentDate().toLocalTime() + "?"
                : "uk".equals(languageCode)
                ? "Ви впевнені, що хочете видалити цей запис: "
                + appointment.getAppointmentDate().toLocalDate() + " "
                + appointment.getAppointmentDate().toLocalTime() + "?"
                : "Are you sure you want to delete this appointment: "
                + appointment.getAppointmentDate().toLocalDate() + " "
                + appointment.getAppointmentDate().toLocalTime() + "?";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                "ru".equals(languageCode)
                        ? "Да, удалить запись"
                        : "uk".equals(languageCode)
                        ? "Так, видалити запис"
                        : "Yes, Delete Appointment"
        );
        yesButton.setCallbackData("/delete_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                "ru".equals(languageCode)
                        ? "Нет, оставить"
                        : "uk".equals(languageCode)
                        ? "Ні, залишити"
                        : "No, keep"
        );
        noButton.setCallbackData("/keep_appointment");

        List<InlineKeyboardButton> row = List.of(yesButton, noButton);
        keyboard.setKeyboard(List.of(row));

        messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
    }

    public void deleteAppointment(Long chatId, Long appointmentId) {
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
        } else if (appointment.getStatus() == Appointment.Status.CONFIRMED) {
            String message = "ru".equals(languageCode)
                    ? "Вы можете удалять записи со статусом ЗАВЕРШЕНО или ОТМЕНЕНО."
                    : "uk".equals(languageCode)
                    ? "Ви можете видаляти записи зі статусом ЗАВЕРШЕНО або СКАСОВАНО."
                    : "You can delete appointments with status COMPLETED or CANCELLED.";
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

        String deleteMessage = "ru".equals(languageCode)
                ? "Запись удалена."
                : "uk".equals(languageCode)
                ? "Запис видалено."
                : "Appointment deleted.";
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
            String message = "ru".equals(languageCode)
                    ? "Запись не найдена."
                    : "uk".equals(languageCode)
                    ? "Запис не знайдено."
                    : "Appointment not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы уверены, что хотите перенести запись на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + "?"
                : "uk".equals(languageCode)
                ? "Ви впевнені, що хочете перенести запис на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + "?"
                : "Are you sure you want to transfer the appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + "?";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Да, перенести запись" : "uk".equals(languageCode) ? "Так, перенести запис" : "Yes, Transfer Appointment"
        );
        yesButton.setCallbackData("/transfer_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Нет, оставить" : "uk".equals(languageCode) ? "Ні, залишити" : "No, keep"
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
            dateButton.setCallbackData("/select_transfer_date_" + date.getId());
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
        String selectTimeMessage = "ru".equals(languageCode)
                ? "Выберите время:"
                : "uk".equals(languageCode)
                ? "Оберіть час:"
                : "Select a time:";
        messageService.sendMessageWithInlineKeyboard(chatId, selectTimeMessage, keyboard);
    }

    // Метод для обработки выбора времени и окончательного подтверждения переноса
    public void handleTransferTimeSelection(Long chatId, Long timeSlotId) {
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
                ? "Вы переносите запись на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nВремя: " + newTimeSlot.getTime() + "\n\nПодтвердить перенос?"
                : "uk".equals(languageCode)
                ? "Ви переносите запис на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nЧас: " + newTimeSlot.getTime() + "\n\nПідтвердити перенесення?"
                : "You are transferring the appointment to:\nDate: " + newTimeSlot.getAvailableDate().getDate() + "\nTime: " + newTimeSlot.getTime() + "\n\nConfirm transfer?";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Подтвердить перенос" : "uk".equals(languageCode) ? "Підтвердити перенесення" : "Confirm Transfer"
        );
        confirmButton.setCallbackData("/transfer_final_" + appointmentId);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Отменить перенос" : "uk".equals(languageCode) ? "Скасувати перенесення" : "Cancel Transfer"
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

        // Уведомляем пользователя о переносе
        String message = "ru".equals(languageCode)
                ? "Запись успешно перенесена на новую дату и время."
                : "uk".equals(languageCode)
                ? "Запис успішно перенесено на нову дату і час."
                : "Appointment successfully transferred to the new date and time.";
        messageService.sendMessage(chatId, message);
        autUserButtons.showBookingInfoMenu(chatId);

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
                    " перенесена на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nВремя: " + newTimeSlot.getTime()
                    : "uk".equals(languageCodeMaster)
                    ? "Запис для клієнта " + clientName +
                    " на послугу " + appointment.getServices().getNameUk() +
                    " перенесено на:\nДата: " + newTimeSlot.getAvailableDate().getDate() + "\nЧас: " + newTimeSlot.getTime()
                    : "The appointment for " + appointment.getServices().getNameEn() +
                    " with client " + clientName +
                    " has been rescheduled to:\nDate: " + newTimeSlot.getAvailableDate().getDate() + "\nTime: " + newTimeSlot.getTime();

            messageService.sendMessage(masterChatId, masterNotification);
        }
    }

    public void confirmCancelAppointment(Long chatId, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null || !appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
            String message = "ru".equals(languageCode)
                    ? "Выбранная запись недоступна для отмены."
                    : "uk".equals(languageCode)
                    ? "Обраний запис недоступний для скасування."
                    : "The selected appointment is not available for cancellation.";
            messageService.sendMessage(chatId, message);
            return;
        }

// Сообщение с подтверждением отмены
        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы уверены, что хотите отменить запись на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + "?"
                : "uk".equals(languageCode)
                ? "Ви впевнені, що хочете скасувати запис на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + "?"
                : "Are you sure you want to cancel the appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + "?";

// Создание inline-кнопок для подтверждения или отмены
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Да, отменить" : "uk".equals(languageCode) ? "Так, скасувати" : "Yes, cancel"
        );
        yesButton.setCallbackData("/cancel_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Нет, оставить" : "uk".equals(languageCode) ? "Ні, залишити" : "No, keep"
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
            String message = "ru".equals(languageCode)
                    ? "Запись недоступна для отмены."
                    : "uk".equals(languageCode)
                    ? "Запис недоступний для скасування."
                    : "The appointment is not available for cancellation.";
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

        String successMessage = "ru".equals(languageCode)
                ? "Ваша запись успешно отменена."
                : "uk".equals(languageCode)
                ? "Ваш запис успішно скасовано."
                : "Your appointment has been successfully canceled.";
        messageService.sendMessage(chatId, successMessage);

        autUserButtons.showBookingInfoMenu(chatId);

        // Отправка уведомления мастеру
        Long masterChatId = appointment.getMaster().getChatId();
        if (masterChatId != null) {
            String clientName = userRepository.findByChatId(chatId).getFirstName();
            String appointmentDate = appointment.getAppointmentDate().toLocalDate().toString();
            String appointmentTime = appointment.getAppointmentDate().toLocalTime().toString();

            String languageCodeMaster = userRepository.findLanguageCodeByChatId(masterChatId);
            String masterNotification = "ru".equals(languageCodeMaster)
                    ? "Клиент " + clientName + " отменил запись на " + appointmentDate + " в " + appointmentTime + "."
                    : "uk".equals(languageCodeMaster)
                    ? "Клієнт " + clientName + " скасував запис на " + appointmentDate + " о " + appointmentTime + "."
                    : "Client " + clientName + " has canceled the appointment on " + appointmentDate + " at " + appointmentTime + ".";

            messageService.sendMessage(masterChatId, masterNotification);
        }
    }

}

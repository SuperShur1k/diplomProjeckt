package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.model.AvailableDate;
import com.example.telegramBotNailsBooking.model.Master;
import com.example.telegramBotNailsBooking.model.TimeSlot;
import com.example.telegramBotNailsBooking.model.UserSession;
import com.example.telegramBotNailsBooking.repository.AvailableDateRepository;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import com.example.telegramBotNailsBooking.repository.TimeSlotRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.buttons.AdminButtons;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvailableDateService {
    private static final Logger log = LoggerFactory.getLogger(AvailableDateService.class);

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AvailableDateRepository availableDateRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private AdminButtons adminButtons;

    @Autowired
    private AutUserButtons autUserButtons;

    @Autowired
    private UserRepository userRepository;

    public List<AvailableDate> getAvailableDatesForMaster(Long masterId) {
        return availableDateRepository.findByMasterId(masterId);
    }

    public Optional<AvailableDate> findAvailableDateByMasterAndDate(Long masterId, LocalDate date) {
        return availableDateRepository.findByMasterIdAndDate(masterId, date);
    }

    public void addAvailableDate(AvailableDate availableDate) {
        availableDateRepository.save(availableDate);
    }

    @Transactional
    public void deleteAvailableDate(AvailableDate availableDate) {
        timeSlotRepository.deleteAllByAvailableDateId(availableDate.getId()); // Удаление связанных временных слотов
        availableDateRepository.delete(availableDate); // Удаление даты
    }


    public List<TimeSlot> getTimeSlotsForAvailableDate(Long availableDateId) {
        return timeSlotRepository.findTimeSlotsByAvailableDateId(availableDateId);
    }

    public String addTimeSlot(Long availableDateId, LocalTime time, Master master, Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        AvailableDate availableDate = availableDateRepository.findById(availableDateId).orElse(null);
        if (availableDate == null) {
            if ("ru".equals(languageCode)) {
                return "Дата не найдена.";
            } else if ("uk".equals(languageCode)) {
                return "Дата не знайдена.";
            } else {
                return "Available date not found.";
            }
        }

        if (master == null) {
            if ("ru".equals(languageCode)) {
                return "Мастер не найден.";
            } else if ("uk".equals(languageCode)) {
                return "Майстра не знайдено.";
            } else {
                return "Master not found.";
            }
        }

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setAvailableDate(availableDate);
        timeSlot.setMaster(master);
        timeSlot.setTime(time);
        timeSlot.setBooked(false);

        timeSlotRepository.save(timeSlot);

        if ("ru".equals(languageCode)) {
            return "Временной слот успешно добавлен.";
        } else if ("uk".equals(languageCode)) {
            return "Часовий слот успішно додано.";
        } else {
            return "Time slot added successfully.";
        }
    }

    public List<AvailableDate> getAvailableDatesForMasterWithSlots(Long masterId) {
        // Получаем все доступные даты для мастера
        List<AvailableDate> dates = availableDateRepository.findByMasterId(masterId);

        // Фильтруем даты, чтобы оставить только те, у которых есть незабронированные временные слоты
        return dates.stream()
                .filter(date -> timeSlotRepository.findTimeSlotsByAvailableDateId(date.getId()).stream()
                        .anyMatch(slot -> !slot.isBooked()))
                .collect(Collectors.toList());
    }

    public void initiateAddDate(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Получаем список всех мастеров
        List<Master> masters = masterRepository.findAll();

        if (masters.isEmpty()) {
            // Если мастеров нет, отправляем сообщение пользователю
            String noMastersMessage = "ru".equals(languageCode)
                    ? "Нет доступных мастеров для добавления даты."
                    : "uk".equals(languageCode)
                    ? "Немає доступних майстрів для додавання дати."
                    : "No available masters to add a date.";
            messageService.sendMessage(chatId, noMastersMessage);
            return;
        }

        // Формируем сообщение в зависимости от языка
        String message = "ru".equals(languageCode)
                ? "Выберите мастера для добавления даты:"
                : "uk".equals(languageCode)
                ? "Оберіть майстра для додавання дати:"
                : "Select a master to add a date:";

        // Создаем кнопки для выбора мастера
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName()); // Имя мастера
            button.setCallbackData("/master_date_" + master.getId()); // Callback с ID мастера
            rows.add(List.of(button));
        }

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        markup.setKeyboard(rows);

        // Отправляем сообщение с кнопками
        messageService.sendMessageWithInlineKeyboard(chatId, message, markup);

        // Инициализация сессии
        userSession.setDateInfo(chatId, new String[2]); // Массив для хранения ID мастера и даты
        userSession.setPreviousState(chatId, "/add_date");
    }

    public void handleAddDateInput(Long chatId, String input) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        String[] dateInfo = userSession.getDateInfo(chatId);

        if (dateInfo == null) {
            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, начните процесс добавления даты снова, используя команду."
                    : "uk".equals(languageCode)
                    ? "Будь ласка, почніть процес додавання дати знову, використовуючи команду."
                    : "Please start the add date process again by using the command.";
            messageService.sendMessage(chatId, message);
            return;
        }

        if (input.startsWith("/master_date_")) {
            // Обработка выбора мастера
            String masterId = input.split("_")[2];
            dateInfo[0] = masterId; // Сохраняем ID мастера

            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, введите дату в формате YYYY-MM-DD:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, введіть дату у форматі YYYY-MM-DD:"
                    : "Please enter the date in format YYYY-MM-DD:";
            messageService.sendMessage(chatId, message);
            return;
        }

        if (dateInfo[1] == null) {
            // Step 2: Ввод даты
            if (!input.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String message = "ru".equals(languageCode)
                        ? "Неверный формат даты. Пожалуйста, введите корректную дату в формате YYYY-MM-DD:"
                        : "uk".equals(languageCode)
                        ? "Невірний формат дати. Будь ласка, введіть правильну дату у форматі YYYY-MM-DD:"
                        : "Invalid date format. Please enter a valid date in format YYYY-MM-DD:";
                messageService.sendMessage(chatId, message);
                return;
            }
            LocalDate date;

            try {
                date = LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                String message = "ru".equals(languageCode)
                        ? "Неверный формат даты. Пожалуйста, введите корректную дату в формате YYYY-MM-DD:"
                        : "uk".equals(languageCode)
                        ? "Невірний формат дати. Будь ласка, введіть правильну дату у форматі YYYY-MM-DD:"
                        : "Invalid date format. Please enter a valid date in format YYYY-MM-DD:";
                messageService.sendMessage(chatId, message);
                return;
            }

            // Проверка на добавление даты в прошлом
            if (date.isBefore(LocalDate.now())) {
                String message = "ru".equals(languageCode)
                        ? "Вы не можете добавить дату в прошлом. Пожалуйста, введите будущую дату."
                        : "uk".equals(languageCode)
                        ? "Ви не можете додати дату у минулому. Будь ласка, введіть майбутню дату."
                        : "You cannot add a date in the past. Please enter a future date.";
                messageService.sendMessage(chatId, message);
                return;
            }

            // Check if the date already exists for this master
            Long masterId = Long.parseLong(dateInfo[0]);
            if (findAvailableDateByMasterAndDate(masterId, date).isPresent()) {
                String message = "ru".equals(languageCode)
                        ? "Эта дата уже существует для мастера. Пожалуйста, введите другую дату или отмените процесс."
                        : "uk".equals(languageCode)
                        ? "Ця дата вже існує для майстра. Будь ласка, введіть іншу дату або скасуйте процес."
                        : "This date already exists for the master. Please enter a different date or cancel the process.";
                messageService.sendMessage(chatId, message);
            } else {
                dateInfo[1] = date.toString();

                // Save the available date
                AvailableDate availableDate = new AvailableDate();
                availableDate.setDate(date);
                availableDate.setMaster(masterRepository.findById(masterId).orElse(null));

                addAvailableDate(availableDate);
                String message = "ru".equals(languageCode)
                        ? "Дата успешно добавлена. Теперь вы можете добавить временные слоты для этой даты."
                        : "uk".equals(languageCode)
                        ? "Дата успішно додана. Тепер ви можете додати час для цієї дати."
                        : "Date successfully added. You can now add time slots to this date.";

                messageService.sendMessage(chatId, message);

                userSession.clearDateInfo(chatId);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
                userSession.setCurrentState(chatId, "/date");
                userSession.setPreviousState(chatId, "/admin");
                adminButtons.getDateInlineKeyboard(chatId, messageService);
            }
        }
    }

    public void initiateAddTime(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя

        String message = "ru".equals(languageCode)
                ? "Пожалуйста, выберите мастера, для которого хотите добавить временной слот:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть майстра, для якого хочете додати часовий слот:"
                : "Please select the master for whom you want to add a time slot:";

        List<Master> masters = masterRepository.findAll(); // Получаем список мастеров
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName()); // Устанавливаем имя мастера
            button.setCallbackData("/master_time_" + master.getId()); // Устанавливаем ID мастера в callback
            rows.add(List.of(button));
        }

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        userSession.setTimeInfo(chatId, new String[3]); // Инициализация массива [мастер, дата, время]
        userSession.setPreviousState(chatId, "/add_time");
    }

    public void handleAddTimeInput(Long chatId, String input) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Язык пользователя
        String[] timeInfo = userSession.getTimeInfo(chatId);

        if (timeInfo == null) {
            messageService.sendMessage(chatId, "ru".equals(languageCode)
                    ? "Пожалуйста, начните процесс добавления времени снова."
                    : "uk".equals(languageCode)
                    ? "Будь ласка, почніть процес додавання часу знову."
                    : "Please start the add time process again.");
            return;
        }

        if (input.startsWith("/master_time_")) {
            String masterId = input.split("_")[2]; // Извлекаем ID мастера
            timeInfo[0] = masterId; // Сохраняем ID мастера
            userSession.setTimeInfo(chatId, timeInfo);

            log.info("Master ID {} успешно сохранен для чата {}", masterId, chatId);

            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, выберите дату из доступных или введите вручную в формате YYYY-MM-DD:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, оберіть дату з доступних або введіть вручну у форматі YYYY-MM-DD:"
                    : "Please select a date from the available options or enter it manually in format YYYY-MM-DD:";

            List<AvailableDate> dates = availableDateRepository.findByMasterId(Long.valueOf(masterId)).stream()
                    .filter(date -> !date.getDate().isBefore(LocalDate.now())) // Remove past dates
                    .sorted(Comparator.comparing(AvailableDate::getDate)) // Sort from nearest to farthest
                    .collect(Collectors.toList());

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            DateTimeFormatter formatter = "ru".equals(languageCode)
                    ? DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("ru"))
                    : "uk".equals(languageCode)
                    ? DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("uk"))
                    : DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH);

            for (AvailableDate date : dates) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(date.getDate().format(formatter)); // Форматируем дату для пользователя
                button.setCallbackData("/date_time_" + date.getDate());
                rows.add(List.of(button));
            }

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);
            messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
            return;
        }

        if (input.startsWith("/date_time_")) {
            String selectedDate = input.split("_")[2];
            timeInfo[1] = selectedDate;
            userSession.setTimeInfo(chatId, timeInfo);

            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, введите время для слота в формате HH:MM:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, введіть час для слота у форматі HH:MM:"
                    : "Please enter the time for the slot in format HH:MM:";
            messageService.sendMessage(chatId, message);
            return;
        }

        if (timeInfo[2] == null) {
            if (!input.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Неверный формат времени. Пожалуйста, введите время в формате HH:MM (от 00:00 до 23:59):"
                        : "uk".equals(languageCode)
                        ? "Невірний формат часу. Будь ласка, введіть час у форматі HH:MM (від 00:00 до 23:59):"
                        : "Invalid time format. Please enter time in HH:MM format (between 00:00 and 23:59):");
                return;
            }

            LocalTime time = LocalTime.parse(input);
            timeInfo[2] = time.toString(); // Сохраняем время
            saveTimeSlot(chatId, timeInfo);
        }
    }

    private void saveTimeSlot(Long chatId, String[] timeInfo) {
        String userLanguage = userRepository.findLanguageCodeByChatId(chatId);
        if (timeInfo[0] == null || timeInfo[1] == null || timeInfo[2] == null) {
            log.error("Некорректные данные timeInfo: {}", Arrays.toString(timeInfo));
            messageService.sendMessage(chatId, userLanguage.equals("ru") ? "Ошибка: данные для сохранения слота некорректны." :
                    userLanguage.equals("en") ? "Error: Data for saving the slot is incorrect." :
                            "Помилка: дані для збереження слота некоректні.");
            return;
        }

        try {
            Long masterId = Long.valueOf(timeInfo[0]);
            LocalDate date = LocalDate.parse(timeInfo[1]);
            LocalTime time = LocalTime.parse(timeInfo[2]);

            log.info("Сохранение слота: мастер={}, дата={}, время={}", masterId, date, time);

            Master master = masterRepository.findById(masterId).orElseThrow(() -> new RuntimeException( userLanguage.equals("ru") ? "Мастер не найден" :
                    userLanguage.equals("en") ? "Master not found" :
                            "Майстра не знайдено"));
            AvailableDate availableDate = availableDateRepository.findByMasterIdAndDate(masterId, date)
                    .orElseThrow(() -> new RuntimeException(userLanguage.equals("ru") ? "Доступная дата не найдена" :
                            userLanguage.equals("en") ? "Available date not found" :
                                    "Доступну дату не знайдено"));

            if (timeSlotRepository.existsByAvailableDateAndTime(availableDate, time)) {
                messageService.sendMessage(chatId,  userLanguage.equals("ru") ? "Временной слот уже существует." :
                        userLanguage.equals("en") ? "The time slot already exists." :
                                "Часовий слот уже існує.");
                return;
            }

            TimeSlot timeSlot = new TimeSlot();
            timeSlot.setAvailableDate(availableDate);
            timeSlot.setTime(time);
            timeSlot.setBooked(false);
            timeSlot.setMaster(master);
            timeSlotRepository.save(timeSlot);

            messageService.sendMessage(chatId,  userLanguage.equals("ru") ? "Временной слот успешно добавлен!" :
                    userLanguage.equals("en") ? "The time slot has been successfully added!" :
                            "Часовий слот успішно додано!");
            userSession.clearDateInfo(chatId);
            userSession.clearStates(chatId);
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, "/date");
            userSession.setPreviousState(chatId, "/admin");
            adminButtons.getDateInlineKeyboard(chatId, messageService);
        } catch (Exception e) {
            log.error("Ошибка при сохранении слота: ", e);
            messageService.sendMessage(chatId, userLanguage.equals("ru") ? "Ошибка при добавлении временного слота. Попробуйте снова." :
                    userLanguage.equals("en") ? "Error adding time slot. Please try again." :
                            "Помилка при додаванні часовго слота. Спробуйте ще раз.");
        }
    }

    public void initiateDeleteDate(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Локализуем сообщение
        String noMastersMessage = "ru".equals(languageCode)
                ? "Нет доступных мастеров для удаления даты."
                : "uk".equals(languageCode)
                ? "Немає доступних майстрів для видалення дати."
                : "No available masters to delete a date.";

        String message = "ru".equals(languageCode)
                ? "Пожалуйста, выберите мастера, для которого хотите удалить дату:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть майстра, для якого хочете видалити дату:"
                : "Please select the master for whom you want to delete a date:";

        // Получаем список мастеров
        List<Master> masters = masterRepository.findAll();

        // Если список мастеров пуст, отправляем сообщение и выходим
        if (masters.isEmpty()) {
            messageService.sendMessage(chatId, noMastersMessage);
            return;
        }

        // Формируем кнопки выбора мастера
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName()); // Имя мастера
            button.setCallbackData("/delete_date_" + master.getId()); // Callback с ID мастера
            rows.add(List.of(button));
        }

        // Добавляем кнопку "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        // Создаем клавиатуру и отправляем сообщение
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        // Инициализация сессии
        userSession.setDateInfo(chatId, new String[2]); // Инициализация массива для хранения ID мастера и даты
    }

    public void handleDeleteDateInput(Long chatId, String input) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя
        String[] deleteDateInfo = userSession.getDateInfo(chatId);

        if (deleteDateInfo == null) {
            messageService.sendMessage(chatId, "ru".equals(languageCode)
                    ? "Пожалуйста, начните процесс удаления даты снова, используя команду."
                    : "uk".equals(languageCode)
                    ? "Будь ласка, почніть процес видалення дати знову, використовуючи команду."
                    : "Please start the delete date process again by using the command.");
            return;
        }

        if (input.startsWith("/delete_date_")) {
            // Выбор мастера
            String masterId = input.split("_")[2]; // Извлекаем ID мастера
            deleteDateInfo[0] = masterId;
            userSession.setDateInfo(chatId, deleteDateInfo);

            List<AvailableDate> dates = availableDateRepository.findByMasterId(Long.valueOf(masterId))
                    .stream().sorted(Comparator.comparing(AvailableDate::getDate)).toList();

            if (dates.isEmpty()) {
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Нет доступных дат для удаления."
                        : "uk".equals(languageCode)
                        ? "Немає доступних дат для видалення."
                        : "No available dates for deletion.");
                return;
            }

            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, выберите дату из доступных:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, оберіть дату з доступних:"
                    : "Please select a date from the available options:";

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (AvailableDate date : dates) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(date.getDate().toString());
                button.setCallbackData("/deleteDate_" + date.getDate());
                rows.add(List.of(button));
            }

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);
            messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
        } else if (input.startsWith("/deleteDate_")) {
            // Выбор даты
            String selectedDate = input.split("_")[1];
            deleteDateInfo[1] = selectedDate;

            Long masterId = Long.valueOf(deleteDateInfo[0]);
            LocalDate date = LocalDate.parse(selectedDate);
            Optional<AvailableDate> availableDateOpt = findAvailableDateByMasterAndDate(masterId, date);

            if (availableDateOpt.isPresent()) {
                AvailableDate availableDate = availableDateOpt.get();
                List<TimeSlot> timeSlots = getTimeSlotsForAvailableDate(availableDate.getId());

                if (!timeSlots.isEmpty()) {
                    // Если есть временные ячейки, запрашиваем подтверждение
                    String confirmationMessage = "ru".equals(languageCode)
                            ? "Для этой даты существуют временные ячейки. Вы хотите удалить дату вместе с временными ячейками?"
                            : "uk".equals(languageCode)
                            ? "Для цієї дати існують часові слоти. Ви хочете видалити дату разом із часовими слотами?"
                            : "This date has associated time slots. Do you want to delete the date along with the time slots?";

                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                    InlineKeyboardButton confirmButton = new InlineKeyboardButton();
                    confirmButton.setText("ru".equals(languageCode) ? "Да, удалить" : "uk".equals(languageCode) ? "Так, видалити" : "Yes, delete");
                    confirmButton.setCallbackData("/confirmDeleteDate_" + availableDate.getId());

                    InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                    cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
                    cancelButton.setCallbackData("/cancel");

                    rows.add(List.of(confirmButton));
                    rows.add(List.of(cancelButton));

                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                    keyboard.setKeyboard(rows);
                    messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
                } else {
                    // Удаление даты, если нет временных ячеек
                    deleteAvailableDate(availableDate);
                    messageService.sendMessage(chatId, "ru".equals(languageCode)
                            ? "Дата " + date + " была удалена для мастера " + availableDate.getMaster().getName() + "."
                            : "uk".equals(languageCode)
                            ? "Дата " + date + " була видалена для майстра " + availableDate.getMaster().getName() + "."
                            : "Date " + date + " has been deleted for master " + availableDate.getMaster().getName() + ".");
                    userSession.clearDateInfo(chatId);
                    userSession.clearStates(chatId);
                    userSession.clearSession(chatId);
                    userSession.setCurrentState(chatId, "/date");
                    userSession.setPreviousState(chatId, "/admin");
                    adminButtons.getDateInlineKeyboard(chatId, messageService);
                }
            } else {
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Нет доступной даты для указанного мастера и даты."
                        : "uk".equals(languageCode)
                        ? "Немає доступної дати для вказаного майстра і дати."
                        : "No available date found for the specified master and date.");
            }
        } else if (input.startsWith("/confirmDeleteDate_")) {
            // Подтверждение удаления даты и временных слотов
            Long availableDateId = Long.valueOf(input.split("_")[1]);
            Optional<AvailableDate> availableDateOpt = availableDateRepository.findById(availableDateId);

            if (availableDateOpt.isPresent()) {
                AvailableDate availableDate = availableDateOpt.get();
                deleteAvailableDate(availableDate); // Метод, который удаляет дату и связанные слоты
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Дата и связанные временные слоты были удалены."
                        : "uk".equals(languageCode)
                        ? "Дата та пов'язані часові слоти були видалені."
                        : "The date and associated time slots have been deleted.");
                userSession.clearDateInfo(chatId);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
                userSession.setCurrentState(chatId, "/date");
                userSession.setPreviousState(chatId, "/admin");
                adminButtons.getDateInlineKeyboard(chatId, messageService);
            } else {
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Дата уже была удалена или недоступна."
                        : "uk".equals(languageCode)
                        ? "Дата вже була видалена або недоступна."
                        : "The date has already been deleted or is unavailable.");
            }
        } else if ("/cancel".equals(input)) {
            // Отмена операции
            messageService.sendMessage(chatId, "ru".equals(languageCode)
                    ? "Операция отменена."
                    : "uk".equals(languageCode)
                    ? "Операцію скасовано."
                    : "Operation cancelled.");
            userSession.clearDateInfo(chatId);
        }
    }

    public void initiateDeleteTime(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя

        // Локализуем сообщение
        String noMastersMessage = "ru".equals(languageCode)
                ? "Нет доступных мастеров для удаления временных слотов."
                : "uk".equals(languageCode)
                ? "Немає доступних майстрів для видалення часових слотів."
                : "No available masters to delete time slots.";

        String message = "ru".equals(languageCode)
                ? "Пожалуйста, выберите мастера, для которого хотите удалить временной слот:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть майстра, для якого хочете видалити часовий слот:"
                : "Please select the master for whom you want to delete a time slot:";

        // Получаем список мастеров
        List<Master> masters = masterRepository.findAll();

        // Если список мастеров пуст, отправляем сообщение и выходим
        if (masters.isEmpty()) {
            messageService.sendMessage(chatId, noMastersMessage);
            return;
        }

        // Формируем кнопки выбора мастера
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName());
            button.setCallbackData("/delete_time_" + master.getId());
            rows.add(List.of(button));
        }

        // Добавляем кнопку "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        // Создаем клавиатуру и отправляем сообщение
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        // Инициализация сессии
        userSession.setTimeInfo(chatId, new String[3]); // Массив для хранения ID мастера, даты и времени
    }

    public void handleDeleteTimeInput(Long chatId, String input) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя
        String[] deleteTimeInfo = userSession.getTimeInfo(chatId);

        if (deleteTimeInfo == null) {
            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, начните процесс удаления времени снова, используя команду."
                    : "uk".equals(languageCode)
                    ? "Будь ласка, почніть процес видалення часу знову, використовуючи команду."
                    : "Please start the delete time process again by using the command.";
            messageService.sendMessage(chatId, message);
            return;
        }

        if (input.startsWith("/delete_time_")) {
            // Шаг 1: Выбор мастера
            String masterId = input.split("_")[2];
            deleteTimeInfo[0] = masterId; // Сохраняем ID мастера
            userSession.setTimeInfo(chatId, deleteTimeInfo);

            List<AvailableDate> dates = availableDateRepository.findByMasterId(Long.valueOf(masterId)).stream()
                    .sorted(Comparator.comparing(AvailableDate::getDate)) // Sort by date in ascending order
                    .toList();

            if (dates.isEmpty()) {
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "У выбранного мастера нет доступных дат."
                        : "uk".equals(languageCode)
                        ? "У вибраного майстра немає доступних дат."
                        : "The selected master has no available dates.");
                return;
            }

            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, выберите дату из доступных:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, оберіть дату з доступних:"
                    : "Please select a date from the available options:";

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (AvailableDate date : dates) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(date.getDate().toString());
                button.setCallbackData("/delete_timeDate_" + date.getDate());
                rows.add(List.of(button));
            }

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);
            messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        } else if (input.startsWith("/delete_timeDate_")) {
            // Шаг 2: Выбор даты
            String selectedDate = input.split("_")[2];
            deleteTimeInfo[1] = selectedDate;
            userSession.setTimeInfo(chatId, deleteTimeInfo);

            Long masterId = Long.valueOf(deleteTimeInfo[0]);
            LocalDate date = LocalDate.parse(selectedDate);
            Optional<AvailableDate> availableDateOpt = findAvailableDateByMasterAndDate(masterId, date);

            if (availableDateOpt.isPresent()) {
                AvailableDate availableDate = availableDateOpt.get();
                List<TimeSlot> timeSlots = getTimeSlotsForAvailableDate(availableDate.getId());
                if (timeSlots.isEmpty()) {
                    messageService.sendMessage(chatId, "ru".equals(languageCode)
                            ? "Нет временных слотов для выбранной даты."
                            : "uk".equals(languageCode)
                            ? "Немає часових слотів для обраної дати."
                            : "No time slots available for the selected date.");
                    return;
                }

                String message = "ru".equals(languageCode)
                        ? "Пожалуйста, выберите время для удаления:"
                        : "uk".equals(languageCode)
                        ? "Будь ласка, оберіть час для видалення:"
                        : "Please select a time to delete:";

                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                for (TimeSlot slot : timeSlots) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(slot.getTime().toString());
                    button.setCallbackData("/delete_timeSlot_" + slot.getTime());
                    rows.add(List.of(button));
                }

                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                keyboard.setKeyboard(rows);
                messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
            } else {
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Нет доступной даты для указанного мастера."
                        : "uk".equals(languageCode)
                        ? "Немає доступної дати для вказаного майстра."
                        : "No available date found for the specified master.");
                userSession.clearTimeInfo(chatId);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
                userSession.setCurrentState(chatId, "/date");
                userSession.setPreviousState(chatId, "/admin");
                adminButtons.getDateInlineKeyboard(chatId, messageService);
            }

        } else if (input.startsWith("/delete_timeSlot_")) {
            // Шаг 3: Выбор времени
            String selectedTime = input.split("_")[2];
            deleteTimeInfo[2] = selectedTime;

            Long masterId = Long.valueOf(deleteTimeInfo[0]);
            LocalDate date = LocalDate.parse(deleteTimeInfo[1]);
            LocalTime time = LocalTime.parse(selectedTime);

            Optional<AvailableDate> availableDateOpt = findAvailableDateByMasterAndDate(masterId, date);
            if (availableDateOpt.isPresent()) {
                AvailableDate availableDate = availableDateOpt.get();
                List<TimeSlot> timeSlots = getTimeSlotsForAvailableDate(availableDate.getId());

                Optional<TimeSlot> timeSlotOpt = timeSlots.stream()
                        .filter(slot -> slot.getTime().equals(time))
                        .findFirst();

                if (timeSlotOpt.isPresent()) {
                    timeSlotRepository.delete(timeSlotOpt.get());
                    messageService.sendMessage(chatId, "ru".equals(languageCode)
                            ? "Временной слот " + time + " на " + date + " был удален."
                            : "uk".equals(languageCode)
                            ? "Часовий слот " + time + " на " + date + " був видалений."
                            : "Time slot " + time + " on " + date + " has been deleted.");
                    userSession.clearTimeInfo(chatId);
                    userSession.clearStates(chatId);
                    userSession.clearSession(chatId);
                    userSession.setCurrentState(chatId, "/date");
                    userSession.setPreviousState(chatId, "/admin");
                    adminButtons.getDateInlineKeyboard(chatId, messageService);
                } else {
                    messageService.sendMessage(chatId, "ru".equals(languageCode)
                            ? "Не найдено временного слота для указанного времени."
                            : "uk".equals(languageCode)
                            ? "Не знайдено часовий слот для вказаного часу."
                            : "No time slot found for the specified time.");
                    userSession.clearTimeInfo(chatId);
                    userSession.clearStates(chatId);
                    userSession.clearSession(chatId);
                    userSession.setCurrentState(chatId, "/date");
                    userSession.setPreviousState(chatId, "/admin");
                    adminButtons.getDateInlineKeyboard(chatId, messageService);
                }
            } else {
                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Нет доступной даты для указанного мастера."
                        : "uk".equals(languageCode)
                        ? "Немає доступної дати для вказаного майстра."
                        : "No available date found for the specified master.");
            }
        }
    }
}

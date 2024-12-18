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

    public void initiateAddDate(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Получаем список всех мастеров
        List<Master> masters = masterRepository.findAll();

        if (masters.isEmpty()) {
            // Если мастеров нет, отправляем сообщение пользователю
            String noMastersMessage = messageService.getLocalizedMessage("master.noAvailableMasters", languageCode);
            messageService.sendMessage(chatId, noMastersMessage);
            return;
        }

        // Формируем сообщение в зависимости от языка
        String message = messageService.getLocalizedMessage("master.selectMasterForDate", languageCode);

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
        cancelButton.setText(messageService.getLocalizedMessage("cancelButton", languageCode));  // Используем локализованное название кнопки
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
            String message = messageService.getLocalizedMessage("dateProcess.startAgain", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        if (input.startsWith("/master_date_")) {
            // Обработка выбора мастера
            String masterId = input.split("_")[2];
            dateInfo[0] = masterId; // Сохраняем ID мастера

            String message = messageService.getLocalizedMessage("dateProcess.enterDate", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        if (dateInfo[1] == null) {
            // Step 2: Ввод даты
            if (!input.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String message = messageService.getLocalizedMessage("dateProcess.invalidFormat", languageCode);
                messageService.sendMessage(chatId, message);
                return;
            }
            LocalDate date;

            try {
                date = LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                String message = messageService.getLocalizedMessage("dateProcess.invalidFormat", languageCode);
                messageService.sendMessage(chatId, message);
                return;
            }

            // Проверка на добавление даты в прошлом
            if (date.isBefore(LocalDate.now())) {
                String message = messageService.getLocalizedMessage("dateProcess.pastDate", languageCode);
                messageService.sendMessage(chatId, message);
                return;
            }

            // Check if the date already exists for this master
            Long masterId = Long.parseLong(dateInfo[0]);
            if (findAvailableDateByMasterAndDate(masterId, date).isPresent()) {
                String message = messageService.getLocalizedMessage("dateProcess.dateExists", languageCode);
                messageService.sendMessage(chatId, message);
            } else {
                dateInfo[1] = date.toString();

                // Save the available date
                AvailableDate availableDate = new AvailableDate();
                availableDate.setDate(date);
                availableDate.setMaster(masterRepository.findById(masterId).orElse(null));

                addAvailableDate(availableDate);
                String message = messageService.getLocalizedMessage("dateProcess.success", languageCode);
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
        // Получаем язык пользователя
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем локализованное сообщение для подсказки пользователю
        String message = messageService.getLocalizedMessage("addTime.selectMaster", languageCode);

        // Получаем список мастеров
        List<Master> masters = masterRepository.findAll();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Формируем кнопки для каждого мастера
        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName()); // Устанавливаем имя мастера
            button.setCallbackData("/master_time_" + master.getId()); // Устанавливаем ID мастера в callback
            rows.add(List.of(button));
        }

        // Кнопка "Отмена" / "Скасувати" / "Cancel"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(messageService.getLocalizedMessage("general.cancel", languageCode));
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        // Формируем клавиатуру с кнопками
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        // Инициализация сессии для хранения данных о мастере, дате и времени
        userSession.setTimeInfo(chatId, new String[3]); // Инициализация массива [мастер, дата, время]
        userSession.setPreviousState(chatId, "/add_time");
    }

    public void handleAddTimeInput(Long chatId, String input) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Язык пользователя
        String[] timeInfo = userSession.getTimeInfo(chatId);

        if (timeInfo == null) {
            String message = messageService.getLocalizedMessage("handle_add_time_start", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        if (input.startsWith("/master_time_")) {
            String masterId = input.split("_")[2]; // Извлекаем ID мастера
            timeInfo[0] = masterId; // Сохраняем ID мастера
            userSession.setTimeInfo(chatId, timeInfo);

            log.info("Master ID {} успешно сохранен для чата {}", masterId, chatId);

            String message = messageService.getLocalizedMessage("master_time_prompt", languageCode);

            List<AvailableDate> dates = availableDateRepository.findByMasterId(Long.valueOf(masterId)).stream()
                    .filter(date -> !date.getDate().isBefore(LocalDate.now())) // Remove past dates
                    .sorted(Comparator.comparing(AvailableDate::getDate)) // Sort from nearest to farthest
                    .collect(Collectors.toList());

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag(languageCode));

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

            String message = messageService.getLocalizedMessage("date_time_prompt", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        if (timeInfo[2] == null) {
            if (!input.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
                String message = messageService.getLocalizedMessage("time_format_error", languageCode);
                messageService.sendMessage(chatId, message);
                return;
            }

            LocalTime time = LocalTime.parse(input);
            timeInfo[2] = time.toString(); // Сохраняем время
            saveTimeSlot(chatId, timeInfo);
        }
    }

    private void saveTimeSlot(Long chatId, String[] timeInfo) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Язык пользователя
        if (timeInfo[0] == null || timeInfo[1] == null || timeInfo[2] == null) {
            log.error("Некорректные данные timeInfo: {}", Arrays.toString(timeInfo));
            String errorMessage = messageService.getLocalizedMessage("time_info_error", languageCode);
            messageService.sendMessage(chatId, errorMessage);
            return;
        }

        try {
            Long masterId = Long.valueOf(timeInfo[0]);
            LocalDate date = LocalDate.parse(timeInfo[1]);
            LocalTime time = LocalTime.parse(timeInfo[2]);

            log.info("Сохранение слота: мастер={}, дата={}, время={}", masterId, date, time);

            Master master = masterRepository.findById(masterId).orElseThrow(() -> new RuntimeException(
                    messageService.getLocalizedMessage("master_not_found", languageCode)
            ));

            AvailableDate availableDate = availableDateRepository.findByMasterIdAndDate(masterId, date)
                    .orElseThrow(() -> new RuntimeException(
                            messageService.getLocalizedMessage("available_date_not_found", languageCode)
                    ));

            if (timeSlotRepository.existsByAvailableDateAndTime(availableDate, time)) {
                String message = messageService.getLocalizedMessage("time_slot_exists", languageCode);
                messageService.sendMessage(chatId, message);
                return;
            }

            TimeSlot timeSlot = new TimeSlot();
            timeSlot.setAvailableDate(availableDate);
            timeSlot.setTime(time);
            timeSlot.setBooked(false);
            timeSlot.setMaster(master);
            timeSlotRepository.save(timeSlot);

            String successMessage = messageService.getLocalizedMessage("time_slot_added_success", languageCode);
            messageService.sendMessage(chatId, successMessage);

            // Очистка сессии и переход в меню
            userSession.clearDateInfo(chatId);
            userSession.clearStates(chatId);
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, "/date");
            userSession.setPreviousState(chatId, "/admin");
            adminButtons.getDateInlineKeyboard(chatId, messageService);
        } catch (Exception e) {
            log.error("Ошибка при сохранении слота: ", e);
            String errorMessage = messageService.getLocalizedMessage("time_slot_add_error", languageCode);
            messageService.sendMessage(chatId, errorMessage);
        }
    }

    public void initiateDeleteDate(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        // Локализуем сообщения
        String noMastersMessage = messageService.getLocalizedMessage("no_masters_for_delete_date", languageCode);
        String message = messageService.getLocalizedMessage("select_master_for_delete_date", languageCode);

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
        cancelButton.setText(messageService.getLocalizedMessage("cancel", languageCode));
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
            messageService.sendMessage(chatId, messageService.getLocalizedMessage("start_delete_date_process", languageCode));
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
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("no_available_dates_for_deletion", languageCode));
                return;
            }

            String message = messageService.getLocalizedMessage("select_date_for_deletion", languageCode);

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
                    String confirmationMessage = messageService.getLocalizedMessage("confirmation_delete_date_with_time_slots", languageCode);

                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                    InlineKeyboardButton confirmButton = new InlineKeyboardButton();
                    confirmButton.setText(messageService.getLocalizedMessage("confirm_delete", languageCode));
                    confirmButton.setCallbackData("/confirmDeleteDate_" + availableDate.getId());

                    InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                    cancelButton.setText(messageService.getLocalizedMessage("cancel", languageCode));
                    cancelButton.setCallbackData("/cancel");

                    rows.add(List.of(confirmButton));
                    rows.add(List.of(cancelButton));

                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                    keyboard.setKeyboard(rows);
                    messageService.sendMessageWithInlineKeyboard(chatId, confirmationMessage, keyboard);
                } else {
                    // Удаление даты, если нет временных ячеек
                    deleteAvailableDate(availableDate);
                    messageService.sendMessage(chatId, messageService.getLocalizedMessage("date_deleted", languageCode, date, availableDate.getMaster().getName()));
                    userSession.clearDateInfo(chatId);
                    userSession.clearStates(chatId);
                    userSession.clearSession(chatId);
                    userSession.setCurrentState(chatId, "/date");
                    userSession.setPreviousState(chatId, "/admin");
                    adminButtons.getDateInlineKeyboard(chatId, messageService);
                }
            } else {
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("no_available_date_for_master", languageCode));
            }
        } else if (input.startsWith("/confirmDeleteDate_")) {
            // Подтверждение удаления даты и временных слотов
            Long availableDateId = Long.valueOf(input.split("_")[1]);
            Optional<AvailableDate> availableDateOpt = availableDateRepository.findById(availableDateId);

            if (availableDateOpt.isPresent()) {
                AvailableDate availableDate = availableDateOpt.get();
                deleteAvailableDate(availableDate); // Метод, который удаляет дату и связанные слоты
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("date_and_time_slots_deleted", languageCode));
                userSession.clearDateInfo(chatId);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
                userSession.setCurrentState(chatId, "/date");
                userSession.setPreviousState(chatId, "/admin");
                adminButtons.getDateInlineKeyboard(chatId, messageService);
            } else {
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("date_already_deleted_or_unavailable", languageCode));
            }
        } else if ("/cancel".equals(input)) {
            // Отмена операции
            messageService.sendMessage(chatId, messageService.getLocalizedMessage("operation_cancelled", languageCode));
            userSession.clearDateInfo(chatId);
        }
    }

    public void initiateDeleteTime(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя

        // Локализуем сообщение
        String noMastersMessage = messageService.getLocalizedMessage("no_masters_for_delete_time", languageCode);

        String message = messageService.getLocalizedMessage("select_master_for_delete_time", languageCode);

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
        cancelButton.setText(messageService.getLocalizedMessage("cancel", languageCode));
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
            messageService.sendMessage(chatId, messageService.getLocalizedMessage("start_delete_time_process", languageCode));
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
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("no_available_dates_for_deletion", languageCode));
                return;
            }

            String message = messageService.getLocalizedMessage("select_date_for_deletion", languageCode);

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
                    messageService.sendMessage(chatId, messageService.getLocalizedMessage("no_time_slots_for_date", languageCode));
                    return;
                }

                String message = messageService.getLocalizedMessage("select_time_for_deletion", languageCode);

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
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("no_available_date_for_master", languageCode));
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
                    messageService.sendMessage(chatId, messageService.getLocalizedMessage("time_slot_deleted", languageCode, time, date));
                    userSession.clearTimeInfo(chatId);
                    userSession.clearStates(chatId);
                    userSession.clearSession(chatId);
                    userSession.setCurrentState(chatId, "/date");
                    userSession.setPreviousState(chatId, "/admin");
                    adminButtons.getDateInlineKeyboard(chatId, messageService);
                } else {
                    messageService.sendMessage(chatId, messageService.getLocalizedMessage("time_slot_not_found", languageCode));
                    userSession.clearTimeInfo(chatId);
                    userSession.clearStates(chatId);
                    userSession.clearSession(chatId);
                    userSession.setCurrentState(chatId, "/date");
                    userSession.setPreviousState(chatId, "/admin");
                    adminButtons.getDateInlineKeyboard(chatId, messageService);
                }
            } else {
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("no_available_date_for_master", languageCode));
            }
        }
    }
}
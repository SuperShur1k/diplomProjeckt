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

        // Формируем сообщение в зависимости от языка
        String message = "ru".equals(languageCode)
                ? "Пожалуйста, введите номер телефона пользователя, которого вы хотите сделать мастером."
                : "uk".equals(languageCode)
                ? "Будь ласка, введіть номер телефону користувача, якого ви хочете зробити майстром."
                : "Please enter the phone number of the user you want to make a master.";

        messageService.sendMessage(chatId, message);
        userSession.setSettingMaster(chatId, true);

        // Сообщение о кнопке отмены операции
        String cancelMessage = "ru".equals(languageCode)
                ? "Вы можете отменить эту операцию, используя кнопку ниже."
                : "uk".equals(languageCode)
                ? "Ви можете скасувати цю операцію за допомогою кнопки нижче."
                : "You can cancel this operation using the button below.";

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
            String message = "ru".equals(languageCode)
                    ? "Пользователь не найден. Пожалуйста, попробуйте снова."
                    : "uk".equals(languageCode)
                    ? "Користувача не знайдено. Будь ласка, спробуйте ще раз."
                    : "User not found. Please try again.";

            if (users == null) {
                messageService.sendMessageWithInlineKeyboard(chatId, message, adminButtons.getAdminInlineKeyboard(chatId));
                userSession.setSettingMaster(chatId, false);
            } else {
                // Сохраняем начальные данные мастера и запрашиваем описание
                masterInfo = new String[3];
                masterInfo[0] = users.getPhoneNumber();  // Сохраняем имя пользователя
                masterInfo[1] = users.getFirstName() + " " + users.getLastName();  // Имя мастера
                userSession.setMasterInfo(chatId, masterInfo);
                String descriptionMessage = "ru".equals(languageCode)
                        ? "Пожалуйста, введите описание для нового мастера:"
                        : "uk".equals(languageCode)
                        ? "Будь ласка, введіть опис для нового майстра:"
                        : "Please enter a description for the new master:";
                messageService.sendMessage(chatId, descriptionMessage);
            }
        } else if (masterInfo[2] == null) {
            // Второй этап - получение описания
            masterInfo[2] = text;  // Сохраняем описание мастера
            String socialLinkMessage = "ru".equals(languageCode)
                    ? "Пожалуйста, введите ссылку на социальную сеть для нового мастера:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, введіть посилання на соціальну мережу для нового майстра:"
                    : "Please enter a SocialLink for the new master:";
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

            String successMessage = "ru".equals(languageCode)
                    ? "Пользователь " + masterInfo[1] + " успешно добавлен как мастер."
                    : "uk".equals(languageCode)
                    ? "Користувача " + masterInfo[1] + " успішно додано як майстра."
                    : "User " + masterInfo[1] + " has been successfully added as a master.";

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
            String noMastersMessage = "ru".equals(languageCode)
                    ? "В системе нет мастеров для управления."
                    : "uk".equals(languageCode)
                    ? "У системі немає майстрів для керування."
                    : "There are no masters in the system to manage.";
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
        cancelButton.setText(
                "ru".equals(languageCode) ? "Отмена" :
                        "uk".equals(languageCode) ? "Скасувати" :
                                "Cancel"
        );
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите мастера для управления:"
                : "uk".equals(languageCode)
                ? "Виберіть майстра для керування:"
                : "Select a master to manage:";

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showMasterSettings(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Master master = masterRepository.findById(masterId).orElse(null);
        if (master == null) {
            String notFoundMessage = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
            messageService.sendMessageWithInlineKeyboard(chatId, notFoundMessage, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        // Создаем inline-кнопки для управления мастером
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки для изменения статуса мастера
        InlineKeyboardButton activateButton = new InlineKeyboardButton();
        activateButton.setText("ru".equals(languageCode) ? "Активировать" : "uk".equals(languageCode) ? "Активувати" : "Activate");
        activateButton.setCallbackData("/set_master_active_" + masterId);

        InlineKeyboardButton deactivateButton = new InlineKeyboardButton();
        deactivateButton.setText("ru".equals(languageCode) ? "Деактивировать" : "uk".equals(languageCode) ? "Деактивувати" : "Deactivate");
        deactivateButton.setCallbackData("/set_master_inactive_" + masterId);

        rows.add(List.of(activateButton, deactivateButton));

        // Кнопка для удаления мастера
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("ru".equals(languageCode) ? "Удалить" : "uk".equals(languageCode) ? "Видалити" : "Delete");
        deleteButton.setCallbackData("/delete_master_" + masterId);

        rows.add(List.of(deleteButton));

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/manage_masters");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Настройки для мастера: " + master.getName()
                : "uk".equals(languageCode)
                ? "Налаштування для майстра: " + master.getName()
                : "Settings for master: " + master.getName();

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void setMasterStatus(Long chatId, Long masterId, Master.Status status) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Master master = masterRepository.findById(masterId).orElse(null);
        if (master == null) {
            String notFoundMessage = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
            messageService.sendMessageWithInlineKeyboard(chatId, notFoundMessage, adminButtons.getAdminInlineKeyboard(chatId));
            return;
        }

        master.setStatus(status);
        masterRepository.save(master);

        String successMessage = "ru".equals(languageCode)
                ? "Статус мастера " + master.getName() + " успешно изменен на " + status + "."
                : "uk".equals(languageCode)
                ? "Статус майстра " + master.getName() + " успішно змінено на " + status + "."
                : "The status of master " + master.getName() + " has been successfully changed to " + status + ".";

        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getAdminInlineKeyboard(chatId));
    }

    public void deleteMaster(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Master master = masterRepository.findById(masterId).orElse(null);
        if (master == null) {
            String notFoundMessage = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
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

        String successMessage = "ru".equals(languageCode)
                ? "Мастер " + master.getName() + " и все связанные данные успешно удалены."
                : "uk".equals(languageCode)
                ? "Майстра " + master.getName() + " та всі пов'язані дані успішно видалено."
                : "Master " + master.getName() + " and all related data have been successfully deleted.";

        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, adminButtons.getAdminInlineKeyboard(chatId));
    }

    public void initialWriteToAdmin(Long chatId, Long adminChatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Получаем информацию об администраторе
        Users admin = userRepository.findByChatId(adminChatId);
        if (admin == null) {
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/main_menu");
            messageService.sendMessageWithInlineKeyboard(chatId, "Admin not found with ID: " + adminChatId, autUserButtons.getAuthenticatedInlineKeyboard(chatId));
        }

        // Сообщение пользователю
        String message = "ru".equals(languageCode)
                ? "Напишите сообщение администратору: " + admin.getFirstName()
                : "uk".equals(languageCode)
                ? "Напишіть повідомлення адміністратору: " + admin.getFirstName()
                : "Write a message to the admin: " + admin.getFirstName();

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
            messageService.sendMessage(masterChatId, "Master not found with chat ID: " + masterChatId);
        }

        // Формируем сообщение для администратора
        String messageToAdmin = "ru".equals(adminLanguageCode)
                ? "Вам написал мастер:\n" + master.getName() + "\n\nСообщение:\n" + messageText
                : "uk".equals(adminLanguageCode)
                ? "Вам написав майстер:\n" + master.getName() + "\n\nПовідомлення:\n" + messageText
                : "A master wrote to you:\n" + master.getName() + "\n\nMessage:\n" + messageText;

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText("ru".equals(adminLanguageCode)
                ? "Ответить"
                : "uk".equals(adminLanguageCode)
                ? "Відповісти"
                : "Reply");
        replyButton.setCallbackData("/write_master_" + masterChatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Отправляем сообщение администратору
        messageService.sendMessageWithInlineKeyboard(adminChatId, messageToAdmin, keyboard);

        // Подтверждаем мастеру отправку сообщения
        String confirmationMessage = "ru".equals(languageCode)
                ? "Ваше сообщение отправлено администратору."
                : "uk".equals(languageCode)
                ? "Ваше повідомлення надіслано адміністратору."
                : "Your message has been sent to the admin.";

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
            String noMasterMessage = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
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
            String noAppointmentsMessage = "ru".equals(languageCode)
                    ? "На данный момент у вас нет записей."
                    : "uk".equals(languageCode)
                    ? "На даний момент у вас немає записів."
                    : "You currently have no appointments.";
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
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/master");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        String message = "ru".equals(languageCode)
                ? "Выберите дату, чтобы посмотреть записи:"
                : "uk".equals(languageCode)
                ? "Оберіть дату, щоб переглянути записи:"
                : "Select a date to view appointments:";
        messageService.sendMessageWithInlineKeyboard(masterChatId, message, keyboard);
    }

    public void timeCheckAppointments(Long masterChatId, LocalDate date) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);

        // Получаем мастера по chatId
        Master master = masterRepository.findByChatId(masterChatId);
        if (master == null) {
            String noMasterMessage = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
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
            String noAppointmentsMessage = "ru".equals(languageCode)
                    ? "На эту дату у вас нет записей."
                    : "uk".equals(languageCode)
                    ? "На цю дату у вас немає записів."
                    : "You have no appointments for this date.";
            messageService.sendMessage(masterChatId, noAppointmentsMessage);
            return;
        }

        // Генерируем кнопки для каждого времени
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (LocalTime time : appointmentTimes) {
            InlineKeyboardButton timeButton = new InlineKeyboardButton();
            timeButton.setText(time.toString()); // Отображаем время как текст
            timeButton.setCallbackData("/appointment_details_" + date + "_" + time); // Устанавливаем callback с датой и временем
            rows.add(List.of(timeButton));
        }

        // Добавляем кнопку "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode) ? "Назад" : "uk".equals(languageCode) ? "Назад" : "Back");
        backButton.setCallbackData("/view_appointments");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с клавиатурой
        String message = "ru".equals(languageCode)
                ? "Выберите время, чтобы посмотреть детали записи:"
                : "uk".equals(languageCode)
                ? "Оберіть час, щоб переглянути деталі запису:"
                : "Select a time to view appointment details:";
        messageService.sendMessageWithInlineKeyboard(masterChatId, message, keyboard);
    }

    public void showInfoAppointments(Long masterChatId, LocalDate date, LocalTime time) {
        String languageCode = userRepository.findLanguageCodeByChatId(masterChatId);

        // Получаем мастера по chatId
        Master master = masterRepository.findByChatId(masterChatId);
        if (master == null) {
            String noMasterMessage = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
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
            String noAppointmentMessage = "ru".equals(languageCode)
                    ? "Запись не найдена на указанную дату и время."
                    : "uk".equals(languageCode)
                    ? "Запис не знайдено на вказану дату і час."
                    : "Appointment not found for the specified date and time.";
            messageService.sendMessage(masterChatId, noAppointmentMessage);
            return;
        }

        // Получаем информацию о клиенте
        Users client = appointment.getUsers();
        if (client == null) {
            String noClientMessage = "ru".equals(languageCode)
                    ? "Клиент не найден."
                    : "uk".equals(languageCode)
                    ? "Клієнта не знайдено."
                    : "Client not found.";
            messageService.sendMessage(masterChatId, noClientMessage);
            return;
        }

        // Формируем сообщение с информацией о записи
        String appointmentInfo = "ru".equals(languageCode)
                ? "Информация о записи:\n" +
                "Имя клиента: " + client.getFirstName() + "\n" +
                "Фамилия клиента: " + client.getLastName() + "\n" +
                "Номер телефона: " + client.getPhoneNumber() + "\n" +
                "Язык: " + client.getLanguage() + "\n" +
                "Дата: " + date + "\n" +
                "Время: " + time
                : "uk".equals(languageCode)
                ? "Інформація про запис:\n" +
                "Ім'я клієнта: " + client.getFirstName() + "\n" +
                "Прізвище клієнта: " + client.getLastName() + "\n" +
                "Номер телефону: " + client.getPhoneNumber() + "\n" +
                "Мова: " + client.getLanguage() + "\n" +
                "Дата: " + date + "\n" +
                "Час: " + time
                : "Appointment Information:\n" +
                "Client First Name: " + client.getFirstName() + "\n" +
                "Client Last Name: " + client.getLastName() + "\n" +
                "Phone Number: " + client.getPhoneNumber() + "\n" +
                "Language: " + client.getLanguage() + "\n" +
                "Date: " + date + "\n" +
                "Time: " + time;

        // Создаём кнопки
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Написать клиенту"
        InlineKeyboardButton messageClientButton = new InlineKeyboardButton();
        messageClientButton.setText("ru".equals(languageCode)
                ? "Написать клиенту"
                : "uk".equals(languageCode)
                ? "Написати клієнту"
                : "Message Client");
        messageClientButton.setCallbackData("/message_client_" + client.getChatId());
        rows.add(List.of(messageClientButton));

        // Кнопка "Отменить запись"
        InlineKeyboardButton cancelAppointmentButton = new InlineKeyboardButton();
        cancelAppointmentButton.setText("ru".equals(languageCode)
                ? "Отменить запись"
                : "uk".equals(languageCode)
                ? "Скасувати запис"
                : "Cancel Appointment");
        cancelAppointmentButton.setCallbackData("/master_cancel_appointment_" + appointment.getId());
        rows.add(List.of(cancelAppointmentButton));

        // Кнопка "Перенести запись"
        InlineKeyboardButton rescheduleAppointmentButton = new InlineKeyboardButton();
        rescheduleAppointmentButton.setText("ru".equals(languageCode)
                ? "Перенести запись"
                : "uk".equals(languageCode)
                ? "Перенести запис"
                : "Reschedule Appointment");
        rescheduleAppointmentButton.setCallbackData("/master_reschedule_appointment_" + appointment.getId());
        rows.add(List.of(rescheduleAppointmentButton));

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("ru".equals(languageCode)
                ? "Назад"
                : "uk".equals(languageCode)
                ? "Назад"
                : "Back");
        backButton.setCallbackData("/appointments_for_date_" + date);
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        // Отправляем сообщение с информацией о записи и кнопками
        messageService.sendMessageWithInlineKeyboard(masterChatId, appointmentInfo, keyboard);
    }

    public void cancelAppointment(Long chatId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Запись не найдена."
                    : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Запис не знайдено."
                    : "Appointment not found.";
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
        String clientMessage = "ru".equals(userRepository.findLanguageCodeByChatId(appointment.getChatId()))
                ? "Ваша запись на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + " была успешно отменена."
                : "uk".equals(userRepository.findLanguageCodeByChatId(appointment.getChatId()))
                ? "Ваш запис на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + " був успішно скасований."
                : "Your appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + " has been successfully canceled.";
        messageService.sendMessage(appointment.getChatId(), clientMessage);

        // Notify the master
        String masterMessage = "ru".equals(userRepository.findLanguageCodeByChatId(appointment.getMaster().getChatId()))
                ? "Клиент отменил запись на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + "."
                : "uk".equals(userRepository.findLanguageCodeByChatId(appointment.getMaster().getChatId()))
                ? "Клієнт скасував запис на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + "."
                : "The client canceled the appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + ".";
        messageService.sendMessage(appointment.getMaster().getChatId(), masterMessage);

        // Notify the admin
        String adminMessage = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Запись успешно отменена."
                : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Запис успішно скасовано."
                : "Appointment successfully canceled.";
        messageService.sendMessageWithInlineKeyboard(chatId, adminMessage, autUserButtons.masterPanel(chatId));
    }

    public void selectTransferDate(Long chatId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Запись не найдена."
                    : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Запис не знайдено."
                    : "Appointment not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        List<AvailableDate> availableDates = availableDateService.getAvailableDatesForMaster(appointment.getMaster().getId())
                .stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now()))
                .filter(date -> availableDateService.getTimeSlotsForAvailableDate(date.getId())
                        .stream().anyMatch(slot -> !slot.isBooked()))
                .sorted(Comparator.comparing(AvailableDate::getDate))
                .collect(Collectors.toList());

        if (availableDates.isEmpty()) {
            String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Нет доступных дат для переноса записи."
                    : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Немає доступних дат для перенесення запису."
                    : "No available dates for transferring the appointment.";
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
        String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Выберите дату для переноса записи:"
                : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Оберіть дату для перенесення запису:"
                : "Select a date to transfer the appointment:";
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void selectTransferTime(Long chatId, Long dateId, Long appointmentId) {
        List<TimeSlot> timeSlots = availableDateService.getTimeSlotsForAvailableDate(dateId)
                .stream()
                .filter(slot -> !slot.isBooked())
                .sorted(Comparator.comparing(TimeSlot::getTime))
                .collect(Collectors.toList());

        if (timeSlots.isEmpty()) {
            String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Нет доступных временных слотов для выбранной даты."
                    : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Немає доступних часових слотів для обраної дати."
                    : "No available time slots for the selected date.";
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
        String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Выберите время для переноса записи:"
                : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Оберіть час для перенесення запису:"
                : "Select a time to transfer the appointment:";
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void finalizeTransfer(Long chatId, Long appointmentId, Long timeSlotId, Long dateId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        TimeSlot newTimeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);
        AvailableDate newDate = availableDateRepository.findById(dateId).orElse(null);

        if (appointment == null || newTimeSlot == null) {
            String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Перенос не удался. Пожалуйста, попробуйте снова."
                    : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Перенесення не вдалося. Будь ласка, спробуйте ще раз."
                    : "Transfer failed. Please try again.";
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


        String successMessage = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Запись успешно перенесена."
                : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Запис успішно перенесено."
                : "Appointment successfully transferred.";
        messageService.sendMessageWithInlineKeyboard(chatId, successMessage, autUserButtons.masterPanel(chatId));

        // Notify the client
        String clientMessage = "ru".equals(userRepository.findLanguageCodeByChatId(appointment.getChatId()))
                ? "Ваша запись была перенесена на " +
                newTimeSlot.getAvailableDate().getDate() +
                " в " + newTimeSlot.getTime() + "."
                : "uk".equals(userRepository.findLanguageCodeByChatId(appointment.getChatId()))
                ? "Ваш запис був перенесений на " +
                newTimeSlot.getAvailableDate().getDate() +
                " о " + newTimeSlot.getTime() + "."
                : "Your appointment has been rescheduled to " +
                newTimeSlot.getAvailableDate().getDate() +
                " at " + newTimeSlot.getTime() + ".";
        messageService.sendMessage(appointment.getChatId(), clientMessage);
    }

    public void writeToClient(Long chatId, Long clientChatId) {
        // Проверяем, существует ли клиент с данным chatId
        if (!userRepository.existsByChatId(clientChatId)) {
            String message = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Клиент с таким ID не найден."
                    : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                    ? "Клієнта з таким ID не знайдено."
                    : "Client with this ID not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем состояние в сессии мастера
        userSession.setCurrentState(chatId, "/master_write_to_client_" + clientChatId);
        String messagePrompt = "ru".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Введите сообщение для клиента:"
                : "uk".equals(userRepository.findLanguageCodeByChatId(chatId))
                ? "Введіть повідомлення для клієнта:"
                : "Enter the message for the client:";
        messageService.sendMessage(chatId, messagePrompt);
    }

    public void finishWriteToClient(Long chatId, Long clientChatId, String text) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        String clientLanguageCode = userRepository.findLanguageCodeByChatId(clientChatId);

        // Формируем сообщение для получателя
        String messageToRecipient = "ru".equals(clientLanguageCode)
                ? "Вам написал мастер:\n" + text
                : "uk".equals(clientLanguageCode)
                ? "Вам написав мастер:\n" + text
                : "An master wrote to you:\n" + text;

        // Создаем клавиатуру с кнопкой "Ответить"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton replyButton = new InlineKeyboardButton();
        replyButton.setText("ru".equals(clientLanguageCode)
                ? "Ответить"
                : "uk".equals(clientLanguageCode)
                ? "Відповісти"
                : "Reply");
        replyButton.setCallbackData("/reply_to_master_" + chatId);

        keyboard.setKeyboard(List.of(List.of(replyButton)));

        // Отправляем сообщение получателю
        messageService.sendMessageWithInlineKeyboard(clientChatId, messageToRecipient, keyboard);

        // Подтверждаем отправителю
        String confirmationMessage = "ru".equals(languageCode)
                ? "Ваше сообщение отправлено пользователю."
                : "uk".equals(languageCode)
                ? "Ваше повідомлення надіслано користувачу."
                : "Your message has been sent to the user.";

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
            InlineKeyboardButton button = new InlineKeyboardButton("ru".equals(languageCode) ? service.getNameRu() :
                    "uk".equals(languageCode) ? service.getNameUk() :
                            service.getNameEn());
            button.setCallbackData("/master_service_" + service.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите услугу:" :
                "uk".equals(languageCode)
                        ? "Оберіть послугу:" :
                        "Select a service:";
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showDateSelection(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<AvailableDate> dates = availableDateRepository.findByMasterId(masterRepository.findByChatId(chatId).getId());
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        dates = dates.stream()
                .filter(date -> date.getDate().isAfter(LocalDate.now()))
                .collect(Collectors.toList());

        for (AvailableDate date : dates) {
            InlineKeyboardButton button = new InlineKeyboardButton(date.getDate().toString());
            button.setCallbackData("/master_date_master_" + date.getId());
            rows.add(List.of(button));
        }

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите дату:" :
                "uk".equals(languageCode)
                        ? "Оберіть дату:" :
                        "Select a date:";
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showTimeSelection(Long chatId, Long dateId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);
        List<TimeSlot> slots = availableDateService.getTimeSlotsForAvailableDate(dateId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = slots.stream()
                .filter(slot -> !slot.isBooked())
                .map(slot -> {
                    InlineKeyboardButton button = new InlineKeyboardButton(slot.getTime().toString());
                    button.setCallbackData("/master_time_master_" + slot.getId());
                    return List.of(button);
                }).collect(Collectors.toList());

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите время:" :
                "uk".equals(languageCode)
                        ? "Оберіть час:" :
                        "Select a time:";
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
            messageService.sendMessageWithInlineKeyboard(chatId, "ru".equals(languageCode) ? "Ошибка: неверное время." :
                    "uk".equals(languageCode) ? "Помилка: невірний час." :
                            "Error: invalid time.", autUserButtons.masterPanel(chatId));
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
            String clientSuccessMessage = "ru".equals(languageCodeClient)
                    ? "Ваша запись на " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + " была успешно создана."
                    : "uk".equals(languageCodeClient)
                    ? "Ваш запис на " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + " був успішно створено."
                    : "Your appointment on " + appointment.getAppointmentDate().toLocalDate() + " at " + appointment.getAppointmentDate().toLocalTime() + " has been successfully booked.";
            messageService.sendMessage(userChatId, clientSuccessMessage);
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

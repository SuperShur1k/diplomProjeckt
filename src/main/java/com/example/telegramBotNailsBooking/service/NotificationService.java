package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.model.Appointment;
import com.example.telegramBotNailsBooking.model.Master;
import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.model.UserSession;
import com.example.telegramBotNailsBooking.repository.AppointmentRepository;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AppointmentRepository appointmentRepository;
    private final MessageService messageService;
    private final UserRepository userRepository;

    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public NotificationService(AppointmentRepository appointmentRepository, MessageService messageService, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // Запускается каждый час в начале часа
    public void sendUpcomingAppointmentNotifications() {
        log.info("NotificationService: sendUpcomingAppointmentNotifications started");
        LocalDateTime now = LocalDateTime.now(clock);
        List<Appointment> appointments = appointmentRepository.findAll();

        for (Appointment appointment : appointments) {
            LocalDateTime appointmentTime = appointment.getAppointmentDate();

            log.info("Current time: {}", now);
            log.info("Appointment time: {}", appointmentTime);
            log.info("Hours between now and appointment: {}", ChronoUnit.HOURS.between(now, appointmentTime));

            String languageCode = userRepository.findLanguageCodeByChatId(appointment.getUsers().getChatId());  // Получаем язык пользователя

            // Проверка за день до сеанса (в пределах от 23 до 24 часов до встречи)
            if (ChronoUnit.HOURS.between(now, appointmentTime) >= 23 && ChronoUnit.HOURS.between(now, appointmentTime) < 24) {
                String message = messageService.getLocalizedMessage("appointment_reminder_tomorrow", languageCode);
                notifyClientAndMaster(appointment, message);
            }

            // Проверка за три часа до сеанса (в пределах от 2 до 3 часов до встречи)
            if (ChronoUnit.HOURS.between(now, appointmentTime) >= 2 && ChronoUnit.HOURS.between(now, appointmentTime) < 3) {
                String message = messageService.getLocalizedMessage("appointment_reminder_3hours", languageCode);
                notifyClientAndMaster(appointment, message);
            }
        }
    }

    private void notifyClientAndMaster(Appointment appointment, String message) {
        // Получаем язык клиента
        String clientLanguageCode = userRepository.findLanguageCodeByChatId(appointment.getUsers().getChatId());
        // Уведомление клиенту
        Users client = appointment.getUsers();
        if (client != null && client.getChatId() != null) {
            String clientMessage = message +
                    getLocalizedClientMessage(clientLanguageCode, appointment);
            log.info("Sending notification to client {}: {}", client.getChatId(), clientMessage);
            messageService.sendMessage(client.getChatId(), clientMessage);
        }

        // Получаем язык мастера
        String masterLanguageCode = userRepository.findLanguageCodeByChatId(appointment.getMaster().getChatId());

        // Уведомление мастеру
        Master master = appointment.getMaster();
        if (master != null && master.getChatId() != null) {
            String masterMessage = message + getLocalizedMasterMessage(masterLanguageCode, appointment);
            log.info("Sending notification to master {}: {}", master.getChatId(), masterMessage);
            messageService.sendMessage(master.getChatId(), masterMessage);
        }
    }

    private String getLocalizedClientMessage(String languageCode, Appointment appointment) {
        // Получаем локализованные сообщения для разных языков
        String masterLabel = messageService.getLocalizedMessage("appointment.master", languageCode);
        String serviceLabel = messageService.getLocalizedMessage("appointment.service", languageCode);
        String dateLabel = messageService.getLocalizedMessage("appointment.date", languageCode);
        String timeLabel = messageService.getLocalizedMessage("appointment.time", languageCode);

        // Формируем сообщение
        String localizedMessage = String.format("%s: %s\n%s: %s\n%s: %s\n%s: %s",
                masterLabel, appointment.getMaster().getName(),
                serviceLabel, appointment.getServices().getNameEn(), // Default in English
                dateLabel, appointment.getAppointmentDate().toLocalDate(),
                timeLabel, appointment.getAppointmentDate().toLocalTime());

        // Применяем локализацию для услуг в зависимости от языка
        if ("ru".equals(languageCode)) {
            localizedMessage = localizedMessage.replace(appointment.getServices().getNameEn(),
                    appointment.getServices().getNameRu());
        } else if ("uk".equals(languageCode)) {
            localizedMessage = localizedMessage.replace(appointment.getServices().getNameEn(),
                    appointment.getServices().getNameUk());
        }

        return localizedMessage;
    }

    private String getLocalizedMasterMessage(String languageCode, Appointment appointment) {
        // Получаем локализованные сообщения для разных языков
        String clientLabel = messageService.getLocalizedMessage("appointment.client", languageCode);
        String serviceLabel = messageService.getLocalizedMessage("appointment.service", languageCode);
        String dateLabel = messageService.getLocalizedMessage("appointment.date", languageCode);
        String timeLabel = messageService.getLocalizedMessage("appointment.time", languageCode);

        // Формируем сообщение
        String localizedMessage = String.format("%s: %s %s\n%s: %s\n%s: %s\n%s: %s",
                clientLabel, appointment.getUsers().getFirstName(), appointment.getUsers().getLastName(),
                serviceLabel, appointment.getServices().getNameEn(), // Default in English
                dateLabel, appointment.getAppointmentDate().toLocalDate(),
                timeLabel, appointment.getAppointmentDate().toLocalTime());

        // Применяем локализацию для услуги в зависимости от языка
        if ("ru".equals(languageCode)) {
            localizedMessage = localizedMessage.replace(appointment.getServices().getNameEn(),
                    appointment.getServices().getNameRu());
        } else if ("uk".equals(languageCode)) {
            localizedMessage = localizedMessage.replace(appointment.getServices().getNameEn(),
                    appointment.getServices().getNameUk());
        }

        return localizedMessage;
    }

    @Scheduled(cron = "0 0 * * * *") // Runs every hour to check completed appointments
    public void markCompletedAppointmentsAndRequestFeedback() {
        log.info("NotificationService: markCompletedAppointmentsAndRequestFeedback started");
        LocalDateTime now = LocalDateTime.now(clock);
        List<Appointment> appointments = appointmentRepository.findAll();

        for (Appointment appointment : appointments) {
            LocalDateTime appointmentEndTime = appointment.getAppointmentDate();

            // Check if the appointment ended more than five hours ago
            long hoursSinceAppointment = ChronoUnit.HOURS.between(appointmentEndTime, now);
            if (hoursSinceAppointment >= 5 && !appointment.getStatus().equals(Appointment.Status.COMPLETED)) {
                // Mark appointment as completed if not already marked
                appointment.setStatus(Appointment.Status.COMPLETED);
                appointmentRepository.save(appointment);

                // Request feedback and rating from the client
                Users client = appointment.getUsers();
                if (client != null && client.getChatId() != null) {
                    String languageCode = userRepository.findLanguageCodeByChatId(client.getChatId());  // Get client language

                    // Form the feedback request based on the client's language
                    String feedbackRequest = messageService.getLocalizedMessage("appointment.completed.feedback_request", languageCode, appointment.getServices().getNameEn(), appointment.getMaster().getName());

                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                    InlineKeyboardButton reviewButton = new InlineKeyboardButton(
                            messageService.getLocalizedMessage("review_button", languageCode) // Use localized button label
                    );
                    reviewButton.setCallbackData("/confirm_review_" + appointment.getId());
                    rows.add(List.of(reviewButton));

                    keyboard.setKeyboard(rows);
                    log.info("Sending feedback request to client {}: {}", client.getChatId(), feedbackRequest);
                    messageService.sendMessageWithInlineKeyboard(client.getChatId(), feedbackRequest, keyboard);
                }
            }
        }
    }

}

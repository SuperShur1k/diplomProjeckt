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
    private final MasterRepository masterRepository;
    private final UserSession userSession;
    private final UserRepository userRepository;

    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public NotificationService(AppointmentRepository appointmentRepository, MessageService messageService, MasterRepository masterRepository, UserSession userSession, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.messageService = messageService;
        this.masterRepository = masterRepository;
        this.userSession = userSession;
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
                String message = "ru".equals(languageCode)
                        ? "Напоминание: Ваша запись назначена на завтра."
                        : "uk".equals(languageCode)
                        ? "Нагадування: Ваш запис призначений на завтра."
                        : "Reminder: Your appointment is scheduled for tomorrow.";
                notifyClientAndMaster(appointment, message);
            }

            // Проверка за три часа до сеанса (в пределах от 2 до 3 часов до встречи)
            if (ChronoUnit.HOURS.between(now, appointmentTime) >= 2 && ChronoUnit.HOURS.between(now, appointmentTime) < 3) {
                String message = "ru".equals(languageCode)
                        ? "Напоминание: Ваша запись через 3 часа."
                        : "uk".equals(languageCode)
                        ? "Нагадування: Ваш запис через 3 години."
                        : "Reminder: Your appointment is in 3 hours.";
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
        String localizedMessage = "\nMaster: " + appointment.getMaster().getName() +
                "\nService: " + appointment.getServices().getNameEn() +
                "\nDate: " + appointment.getAppointmentDate().toLocalDate() +
                "\nTime: " + appointment.getAppointmentDate().toLocalTime();

        if ("ru".equals(languageCode)) {
            localizedMessage = "\nМастер: " + appointment.getMaster().getName() +
                    "\nУслуга: " + appointment.getServices().getNameRu() +
                    "\nДата: " + appointment.getAppointmentDate().toLocalDate() +
                    "\nВремя: " + appointment.getAppointmentDate().toLocalTime();
        } else if ("uk".equals(languageCode)) {
            localizedMessage = "\nМайстер: " + appointment.getMaster().getName() +
                    "\nПослуга: " + appointment.getServices().getNameUk() +
                    "\nДата: " + appointment.getAppointmentDate().toLocalDate() +
                    "\nЧас: " + appointment.getAppointmentDate().toLocalTime();
        }

        return localizedMessage;
    }

    private String getLocalizedMasterMessage(String languageCode, Appointment appointment) {
        String localizedMessage = "\nClient: " + appointment.getUsers().getFirstName() + " " + appointment.getUsers().getLastName() +
                "\nService: " + appointment.getServices().getNameEn() +
                "\nDate: " + appointment.getAppointmentDate().toLocalDate() +
                "\nTime: " + appointment.getAppointmentDate().toLocalTime();

        if ("ru".equals(languageCode)) {
            localizedMessage = "\nКлиент: " + appointment.getUsers().getFirstName() + " " + appointment.getUsers().getLastName() +
                    "\nУслуга: " + appointment.getServices().getNameRu() +
                    "\nДата: " + appointment.getAppointmentDate().toLocalDate() +
                    "\nВремя: " + appointment.getAppointmentDate().toLocalTime();
        } else if ("uk".equals(languageCode)) {
            localizedMessage = "\nКлієнт: " + appointment.getUsers().getFirstName() + " " + appointment.getUsers().getLastName() +
                    "\nПослуга: " + appointment.getServices().getNameUk() +
                    "\nДата: " + appointment.getAppointmentDate().toLocalDate() +
                    "\nЧас: " + appointment.getAppointmentDate().toLocalTime();
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

            // Check if the appointment ended more than three hours ago
            long hoursSinceAppointment = ChronoUnit.HOURS.between(appointmentEndTime, now);
            if (hoursSinceAppointment >= 5 && appointment.getStatus() != Appointment.Status.COMPLETED) {
                // Mark appointment as completed
                appointment.setStatus(Appointment.Status.COMPLETED);
                appointmentRepository.save(appointment);

                // Request feedback and rating from the client
                Users client = appointment.getUsers();
                if (client != null && client.getChatId() != null) {
                    String languageCode = userRepository.findLanguageCodeByChatId(client.getChatId());  // Получаем язык клиента

                    // Form the feedback request based on the client's language
                    String feedbackRequest = getLocalizedFeedbackRequest(languageCode, appointment);

                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                    InlineKeyboardButton reviewButton = new InlineKeyboardButton(
                            "ru".equals(languageCode) ? "Оставить отзыв" :
                                    "uk".equals(languageCode) ? "Залишити відгук" : "Review Appointment"
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

    private String getLocalizedFeedbackRequest(String languageCode, Appointment appointment) {
        String feedbackRequest = "Your appointment for " + appointment.getServices().getNameEn() +
                " with " + appointment.getMaster().getName() + " has been completed. We would love to hear your feedback!\n" +
                "Please rate the service from 1 to 5 and leave a comment.";

        if ("ru".equals(languageCode)) {
            feedbackRequest = "Ваша запись на услугу " + appointment.getServices().getNameRu() +
                    " с мастером " + appointment.getMaster().getName() + " завершена. Мы будем рады услышать ваш отзыв!\n" +
                    "Пожалуйста, оцените услугу от 1 до 5 и оставьте комментарий.";
        } else if ("uk".equals(languageCode)) {
            feedbackRequest = "Ваш запис на послугу " + appointment.getServices().getNameUk() +
                    " з майстром " + appointment.getMaster().getName() + " завершено. Ми будемо раді почути ваш відгук!\n" +
                    "Будь ласка, оцініть послугу від 1 до 5 і залиште коментар.";
        }

        return feedbackRequest;
    }

}

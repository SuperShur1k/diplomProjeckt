package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.model.*;
import com.example.telegramBotNailsBooking.repository.AppointmentRepository;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import com.example.telegramBotNailsBooking.repository.ReviewRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AutUserButtons autUserButtons;

    // Метод для добавления отзыва
    public void addReview(Long userId, Long masterId, int rating, String comment) {
        Users users = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new IllegalArgumentException("Master not found"));

        Review review = new Review();
        review.setUsers(users);
        review.setMaster(master);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);
    }

    public void clientFeedback(Long chatID, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatID);  // Получаем язык пользователя
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        if (appointment == null) {
            String message = messageService.getLocalizedMessage("appointment.not_found", languageCode);
            messageService.sendMessage(chatID, message);
            return;
        }

        // Подтверждение отправки отзыва
        String confirmationMessage = messageService.getLocalizedMessage(
                "appointment.feedback_confirmation", languageCode,
                appointment.getAppointmentDate().toLocalDate(), appointment.getAppointmentDate().toLocalTime()
        );

        // Кнопки для подтверждения и отмены
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.send_review", languageCode)
        );
        yesButton.setCallbackData("/review_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("appointment.cancel_review", languageCode)
        );
        noButton.setCallbackData("/keep_appointment");

        List<InlineKeyboardButton> rows = List.of(yesButton, noButton);
        keyboard.setKeyboard(List.of(rows));

        messageService.sendMessageWithInlineKeyboard(chatID, confirmationMessage, keyboard);
    }

    public void askMarkAndFeedback(Long chatID, Long appointmentId) {
        userSession.setRequestingFeedback(chatID, appointmentId);
        // Запрашиваем оценку
        requestRating(chatID);
    }

    // Метод для запроса оценки у клиента
    private void requestRating(Long chatID) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatID);  // Получаем язык пользователя

        // Запрос на оценку с использованием локализованных сообщений
        String message = messageService.getLocalizedMessage("appointment.request_rating", languageCode);

        messageService.sendMessage(chatID, message);
        userSession.setCurrentState(chatID, "/waiting_for_rating_");
    }

    // Метод для запроса комментария после оценки
    public void requestComment(Long chatID, int rating) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatID);  // Получаем язык пользователя

        try {
            if (rating < 1 || rating > 5) {
                // Получаем локализованное сообщение для некорректной оценки
                String message = messageService.getLocalizedMessage("appointment.invalid_rating", languageCode);
                messageService.sendMessage(chatID, message);
                requestRating(chatID);  // Запрос повторно, если оценка некорректна
            } else {
                // Получаем локализованное сообщение для запроса комментария
                String message = messageService.getLocalizedMessage("appointment.thank_you_comment", languageCode);
                messageService.sendMessage(chatID, message);
                userSession.setCurrentState(chatID, "/waiting_for_comment_" + rating);
            }
        } catch (NumberFormatException e) {
            // Получаем локализованное сообщение для неверного ввода
            String message = messageService.getLocalizedMessage("appointment.invalid_input", languageCode);
            messageService.sendMessage(chatID, message);
            requestRating(chatID);
        }
    }

    public void handleCommentResponse(Long chatID, Integer rating, String comment, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatID);  // Получаем язык пользователя

        if (rating != null && appointmentId != null && comment != null) {
            handleClientFeedback(chatID, rating, comment, appointmentId);
        } else if (comment == null) {
            // Получаем локализованное сообщение для запроса комментария
            String message = messageService.getLocalizedMessage("feedback.leave_comment", languageCode);
            messageService.sendMessage(chatID, message);
            userSession.setCurrentState(chatID, "/waiting_for_comment_" + rating);
        } else {
            // Получаем локализованное сообщение для ошибки
            String message = messageService.getLocalizedMessage("feedback.session_interrupted", languageCode);
            messageService.sendMessage(chatID, message);
            autUserButtons.showBookingInfoMenu(chatID);
        }
    }

    public void handleClientFeedback(Long chatId, Integer rating, String comment, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя

        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

            if (appointment != null) {
                // Добавляем отзыв
                addReview(appointment.getUsers().getId(), appointment.getMaster().getId(), rating, comment);

                // Локализованное сообщение для мастера
                String masterNotification = messageService.getLocalizedMessage("feedback.master_notification", languageCode,
                        appointment.getUsers().getFirstName(), appointment.getUsers().getLastName(),
                        appointment.getServices().getNameRu(), appointment.getAppointmentDate().toLocalDate(),
                        appointment.getAppointmentDate().toLocalTime(), rating, comment);

                messageService.sendMessage(appointment.getMaster().getChatId(), masterNotification);

                // Локализованное сообщение для клиента
                String feedbackMessage = messageService.getLocalizedMessage("feedback.thank_you", languageCode);
                messageService.sendMessage(chatId, feedbackMessage);
                autUserButtons.showBookingInfoMenu(chatId);
            } else {
                // Локализованное сообщение об ошибке
                String errorMessage = messageService.getLocalizedMessage("feedback.error_no_appointment", languageCode);
                messageService.sendMessage(chatId, errorMessage);
                autUserButtons.showBookingInfoMenu(chatId);
            }

            // Очищаем запрос на отзыв из сессии
            userSession.clearRequestingFeedback(chatId);
            userSession.clearStates(chatId);
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, "/book_info");
            userSession.setPreviousState(chatId, "/book_service");
        } else {
            // Локализованное сообщение о том, что нет ожидающих отзывов
            String noFeedbackMessage = messageService.getLocalizedMessage("feedback.no_pending_feedback", languageCode);
            messageService.sendMessage(chatId, noFeedbackMessage);
            userSession.clearRequestingFeedback(chatId);
            userSession.clearStates(chatId);
            userSession.clearSession(chatId);
            userSession.setCurrentState(chatId, "/book_info");
            userSession.setPreviousState(chatId, "/book_service");
            autUserButtons.showBookingInfoMenu(chatId);
        }
    }

    public void showMasterListForReview(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        List<Master> masters = masterRepository.findAll();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Для каждого мастера создаем кнопку
        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton(master.getName());
            button.setCallbackData("/show_reviews_" + master.getId());
            rows.add(List.of(button));
        }

        // Кнопка для возврата в меню
        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText(messageService.getLocalizedMessage("menu.back_to_menu", languageCode));
        menuButton.setCallbackData("/main_menu");
        rows.add(List.of(menuButton));

        keyboard.setKeyboard(rows);

        // Сообщение для пользователя
        String message = messageService.getLocalizedMessage("reviews.select_master_for_reviews", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showLastFiveReviewsForMaster(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        List<Review> reviews = reviewRepository.findTop5ByMasterIdOrderByCreatedAtDesc(masterId);

        if (reviews.isEmpty()) {
            // Получаем сообщение, если отзывов нет
            String noReviewsMessage = messageService.getLocalizedMessage("reviews.no_reviews_for_master", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, noReviewsMessage,
                    autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        // Получаем заголовок для списка отзывов
        String reviewText = messageService.getLocalizedMessage("reviews.latest_reviews", languageCode);

        // Добавляем отзывы
        for (Review review : reviews) {
            reviewText += messageService.getLocalizedMessage("reviews.review_details", languageCode, review.getRating(), review.getComment());
        }

        // Создаем кнопку "Назад"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                messageService.getLocalizedMessage("menu.back_to_masters", languageCode)
        );
        backButton.setCallbackData("/review");
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, reviewText, keyboard);
    }
}

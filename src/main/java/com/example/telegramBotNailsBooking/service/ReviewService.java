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
            String message = "ru".equals(languageCode)
                    ? "Запись не найдена."
                    : "uk".equals(languageCode)
                    ? "Запис не знайдено."
                    : "Appointment not found.";
            messageService.sendMessage(chatID, message);
            return;
        }

        // Подтверждение отправки отзыва
        String confirmationMessage = "ru".equals(languageCode)
                ? "Вы уверены, что хотите отправить отзыв для этой записи: " + appointment.getAppointmentDate().toLocalDate() + " в " + appointment.getAppointmentDate().toLocalTime() + "?"
                : "uk".equals(languageCode)
                ? "Ви впевнені, що хочете надіслати відгук для цього запису: " + appointment.getAppointmentDate().toLocalDate() + " о " + appointment.getAppointmentDate().toLocalTime() + "?"
                : "Are you sure you want to send review for this appointment: " + appointment.getAppointmentDate().toLocalDate() + " " + appointment.getAppointmentDate().toLocalTime() + "?";

        // Кнопки для подтверждения и отмены
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton yesButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Да, отправить отзыв" : "uk".equals(languageCode) ? "Так, надіслати відгук" : "Yes, Send Review"
        );
        yesButton.setCallbackData("/review_appointment_" + appointmentId);

        InlineKeyboardButton noButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Нет, не сегодня" : "uk".equals(languageCode) ? "Ні, не сьогодні" : "No, not today"
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

        // Сообщение для запроса оценки
        String message = "ru".equals(languageCode)
                ? "Пожалуйста, поставьте оценку за вашу запись (1-5):"
                : "uk".equals(languageCode)
                ? "Будь ласка, поставте оцінку за вашу запис (1-5):"
                : "Please provide a rating for your appointment (1-5):";

        messageService.sendMessage(chatID, message);
        userSession.setCurrentState(chatID, "/waiting_for_rating_");
    }

    // Метод для запроса комментария после оценки
    public void requestComment(Long chatID, int rating) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatID);  // Получаем язык пользователя

        try {
            if (rating < 1 || rating > 5) {
                String message = "ru".equals(languageCode)
                        ? "Неверная оценка. Пожалуйста, поставьте оценку от 1 до 5."
                        : "uk".equals(languageCode)
                        ? "Невірна оцінка. Будь ласка, поставте оцінку від 1 до 5."
                        : "Invalid rating. Please provide a rating between 1 and 5.";
                messageService.sendMessage(chatID, message);
                requestRating(chatID);  // Запрос повторно, если оценка некорректна
            } else {
                String message = "ru".equals(languageCode)
                        ? "Спасибо! Теперь, пожалуйста, оставьте комментарий о вашем опыте:"
                        : "uk".equals(languageCode)
                        ? "Дякуємо! Тепер, будь ласка, залиште коментар про ваш досвід:"
                        : "Thank you! Now, please leave a comment about your experience:";
                messageService.sendMessage(chatID, message);
                userSession.setCurrentState(chatID, "/waiting_for_comment_" + rating);
            }
        } catch (NumberFormatException e) {
            String message = "ru".equals(languageCode)
                    ? "Неверный ввод. Пожалуйста, введите число от 1 до 5."
                    : "uk".equals(languageCode)
                    ? "Невірний ввід. Будь ласка, введіть число від 1 до 5."
                    : "Invalid input. Please enter a number between 1 and 5.";
            messageService.sendMessage(chatID, message);
            requestRating(chatID);
        }
    }

    public void handleCommentResponse(Long chatID, Integer rating, String comment, Long appointmentId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatID);  // Получаем язык пользователя

        if (rating != null && appointmentId != null && comment != null) {
            handleClientFeedback(chatID, rating, comment, appointmentId);
        } else if (comment == null) {
            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, оставьте не пустой комментарий о вашем опыте:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, залиште непорожній коментар про ваш досвід:"
                    : "Please leave a not empty comment about your experience:";
            messageService.sendMessage(chatID, message);
            userSession.setCurrentState(chatID, "/waiting_for_comment_" + rating);
        } else {
            String message = "ru".equals(languageCode)
                    ? "Сессия отзыва была прервана. Пожалуйста, попробуйте снова."
                    : "uk".equals(languageCode)
                    ? "Сесію відгуку було перервано. Будь ласка, спробуйте ще раз."
                    : "Feedback session was interrupted. Please try again.";
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

                // Сообщение для мастера
                String masterNotification = "ru".equals(languageCode)
                        ? "Клиент " + appointment.getUsers().getFirstName() + " " + appointment.getUsers().getLastName() +
                        " оставил отзыв о процедуре: " + appointment.getServices().getNameRu() + "\n" +
                        "Дата: " + appointment.getAppointmentDate().toLocalDate() + "\n" +
                        "Время: " + appointment.getAppointmentDate().toLocalTime() + "\n" +
                        "Оценка: " + rating + "\n" +
                        "Комментарий: " + comment
                        : "uk".equals(languageCode)
                        ? "Клієнт " + appointment.getUsers().getFirstName() + " " + appointment.getUsers().getLastName() +
                        " залишив відгук про процедуру: " + appointment.getServices().getNameUk() + "\n" +
                        "Дата: " + appointment.getAppointmentDate().toLocalDate() + "\n" +
                        "Час: " + appointment.getAppointmentDate().toLocalTime() + "\n" +
                        "Оцінка: " + rating + "\n" +
                        "Коментар: " + comment
                        : "Client " + appointment.getUsers().getFirstName() + " " + appointment.getUsers().getLastName() +
                        " wrote a review of the procedure: " + appointment.getServices().getNameEn() + "\n" +
                        "Date: " + appointment.getAppointmentDate().toLocalDate() + "\n" +
                        "Time: " + appointment.getAppointmentDate().toLocalTime() + "\n" +
                        "Rating: " + rating + "\n" +
                        "Comment: " + comment;

                messageService.sendMessage(appointment.getMaster().getChatId(), masterNotification);

                // Сообщение клиенту
                String feedbackMessage = "ru".equals(languageCode)
                        ? "Спасибо за ваш отзыв!"
                        : "uk".equals(languageCode)
                        ? "Дякуємо за ваш відгук!"
                        : "Thank you for your feedback!";
                messageService.sendMessage(chatId, feedbackMessage);
                autUserButtons.showBookingInfoMenu(chatId);
            } else {
                String errorMessage = "ru".equals(languageCode)
                        ? "Не удалось найти запись для оставления отзыва."
                        : "uk".equals(languageCode)
                        ? "Не вдалося знайти запис для залишення відгуку."
                        : "Could not find the appointment to leave feedback.";
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
            String noFeedbackMessage = "ru".equals(languageCode)
                    ? "Нет ожидающих отзывов."
                    : "uk".equals(languageCode)
                    ? "Немає очікуючих відгуків."
                    : "No pending feedback request.";
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

        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton(master.getName());
            button.setCallbackData("/show_reviews_" + master.getId());
            rows.add(List.of(button));
        }

        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText(
                "ru".equals(languageCode) ? "Назад в меню" : "uk".equals(languageCode) ? "Назад до меню" : "Back to Menu"
        );
        menuButton.setCallbackData("/start");
        rows.add(List.of(menuButton));

        keyboard.setKeyboard(rows);

        String message = "ru".equals(languageCode)
                ? "Выберите мастера, чтобы увидеть отзывы:"
                : "uk".equals(languageCode)
                ? "Оберіть майстра, щоб побачити відгуки:"
                : "Select a master to see reviews:";
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showLastFiveReviewsForMaster(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        List<Review> reviews = reviewRepository.findTop5ByMasterIdOrderByCreatedAtDesc(masterId);

        if (reviews.isEmpty()) {
            String noReviewsMessage = "ru".equals(languageCode)
                    ? "Не найдено отзывов для этого мастера."
                    : "uk".equals(languageCode)
                    ? "Не знайдено відгуків для цього майстра."
                    : "No reviews found for this master.";

            messageService.sendMessageWithInlineKeyboard(chatId, noReviewsMessage,
                    autUserButtons.getAuthenticatedInlineKeyboard(chatId));
            return;
        }

        String reviewText = "ru".equals(languageCode)
                ? "Последние отзывы:\n\n"
                : "uk".equals(languageCode)
                ? "Останні відгуки:\n\n"
                : "Latest reviews:\n\n";

        for (Review review : reviews) {
            reviewText += "ru".equals(languageCode)
                    ? "Оценка: " + review.getRating() + "\nКомментарий: " + review.getComment() + "\n---\n"
                    : "uk".equals(languageCode)
                    ? "Оцінка: " + review.getRating() + "\nКоментар: " + review.getComment() + "\n---\n"
                    : "Rating: " + review.getRating() + "\nComment: " + review.getComment() + "\n---\n";
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                "ru".equals(languageCode) ? "Назад к мастерам" : "uk".equals(languageCode) ? "Назад до майстрів" : "Back to Masters"
        );
        backButton.setCallbackData("/review");
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, reviewText, keyboard);
    }
}

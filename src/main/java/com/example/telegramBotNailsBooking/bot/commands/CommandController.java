package com.example.telegramBotNailsBooking.bot.commands;

import com.example.telegramBotNailsBooking.model.UserSession;
import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;

@Service
public class CommandController {

    @Autowired
    private AuthenticatedCommandHandler authenticatedCommandHandler;

    @Autowired
    private AdminCommandHandler adminCommandHandler;

    @Autowired
    private UserSession userSession;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    public void handleCommand(Long chatId, String command, Update update) {
        Users users = userRepository.findByChatId(chatId);
        if (users != null) {
            users.setUpdatedAt(LocalDateTime.now());
            userRepository.save(users); // Сохранение изменений в базу данных
        }

        if (userSession.getCurrentState(chatId) == null){
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/main_menu");
        }
        // Обработка общей команды отмены
        if (command.equals("/cancel")) {
            // Проверяем аутентификацию и статус админа
            if (userService.isAdmin(chatId)) {
                adminCommandHandler.cancel(chatId);
            } else {
                authenticatedCommandHandler.cancel(chatId);
            }
            return;
        } else if (command.equals("/help")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.equals("/review")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/start")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/services")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/select_master_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/show_reviews_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/description_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.equals("/list_commands")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.equals("/contact_admin")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.equals("/ask_new_question")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.equals("/view_requests")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/view_open_requests")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/view_request_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.equals("/view_in_progress_requests")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.startsWith("/progress_request_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/close_request_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/new_question_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.equals("/view_closed_requests")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (command.startsWith("/closed_request_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/delete_request_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/contact_master")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/contact_master_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/change_name")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/change_first_name")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/confirm_change_first_name_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/cancel_change_first_name")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/change_last_name")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/confirm_change_last_name_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/cancel_change_last_name")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/change_language")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/lang_ru")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/lang_en")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/lang_uk")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/change_phone")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/confirm_change_phone_number_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/cancel_change_phone_number_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/reply_to_admin_master_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/reply_to_admin_user_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/master")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/view_appointments")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/appointments_for_date_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/appointment_details_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_cancel_appointment_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_reschedule_appointment_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_select_transfer_date_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_select_transfer_time_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/message_client_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/reply_to_master_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/master_write_to_client")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/master_cancel_appointment")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/master_record_client")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/master_cancel_appointment_master")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_service_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_date_master_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_time_master_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/master_write_client")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.startsWith("/master_appointment_details_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (command.equals("/admin_cancel_appointment")) {
            adminCommandHandler.handleAdminCommand(chatId, command);
            return;
        }

        String currentState = userSession.getCurrentState(chatId);

        if (currentState.equals("/new_question")){
            userService.handleNewQuestion(chatId, command);
            return;
        } else if (currentState.equals("/contact_admin")) {
            userService.contactAdmin(chatId);
            return;
        }else if (currentState.equals("/view_requests")) {
            userService.viewRequests(chatId);
            return;
        }else if (currentState.startsWith("/writing_to_admin_from_master_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }else if (currentState.startsWith("/writing_to_admin_from_user_")){
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.startsWith("/master_write_to_client_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.startsWith("/writing_to_master_from_user_")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/view_appointments")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master_write_to_client")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master_record_client")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master_add_new_client")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master_add_new_client_last_name")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master_service")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master_date")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        } else if (currentState.equals("/master_time")) {
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
            return;
        }

        // Проверка на админ-команды
        if (userService.isAdmin(chatId)) {
            adminCommandHandler.handleAdminCommand(chatId, command);
        } else {
            // Если команда не является административной или пользователь не админ
            authenticatedCommandHandler.handleAuthenticatedCommand(chatId, command, update);
        }
    }
}

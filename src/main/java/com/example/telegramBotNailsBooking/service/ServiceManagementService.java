package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.model.Master;
import com.example.telegramBotNailsBooking.model.Services;
import com.example.telegramBotNailsBooking.model.UserSession;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import com.example.telegramBotNailsBooking.repository.ServiceRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.buttons.AdminButtons;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ServiceManagementService {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private AdminButtons adminButtons;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    public void initiateAddService(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя

        // Получаем локализованное сообщение для запроса выбора мастера
        String message = messageService.getLocalizedMessage("service.select_master_message", languageCode);

        List<Master> masters = masterRepository.findAll();

        // Проверяем, есть ли мастера
        if (masters.isEmpty()) {
            String noMastersMessage = messageService.getLocalizedMessage("service.no_masters_message", languageCode);
            messageService.sendMessage(chatId, noMastersMessage);
            return;
        }

        // Генерируем кнопки мастеров
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName());
            button.setCallbackData("/add_serviceMaster_" + master.getId());
            rows.add(List.of(button));
        }

        // Кнопка "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(messageService.getLocalizedMessage("service.cancel_button", languageCode));
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        // Отправляем клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        // Инициализация сессии
        userSession.setServiceInfo(chatId, new String[8]); // ID мастера, название, описание, цена
        userSession.setPreviousState(chatId, "/add_service");
    }

    public void handleAddService(Long chatId, String input) {
        String[] serviceInfo = userSession.getServiceInfo(chatId);
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        if (serviceInfo == null) {
            sendError(chatId, languageCode,
                    messageService.getLocalizedMessage("service.error.process_not_started", languageCode),
                    messageService.getLocalizedMessage("service.error.process_not_started_uk", languageCode),
                    messageService.getLocalizedMessage("service.error.process_not_started_en", languageCode));
            return;
        }

        try {
            if (serviceInfo[0] == null) {
                // Устанавливаем ID мастера
                Long masterId = Long.parseLong(input.split("_")[2]);
                serviceInfo[0] = String.valueOf(masterId);

                Master master = masterRepository.findById(masterId).orElse(null);
                if (master == null) {
                    sendError(chatId, languageCode,
                            messageService.getLocalizedMessage("service.error.master_not_found", languageCode),
                            messageService.getLocalizedMessage("service.error.master_not_found_uk", languageCode),
                            messageService.getLocalizedMessage("service.error.master_not_found_en", languageCode));
                    return;
                }

                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.enter_name_ru", languageCode));
            } else if (serviceInfo[1] == null) {
                // Устанавливаем название на русском
                serviceInfo[1] = input;
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.enter_name_uk", languageCode));
            } else if (serviceInfo[2] == null) {
                // Устанавливаем название на украинском
                serviceInfo[2] = input;
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.enter_name_en", languageCode));
            } else if (serviceInfo[3] == null) {
                // Устанавливаем название на английском
                serviceInfo[3] = input;
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.enter_description_ru", languageCode));
            } else if (serviceInfo[4] == null) {
                // Устанавливаем описание на русском
                serviceInfo[4] = input;
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.enter_description_uk", languageCode));
            } else if (serviceInfo[5] == null) {
                // Устанавливаем описание на украинском
                serviceInfo[5] = input;
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.enter_description_en", languageCode));
            } else if (serviceInfo[6] == null) {
                // Устанавливаем описание на английском
                serviceInfo[6] = input;
                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.enter_price", languageCode));
            } else if (serviceInfo[7] == null) {
                // Устанавливаем цену и сохраняем услугу
                Double price = Double.parseDouble(input);
                serviceInfo[7] = String.valueOf(price);

                Long masterId = Long.parseLong(serviceInfo[0]);
                Master master = masterRepository.findById(masterId).orElse(null);
                if (master == null) {
                    sendError(chatId, languageCode,
                            messageService.getLocalizedMessage("service.error.master_not_found", languageCode),
                            messageService.getLocalizedMessage("service.error.master_not_found_uk", languageCode),
                            messageService.getLocalizedMessage("service.error.master_not_found_en", languageCode));
                    return;
                }

                Services service = new Services();
                service.setNameRu(serviceInfo[1]);
                service.setNameUk(serviceInfo[2]);
                service.setNameEn(serviceInfo[3]);
                service.setDescriptionRu(serviceInfo[4]);
                service.setDescriptionUk(serviceInfo[5]);
                service.setDescriptionEn(serviceInfo[6]);
                service.setPrice(price);
                service.setMaster(master);

                serviceRepository.save(service);

                messageService.sendMessage(chatId, messageService.getLocalizedMessage("service.success_added", languageCode));
                adminButtons.getServiceInlineKeyboard(chatId, messageService);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
            }
        } catch (NumberFormatException e) {
            sendError(chatId, languageCode,
                    messageService.getLocalizedMessage("service.error.invalid_price", languageCode),
                    messageService.getLocalizedMessage("service.error.invalid_price_uk", languageCode),
                    messageService.getLocalizedMessage("service.error.invalid_price_en", languageCode));
        }
    }

    private void sendError(Long chatId, String languageCode, String ruMessage, String ukMessage, String enMessage) {
        String message = "ru".equals(languageCode) ? ruMessage : "uk".equals(languageCode) ? ukMessage : enMessage;
        messageService.sendMessage(chatId, message);
    }

    public void initiateRemoveService(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Определяем язык пользователя

        // Получаем локализованное сообщение для выбора мастера
        String message = messageService.getLocalizedMessage("service.remove.select_master", languageCode);

        List<Master> masters = masterRepository.findAll();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для каждого мастера
        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName());
            button.setCallbackData("/del_service_master_" + master.getId());
            rows.add(List.of(button));
        }

        // Кнопка "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(messageService.getLocalizedMessage("service.remove.cancel", languageCode));
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        // Инициализируем сессию для хранения информации
        userSession.setServiceInfo(chatId, new String[2]); // [ID мастера, название услуги]
        userSession.setPreviousState(chatId, "/del_service");
    }

    public void handleRemoveService(Long chatId, String callbackData) {
        String[] serviceInfo = userSession.getServiceInfo(chatId);
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        // Проверяем текущий этап процесса
        if (callbackData.startsWith("/del_service_master_")) {
            // Этап 1: Пользователь выбрал мастера
            Long masterId = Long.parseLong(callbackData.split("_")[3]);
            serviceInfo[0] = String.valueOf(masterId); // Сохраняем ID мастера

            // Получаем услуги для выбранного мастера
            List<Services> services = serviceRepository.findByMasterId(masterId);

            if (services.isEmpty()) {
                // Локализованное сообщение
                String message = messageService.getLocalizedMessage("service.remove.no_services", languageCode);
                messageService.sendMessage(chatId, message);
                return;
            }

            // Создаем кнопки для выбора услуг
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (Services service : services) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(messageService.getLocalizedServiceName(service, languageCode));
                button.setCallbackData("/del_service_" + service.getId());
                rows.add(List.of(button));
            }

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);

            // Локализованное сообщение
            String message = messageService.getLocalizedMessage("service.remove.select_service", languageCode);
            messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
        } else if (callbackData.startsWith("/del_service_")) {
            // Этап 2: Пользователь выбрал услугу
            Long serviceId = Long.parseLong(callbackData.split("_")[2]);

            // Проверяем, существует ли услуга
            Optional<Services> serviceOpt = serviceRepository.findById(serviceId);

            if (serviceOpt.isPresent()) {
                Services service = serviceOpt.get();

                // Удаляем услугу
                serviceRepository.delete(service);

                // Локализованное сообщение
                String message = messageService.getLocalizedMessage("service.remove.success", languageCode, service.getNameRu(), service.getNameUk(), service.getNameEn());
                messageService.sendMessage(chatId, message);

                // Возвращаемся в меню администрирования
                adminButtons.getServiceInlineKeyboard(chatId, messageService);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
                userSession.setPreviousState(chatId, "/admin");
                userSession.setCurrentState(chatId, "/service");
            } else {
                // Локализованное сообщение
                String message = messageService.getLocalizedMessage("service.remove.not_found", languageCode);
                messageService.sendMessage(chatId, message);
            }
        } else {
            // Некорректный callbackData
            String message = messageService.getLocalizedMessage("service.remove.invalid_request", languageCode);
            messageService.sendMessage(chatId, message);
        }
    }

    public void showMasterListForServiceSelection(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        List<Master> masters = masterRepository.findAll();

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для мастеров
        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton(master.getName());
            button.setCallbackData("/select_master_" + master.getId());
            rows.add(List.of(button));
        }

        // Добавляем кнопку "Back to Menu" в зависимости от языка
        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText(messageService.getLocalizedMessage("menu.back", languageCode));
        menuButton.setCallbackData("/back");
        rows.add(List.of(menuButton));

        keyboard.setKeyboard(rows);

        // Сообщение в зависимости от языка
        String message = messageService.getLocalizedMessage("service.select_master", languageCode);

        userSession.clearStates(chatId);
        userSession.setPreviousState(chatId, "/main_menu");
        userSession.setCurrentState(chatId, "/services");

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showServicesForSelectedMaster(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        Master master = masterRepository.findById(masterId).orElse(null);

        if (master == null) {
            String message = messageService.getLocalizedMessage("master.not_found", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        List<Services> services = serviceRepository.findByMasterId(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Кнопка с именем услуги
            InlineKeyboardButton serviceButton = new InlineKeyboardButton(
                    messageService.getLocalizedMessage("service.name", languageCode, service.getNameRu(), service.getNameUk(), service.getNameEn()));
            serviceButton.setCallbackData("/select_master");

            // Кнопка с ценой услуги
            InlineKeyboardButton priceButton = new InlineKeyboardButton(service.getPrice() + "€ ↑ ↑");
            priceButton.setCallbackData("/select_master"); // Не добавляем действие на кнопку цены

            InlineKeyboardButton descriptionButton = new InlineKeyboardButton(
                    messageService.getLocalizedMessage("service.description", languageCode)
            );
            descriptionButton.setCallbackData("/description_" + service.getId());

            // Добавляем в одну строку имя услуги и цену
            rows.add(List.of(serviceButton));
            rows.add(List.of(descriptionButton, priceButton));
        }

        // Кнопка "Back to Masters" для возврата к списку мастеров, добавляется в отдельную строку
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("menu.back_to_masters", languageCode));
        backButton.setCallbackData("/services");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String servicesMessage = messageService.getLocalizedMessage("service.list_for_master", languageCode, master.getName());
        messageService.sendMessageWithInlineKeyboard(chatId, servicesMessage, keyboard);
    }

    public void initialChangeCost(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя

        // Локализуем сообщение
        String message = messageService.getLocalizedMessage("service.change_cost_prompt", languageCode);

        List<Master> masters = masterRepository.findAll();

        // Проверяем, есть ли мастера
        if (masters.isEmpty()) {
            String noMastersMessage = messageService.getLocalizedMessage("service.no_masters", languageCode);
            messageService.sendMessage(chatId, noMastersMessage);
            return;
        }

        // Генерируем кнопки мастеров
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Master master : masters) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(master.getName());
            button.setCallbackData("/change_cost_service_master_" + master.getId());
            rows.add(List.of(button));
        }

        // Кнопка "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(messageService.getLocalizedMessage("button.cancel", languageCode));
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        // Отправляем клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void selectServiceChangeCost(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        Master master = masterRepository.findById(masterId).orElse(null);

        if (master == null) {
            String message = messageService.getLocalizedMessage("master.not_found", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        List<Services> services = serviceRepository.findByMasterId(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Кнопка с именем услуги
            InlineKeyboardButton serviceButton = new InlineKeyboardButton(messageService.getLocalizedServiceName(service, languageCode));
            serviceButton.setCallbackData("/admin_select_service_change_cost_" + service.getId() + "_" + masterId);
            // Добавляем в одну строку имя услуги
            rows.add(List.of(serviceButton));
        }

        // Кнопка "Back to Masters"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(messageService.getLocalizedMessage("button.back_to_masters", languageCode));
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String servicesMessage = messageService.getLocalizedMessage("service.select_for_change_cost", languageCode);
        messageService.sendMessageWithInlineKeyboard(chatId, servicesMessage, keyboard);
    }

    public void changeServiceCost(Long chatId, Long serviceId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Services service = serviceRepository.findById(serviceId).orElse(null);

        if (service == null) {
            String message = messageService.getLocalizedMessage("service.not_found", languageCode);
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем идентификатор услуги в сессии пользователя
        userSession.setTempData(chatId, "serviceId", String.valueOf(serviceId));

        String message = messageService.getLocalizedMessage("service.change_cost", languageCode, service.getNameRu(), service.getNameUk(), service.getNameEn());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(messageService.getLocalizedMessage("button.cancel", languageCode));
        cancelButton.setCallbackData("/cancel");
        rows.add(List.of(cancelButton));

        keyboard.setKeyboard(rows);

        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);

        // Устанавливаем состояние для ожидания ввода цены
        userSession.setCurrentState(chatId, "/enter_new_service_cost");
    }

    public void finalChangeCost(Long chatId, String text) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        if ("/cancel".equals(text)) {
            // Если пользователь выбрал "Отмена"
            userSession.clearStates(chatId);
            userSession.clearTempData(chatId);

            String cancelMessage = messageService.getLocalizedMessage("price_change_cancelled", languageCode);

            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/service");
            userSession.setPreviousState(chatId, "/admin");

            messageService.sendMessage(chatId, cancelMessage);
            adminButtons.getServiceInlineKeyboard(chatId, messageService);
            return;
        }

        try {
            // Получаем введенную цену
            Double newCost = Double.parseDouble(text.trim());
            Long serviceId = Long.parseLong(userSession.getTempData(chatId, "serviceId"));

            Services service = serviceRepository.findById(serviceId).orElse(null);

            if (service == null) {
                String message = messageService.getLocalizedMessage("service_not_found", languageCode);
                userSession.clearSession(chatId);
                userSession.clearTempData(chatId);
                userSession.clearStates(chatId);
                userSession.setCurrentState(chatId, "/service");
                userSession.setPreviousState(chatId, "/admin");

                messageService.sendMessage(chatId, message);
                adminButtons.getServiceInlineKeyboard(chatId, messageService);
                return;
            }

            // Обновляем цену услуги
            service.setPrice(newCost);
            serviceRepository.save(service);

            String successMessage = messageService.getLocalizedMessage("price_updated_successfully", languageCode, newCost, service.getNameRu(), service.getNameUk(), service.getNameEn());

            messageService.sendMessage(chatId, successMessage);
            adminButtons.getServiceInlineKeyboard(chatId, messageService);

            // Сбрасываем состояние
            userSession.clearSession(chatId);
            userSession.clearTempData(chatId);
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/service");
            userSession.setPreviousState(chatId, "/admin");

        } catch (NumberFormatException e) {
            String errorMessage = messageService.getLocalizedMessage("invalid_price_format", languageCode);
            messageService.sendMessage(chatId, errorMessage);
        }
    }
}
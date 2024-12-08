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

        String message = "ru".equals(languageCode)
                ? "Пожалуйста, выберите мастера, для которого хотите добавить услугу:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть майстра, для якого хочете додати послугу:"
                : "Please select the master for whom you want to add a service:";

        List<Master> masters = masterRepository.findAll();

        // Проверяем, есть ли мастера
        if (masters.isEmpty()) {
            messageService.sendMessage(chatId, "ru".equals(languageCode)
                    ? "Нет доступных мастеров для добавления услуги."
                    : "uk".equals(languageCode)
                    ? "Немає доступних майстрів для додавання послуги."
                    : "No available masters to add a service.");
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
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
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
                    "Процесс добавления услуги не был начат. Пожалуйста, начните сначала.",
                    "Процес додавання послуги не був розпочатий. Будь ласка, почніть спочатку.",
                    "Service addition process was not started. Please start over.");
            return;
        }

        try {
            if (serviceInfo[0] == null) {
                // Устанавливаем ID мастера
                Long masterId = Long.parseLong(input.split("_")[2]);
                serviceInfo[0] = String.valueOf(masterId);

                Master master = masterRepository.findById(masterId).orElse(null);
                if (master == null) {
                    sendError(chatId, languageCode, "Мастер не найден.", "Майстра не знайдено.", "Master not found.");
                    return;
                }

                messageService.sendMessage(chatId, "Введите название услуги на русском языке:");
            } else if (serviceInfo[1] == null) {
                // Устанавливаем название на русском
                serviceInfo[1] = input;
                messageService.sendMessage(chatId, "Введите название услуги на украинском языке:");
            } else if (serviceInfo[2] == null) {
                // Устанавливаем название на украинском
                serviceInfo[2] = input;
                messageService.sendMessage(chatId, "Введите название услуги на английском языке:");
            } else if (serviceInfo[3] == null) {
                // Устанавливаем название на английском
                serviceInfo[3] = input;
                messageService.sendMessage(chatId, "Введите описание услуги на русском языке:");
            } else if (serviceInfo[4] == null) {
                // Устанавливаем описание на русском
                serviceInfo[4] = input;
                messageService.sendMessage(chatId, "Введите описание услуги на украинском языке:");
            } else if (serviceInfo[5] == null) {
                // Устанавливаем описание на украинском
                serviceInfo[5] = input;
                messageService.sendMessage(chatId, "Введите описание услуги на английском языке:");
            } else if (serviceInfo[6] == null) {
                // Устанавливаем описание на английском
                serviceInfo[6] = input;
                messageService.sendMessage(chatId, "Введите цену для новой услуги:");
            } else if (serviceInfo[7] == null) {
                // Устанавливаем цену и сохраняем услугу
                Double price = Double.parseDouble(input);
                serviceInfo[7] = String.valueOf(price);

                Long masterId = Long.parseLong(serviceInfo[0]);
                Master master = masterRepository.findById(masterId).orElse(null);
                if (master == null) {
                    sendError(chatId, languageCode, "Мастер не найден.", "Майстра не знайдено.", "Master not found.");
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

                messageService.sendMessage(chatId, "ru".equals(languageCode)
                        ? "Услуга успешно добавлена!"
                        : "uk".equals(languageCode)
                        ? "Послугу успішно додано!"
                        : "Service added successfully!");
                adminButtons.getServiceInlineKeyboard(chatId, messageService);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
            }
        } catch (NumberFormatException e) {
            sendError(chatId, languageCode,
                    "Неверный формат цены. Введите корректную цену.",
                    "Невірний формат ціни. Введіть правильну ціну.",
                    "Invalid price format. Enter a valid price.");
        }
    }


    private void sendError(Long chatId, String languageCode, String ruMessage, String ukMessage, String enMessage) {
        String message = "ru".equals(languageCode) ? ruMessage : "uk".equals(languageCode) ? ukMessage : enMessage;
        messageService.sendMessage(chatId, message);
    }

    public void initiateRemoveService(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Определяем язык пользователя

        String message = "ru".equals(languageCode)
                ? "Пожалуйста, выберите мастера, для которого хотите удалить услугу:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть майстра, для якого хочете видалити послугу:"
                : "Please select the master for whom you want to delete a service:";

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
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
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
                String message = "ru".equals(languageCode)
                        ? "У выбранного мастера нет доступных услуг для удаления."
                        : "uk".equals(languageCode)
                        ? "У вибраного майстра немає послуг для видалення."
                        : "The selected master has no services available for deletion.";
                messageService.sendMessage(chatId, message);
                return;
            }

            // Создаем кнопки для выбора услуг
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (Services service : services) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("ru".equals(languageCode) ? service.getNameRu() :
                        "uk".equals(languageCode) ? service.getNameUk() :
                                service.getNameEn());
                button.setCallbackData("/del_service_" + service.getId());
                rows.add(List.of(button));
            }

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);

            String message = "ru".equals(languageCode)
                    ? "Пожалуйста, выберите услугу для удаления:"
                    : "uk".equals(languageCode)
                    ? "Будь ласка, оберіть послугу для видалення:"
                    : "Please select a service to delete:";

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

                String message = "ru".equals(languageCode)
                        ? "Услуга '" + service.getNameRu() + "' успешно удалена."
                        : "uk".equals(languageCode)
                        ? "Послугу '" + service.getNameUk() + "' успішно видалено."
                        : "Service '" + service.getNameEn() + "' has been successfully deleted.";
                messageService.sendMessage(chatId, message);

                // Возвращаемся в меню администрирования
                adminButtons.getServiceInlineKeyboard(chatId, messageService);
                userSession.clearStates(chatId);
                userSession.clearSession(chatId);
                userSession.setPreviousState(chatId, "/admin");
                userSession.setCurrentState(chatId, "/service");
            } else {
                String message = "ru".equals(languageCode)
                        ? "Услуга не найдена."
                        : "uk".equals(languageCode)
                        ? "Послугу не знайдено."
                        : "Service not found.";
                messageService.sendMessage(chatId, message);
            }
        } else {
            // Некорректный callbackData
            String message = "ru".equals(languageCode)
                    ? "Некорректный запрос. Пожалуйста, начните процесс удаления услуги заново."
                    : "uk".equals(languageCode)
                    ? "Некоректний запит. Будь ласка, почніть процес видалення послуги заново."
                    : "Invalid request. Please restart the service removal process.";
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
        menuButton.setText(
                "ru".equals(languageCode) ? "Назад в меню" : "uk".equals(languageCode) ? "Назад до меню" : "Back to Menu"
        );
        menuButton.setCallbackData("/back");
        rows.add(List.of(menuButton));

        keyboard.setKeyboard(rows);

        // Сообщение в зависимости от языка
        String message = "ru".equals(languageCode)
                ? "Выберите мастера, чтобы увидеть услуги и цены:"
                : "uk".equals(languageCode)
                ? "Оберіть майстра, щоб побачити послуги та ціни:"
                : "Select a master to see services and prices:";
        userSession.clearStates(chatId);
        userSession.setPreviousState(chatId, "/main_menu");
        userSession.setCurrentState(chatId, "/services");
        messageService.sendMessageWithInlineKeyboard(chatId, message, keyboard);
    }

    public void showServicesForSelectedMaster(Long chatId, Long masterId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);  // Получаем язык пользователя
        Master master = masterRepository.findById(masterId).orElse(null);

        if (master == null) {
            String message = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        List<Services> services = serviceRepository.findByMasterId(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Кнопка с именем услуги
            InlineKeyboardButton serviceButton = new InlineKeyboardButton("ru".equals(languageCode) ? service.getNameRu() :
                    "uk".equals(languageCode) ? service.getNameUk() :
                            service.getNameEn());
            serviceButton.setCallbackData("/select_master");

            // Кнопка с ценой услуги
            InlineKeyboardButton priceButton = new InlineKeyboardButton(service.getPrice() + "€ ↑ ↑");
            priceButton.setCallbackData("/select_master"); // Не добавляем действие на кнопку цены

            InlineKeyboardButton descriptionButton = new InlineKeyboardButton("ru".equals(languageCode) ? "↑ ↑ Описание" :
                    "uk".equals(languageCode) ? "↑ ↑ Опис" :
                            "↑ ↑ Description");
            descriptionButton.setCallbackData("/description_" + service.getId());

            // Добавляем в одну строку имя услуги и цену
            rows.add(List.of(serviceButton));
            rows.add(List.of(descriptionButton, priceButton));
        }

        // Кнопка "Back to Masters" для возврата к списку мастеров, добавляется в отдельную строку
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(
                "ru".equals(languageCode) ? "Назад к мастерам" :
                        "uk".equals(languageCode) ? "Назад до майстрів" : "Back to Masters"
        );
        backButton.setCallbackData("/services");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String servicesMessage = "ru".equals(languageCode)
                ? "Услуги и цены для " + master.getName() + ":"
                : "uk".equals(languageCode)
                ? "Послуги та ціни для " + master.getName() + ":"
                : "Services and prices for " + master.getName() + ":";

        messageService.sendMessageWithInlineKeyboard(chatId, servicesMessage, keyboard);
    }

    public void initialChangeCost(Long chatId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId); // Получаем язык пользователя

        String message = "ru".equals(languageCode)
                ? "Пожалуйста, выберите мастера, для которого хотите изменить цену услуги:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть майстра, для якого хочете змінити ціну послуги:"
                : "Please select the master for whom you want to change the service cost:";

        List<Master> masters = masterRepository.findAll();

        // Проверяем, есть ли мастера
        if (masters.isEmpty()) {
            messageService.sendMessage(chatId, "ru".equals(languageCode)
                    ? "Нет доступных мастеров для изменения цены услуги."
                    : "uk".equals(languageCode)
                    ? "Немає доступних майстрів для зміни ціни послуги."
                    : "No available masters to change the service cost.");
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
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
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
            String message = "ru".equals(languageCode)
                    ? "Мастер не найден."
                    : "uk".equals(languageCode)
                    ? "Майстра не знайдено."
                    : "Master not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        List<Services> services = serviceRepository.findByMasterId(masterId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Services service : services) {
            // Кнопка с именем услуги
            InlineKeyboardButton serviceButton = new InlineKeyboardButton("ru".equals(languageCode) ? service.getNameRu() :
                    "uk".equals(languageCode) ? service.getNameUk() :
                            service.getNameEn());
            serviceButton.setCallbackData("/admin_select_service_change_cost_" + service.getId() + "_" + masterId);
            // Добавляем в одну строку имя услуги и цену
            rows.add(List.of(serviceButton));
        }

        // Кнопка "Back to Masters" для возврата к списку мастеров, добавляется в отдельную строку
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(
                "ru".equals(languageCode) ? "Назад к мастерам" :
                        "uk".equals(languageCode) ? "Назад до майстрів" : "Back to Masters"
        );
        backButton.setCallbackData("/back");
        rows.add(List.of(backButton));

        keyboard.setKeyboard(rows);

        String servicesMessage = "ru".equals(languageCode)
                ? "Пожалуйста, выберите услугу, для которой хотите изменить цену:"
                : "uk".equals(languageCode)
                ? "Будь ласка, оберіть послугу, для якої хочете змінити ціну:"
                : "Please select the service for which you want to change the cost:";

        messageService.sendMessageWithInlineKeyboard(chatId, servicesMessage, keyboard);
    }

    public void changeServiceCost(Long chatId, Long serviceId) {
        String languageCode = userRepository.findLanguageCodeByChatId(chatId);

        Services service = serviceRepository.findById(serviceId).orElse(null);

        if (service == null) {
            String message = "ru".equals(languageCode)
                    ? "Услуга не найдена."
                    : "uk".equals(languageCode)
                    ? "Послугу не знайдено."
                    : "Service not found.";
            messageService.sendMessage(chatId, message);
            return;
        }

        // Сохраняем идентификатор услуги в сессии пользователя
        userSession.setTempData(chatId, "serviceId", String.valueOf(serviceId));

        String message = "ru".equals(languageCode)
                ? "Введите новую цену для услуги \"" + service.getNameRu() + "\":"
                : "uk".equals(languageCode)
                ? "Введіть нову ціну для послуги \"" + service.getNameUk() + "\":"
                : "Enter the new price for the service \"" + service.getNameEn() + "\":";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Отмена"
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("ru".equals(languageCode) ? "Отмена" : "uk".equals(languageCode) ? "Скасувати" : "Cancel");
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

            String cancelMessage = "ru".equals(languageCode)
                    ? "Изменение цены отменено."
                    : "uk".equals(languageCode)
                    ? "Зміну ціни скасовано."
                    : "Price change canceled.";

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
                String message = "ru".equals(languageCode)
                        ? "Услуга не найдена."
                        : "uk".equals(languageCode)
                        ? "Послугу не знайдено."
                        : "Service not found.";

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

            String successMessage = "ru".equals(languageCode)
                    ? "Цена успешно изменена на " + newCost + " для услуги \"" + service.getNameRu() + "\"."
                    : "uk".equals(languageCode)
                    ? "Ціну успішно змінено на " + newCost + " для послуги \"" + service.getNameUk() + "\"."
                    : "The price has been successfully updated to " + newCost + " for the service \"" + service.getNameEn() + "\".";

            messageService.sendMessage(chatId, successMessage);
            adminButtons.getServiceInlineKeyboard(chatId, messageService);

            // Сбрасываем состояние
            userSession.clearSession(chatId);
            userSession.clearTempData(chatId);
            userSession.clearStates(chatId);
            userSession.setCurrentState(chatId, "/service");
            userSession.setPreviousState(chatId, "/admin");

        } catch (NumberFormatException e) {
            String errorMessage = "ru".equals(languageCode)
                    ? "Ошибка: введите корректную цену."
                    : "uk".equals(languageCode)
                    ? "Помилка: введіть коректну ціну."
                    : "Error: please enter a valid price.";

            messageService.sendMessage(chatId, errorMessage);
        }
    }


}

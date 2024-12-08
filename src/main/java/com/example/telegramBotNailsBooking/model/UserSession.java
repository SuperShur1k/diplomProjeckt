package com.example.telegramBotNailsBooking.model;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserSession {
    private static final Logger logger = LoggerFactory.getLogger(UserSession.class);

    private Map<Long, String[]> userInfo = new HashMap<>();
    private Map<Long, Boolean> settingAdmin = new HashMap<>();
    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, String> previousStates = new HashMap<>();
    private Map<Long, Boolean> settingMaster = new HashMap<>();
    private Map<Long, String[]> masterInfo = new HashMap<>();
    private Map<Long, String[]> dateInfo = new HashMap<>(); // Для добавления и удаления даты
    private Map<Long, String[]> timeInfo = new HashMap<>(); // Для добавления и удаления времени
    private Map<Long, Boolean> awaitingConfirmation = new HashMap<>();
    private Map<Long, String[]> serviceInfoMap = new HashMap<>();
    private Map<Long, String> selectedMaster = new HashMap<>();
    private Map<Long, String> selectedDate = new HashMap<>();
    private Map<Long, String> selectedTimeSlot = new HashMap<>();
    private Map<Long, String> selectedService = new HashMap<>();
    private Map<Long, Long> appointmentToTransfer = new HashMap<>();
    private Map<Long, Long> requestingFeedback = new HashMap<>();
    private final Map<Long, Map<String, String>> temporaryData = new HashMap<>();

    public void setTempData(Long chatId, String key, String value) {
        temporaryData.computeIfAbsent(chatId, k -> new HashMap<>()).put(key, value);
    }

    public String getTempData(Long chatId, String key) {
        return temporaryData.getOrDefault(chatId, new HashMap<>()).get(key);
    }

    public void clearTempData(Long chatId) {
        temporaryData.remove(chatId);
    }

    // Метод для сохранения запроса отзыва
    public void setRequestingFeedback(Long chatId, Long appointmentId) {
        requestingFeedback.put(chatId, appointmentId);
    }

    // Метод для получения ID записи, для которой запрашивается отзыв
    public Long getRequestingFeedback(Long chatId) {
        return requestingFeedback.get(chatId);
    }

    // Метод для очистки запроса на отзыв после его получения
    public void clearRequestingFeedback(Long chatId) {
        requestingFeedback.remove(chatId);
    }

    // Метод для сохранения ID записи для переноса
    public void setAppointmentToTransfer(Long chatId, Long appointmentId) {
        appointmentToTransfer.put(chatId, appointmentId);
    }

    // Метод для получения ID записи для переноса
    public Long getAppointmentToTransfer(Long chatId) {
        return appointmentToTransfer.get(chatId);
    }

    // Метод для удаления ID записи после переноса
    public void clearAppointmentToTransfer(Long chatId) {
        appointmentToTransfer.remove(chatId);
    }

    public void setSelectedMaster(Long chatId, String masterId) {
        selectedMaster.put(chatId, masterId);
    }

    public String getSelectedMaster(Long chatId) {
        return selectedMaster.get(chatId);
    }

    public void setSelectedDate(Long chatId, String dateId) {
        selectedDate.put(chatId, dateId);
    }

    public String getSelectedDate(Long chatId) {
        return selectedDate.get(chatId);
    }

    public void setSelectedTimeSlot(Long chatId, String timeSlotId) {
        selectedTimeSlot.put(chatId, timeSlotId);
    }

    public String getSelectedTimeSlot(Long chatId) {
        return selectedTimeSlot.get(chatId);
    }

    public void setSelectedService(Long chatId, String serviceId) {
        selectedService.put(chatId, serviceId);
    }

    public String getSelectedService(Long chatId) {
        return selectedService.get(chatId);
    }

    public void clearBookingInfo(Long chatId) {
        selectedMaster.remove(chatId);
        selectedDate.remove(chatId);
        selectedTimeSlot.remove(chatId);
        selectedService.remove(chatId);
    }


    public void setServiceInfo(Long chatId, String[] serviceInfo) {
        serviceInfoMap.put(chatId, serviceInfo);
    }

    public String[] getServiceInfo(Long chatId) {
        return serviceInfoMap.get(chatId);
    }

    public void clearServiceInfo(Long chatId) {
        serviceInfoMap.remove(chatId);
    }

    public boolean isAwaitingConfirmation(Long chatId) {
        return awaitingConfirmation.getOrDefault(chatId, false);
    }

    public void setAwaitingConfirmation(Long chatId, boolean value) {
        awaitingConfirmation.put(chatId, value);
    }

    public void clearAwaitingConfirmation(Long chatId) {
        awaitingConfirmation.remove(chatId);
    }


    // Методы для работы с DateInfo (добавление и удаление даты)
    public void setDateInfo(Long chatId, String[] dateInfoData) {
        dateInfo.put(chatId, dateInfoData);
    }

    public String[] getDateInfo(Long chatId) {
        return dateInfo.get(chatId);
    }

    public void clearDateInfo(Long chatId) {
        dateInfo.remove(chatId);
    }

    // Методы для работы с TimeInfo (добавление и удаление времени)
    public void setTimeInfo(Long chatId, String[] timeInfoData) {
        timeInfo.put(chatId, timeInfoData);
    }

    public String[] getTimeInfo(Long chatId) {
        return timeInfo.get(chatId);
    }

    public void clearTimeInfo(Long chatId) {
        timeInfo.remove(chatId);
    }

    public void setSettingAdmin(Long chatId, boolean isSetting) {
        settingAdmin.put(chatId, isSetting);
    }

    public boolean isSettingAdmin(Long chatId) {
        return settingAdmin.getOrDefault(chatId, false);
    }

    public void setSettingMaster(Long chatId, boolean isSetting) {
        settingMaster.put(chatId, isSetting);
    }

    public boolean isSettingMaster(Long chatId) {
        return settingMaster.getOrDefault(chatId, false);
    }

    public void clearSession(Long chatId) {
        settingAdmin.remove(chatId);
        settingMaster.remove(chatId);
        masterInfo.remove(chatId);
        dateInfo.remove(chatId);
        timeInfo.remove(chatId);
        awaitingConfirmation.remove(chatId);
        serviceInfoMap.remove(chatId);
        selectedMaster.remove(chatId);
        selectedDate.remove(chatId);
        selectedTimeSlot.remove(chatId);
        selectedService.remove(chatId);
        appointmentToTransfer.remove(chatId);
        requestingFeedback.remove(chatId);
        userInfo.remove(chatId);
    }

    public void setCurrentState(Long chatId, String currentState) {
        // Сохраняем текущее состояние как предыдущее
        if (userStates.containsKey(chatId)) {
            previousStates.put(chatId, userStates.get(chatId));
        }
        logger.info("Setting current state for chat ID {}: {}", chatId, currentState);
        userStates.put(chatId, currentState); // Устанавливаем новое текущее состояние
    }

    public String getCurrentState(Long chatId) {
        String state = userStates.get(chatId);
        logger.info("Getting current state for chat ID {}: {}", chatId, state);
        return state;  // Возвращаем текущее состояние
    }

    public String getPreviousState(Long chatId) {
        String state = previousStates.get(chatId);
        logger.info("Getting previous state for chat ID {}: {}", chatId, state);
        return state;  // Возвращаем предыдущее состояние
    }

    public void setPreviousState(Long chatId, String previousState) {
        logger.info("Setting previous state for chat ID {}: {}", chatId, previousState);
        previousStates.put(chatId, previousState);  // Устанавливаем предыдущее состояние
    }

    public void clearStates(Long chatId) {
        logger.info("Clearing states for chat ID {}", chatId);
        userStates.remove(chatId);
        previousStates.remove(chatId);
    }

    public void setMasterInfo(Long chatId, String[] info) {
        masterInfo.put(chatId, info);
    }

    public String[] getMasterInfo(Long chatId) {
        return masterInfo.get(chatId);
    }

    public void removeMasterInfo(Long chatId) {
        masterInfo.remove(chatId);
    }

    public void setUserInfo(Long chatId, String[] info) {
        userInfo.put(chatId, info);
    }

    public String[] getUserInfo(Long chatId) {
        return userInfo.get(chatId);
    }
    public void clearUserInfo(Long chatId) {
        userInfo.remove(chatId);
    }
}


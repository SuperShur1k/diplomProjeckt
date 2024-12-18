package com.example.telegramBotNailsBooking.service;

import com.example.telegramBotNailsBooking.model.Help;
import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.repository.HelpRepository;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HelpService {

    private final HelpRepository helpRepository;
    private final UserRepository userRepository;

    @Autowired
    public HelpService(HelpRepository helpRepository, UserRepository userRepository) {
        this.helpRepository = helpRepository;
        this.userRepository = userRepository;
    }

    public Help createHelpRequest(Long userId, String helpQuestion) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Help helpRequest = new Help();
        helpRequest.setUser(user);
        helpRequest.setHelpQuestion(helpQuestion);
        helpRequest.setCreatedAt(LocalDateTime.now());
        helpRequest.setStatus(Help.HelpStatus.WAIT);

        // Сохраняем запрос в базе данных
        return helpRepository.save(helpRequest);
    }
}

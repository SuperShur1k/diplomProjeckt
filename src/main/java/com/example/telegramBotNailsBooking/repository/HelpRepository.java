package com.example.telegramBotNailsBooking.repository;

import com.example.telegramBotNailsBooking.model.Help;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HelpRepository extends JpaRepository<Help, Long> {

    // Найти запросы помощи по идентификатору пользователя и статусу
    List<Help> findByUser_IdAndStatus(Long userId, Help.HelpStatus status);

    List<Help> findByAdmin_IdAndStatus(Long adminId, Help.HelpStatus status);

    // Найти все запросы помощи по статусу
    List<Help> findByStatus(Help.HelpStatus status);

    // Найти запрос помощи по ID
    Optional<Help> findById(Long id);
}



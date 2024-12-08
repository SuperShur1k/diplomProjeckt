package com.example.telegramBotNailsBooking.repository;

import com.example.telegramBotNailsBooking.model.Master;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MasterRepository extends JpaRepository<Master, Long> {

    @Query("SELECT m FROM Master m WHERE m.status = :status")
    List<Master> findAllByStatus(@Param("status") Master.Status status);

    boolean existsByChatId(Long chatId);

    Master findByChatId(Long chatId);

}

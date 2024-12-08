package com.example.telegramBotNailsBooking.repository;

import com.example.telegramBotNailsBooking.model.AvailableDate;
import com.example.telegramBotNailsBooking.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AvailableDateRepository extends JpaRepository<AvailableDate, Long> {
    List<AvailableDate> findByMasterId(Long masterId);

    Optional<AvailableDate> findByMasterIdAndDate(Long masterId, LocalDate date);

    Optional<AvailableDate> findByDate(LocalDate date);

    Optional<AvailableDate> findByDateAndMasterId(LocalDate date, Long masterId);

}

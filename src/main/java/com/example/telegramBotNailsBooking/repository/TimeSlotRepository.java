package com.example.telegramBotNailsBooking.repository;

import com.example.telegramBotNailsBooking.model.AvailableDate;
import com.example.telegramBotNailsBooking.model.TimeSlot;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.availableDate.id = :availableDateId")
    List<TimeSlot> findTimeSlotsByAvailableDateId(@Param("availableDateId") Long availableDateId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TimeSlot ts WHERE ts.availableDate.id = :availableDateId")
    void deleteAllByAvailableDateId(Long availableDateId);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.availableDate = :availableDate AND ts.time = :time")
    List<TimeSlot> findByAvailableDateAndTime(@Param("availableDate") AvailableDate availableDate, @Param("time") LocalTime time);

    TimeSlot findByTimeAndMasterId(LocalTime localTime, Long id);

    boolean existsByAvailableDateAndTime(AvailableDate availableDate, LocalTime time);

    @Query("SELECT t FROM TimeSlot t WHERE t.time = :time AND t.availableDate.id = :dateId AND t.master.id = :masterId")
    Optional<TimeSlot> findByMasterDateAndTime(@Param("time") LocalTime time, @Param("dateId") Long dateId, @Param("masterId") Long masterId);
}


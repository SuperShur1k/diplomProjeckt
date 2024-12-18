package com.example.telegramBotNailsBooking.repository;

import com.example.telegramBotNailsBooking.model.Appointment;
import com.example.telegramBotNailsBooking.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByChatIdAndStatus(Long chatId, Appointment.Status status);

    @Query("SELECT a FROM Appointment a WHERE a.master.id = :masterId")
    List<Appointment> findByMasterId(@Param("masterId") Long masterId);

    @Query("SELECT DISTINCT a.users FROM Appointment a WHERE a.status = :status")
    List<Users> findDistinctUsersByStatus(@Param("status") Appointment.Status status);

    @Query("SELECT a FROM Appointment a WHERE a.master.id = :masterId AND a.status = 'CONFIRMED'")
    List<Appointment> findConfirmedAppointmentsByMaster(@Param("masterId") Long masterId);

}

package com.example.telegramBotNailsBooking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "time_slots")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "available_date_id", nullable = false)
    private AvailableDate availableDate; // Связь с доступной датой

    private LocalTime time; // Время записи

    private boolean isBooked; // Флаг, показывающий, забронирован ли временной слот

    @ManyToOne
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;
}

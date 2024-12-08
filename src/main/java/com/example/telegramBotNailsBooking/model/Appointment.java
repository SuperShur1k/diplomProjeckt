package com.example.telegramBotNailsBooking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users users; // Пользователь, записанный на процедуру

    @ManyToOne
    @JoinColumn(name = "master_id", nullable = false)
    private Master master; // Мастер, выбранный пользователем

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Services services; // Услуга, на которую записан пользователь

    private LocalDateTime appointmentDate; // Дата и время записи

    @Enumerated(EnumType.STRING)
    private Status status; // Enum: CONFIRMED, CANCELLED, COMPLETED

    // Enum для статусов записи
    public enum Status {
        CONFIRMED,
        CANCELLED,
        COMPLETED
    }
}

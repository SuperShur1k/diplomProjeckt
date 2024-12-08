package com.example.telegramBotNailsBooking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "available_dates")
public class AvailableDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date; // Дата и время доступного слота

    @ManyToOne
    @JoinColumn(name = "master_id", nullable = false)
    private Master master; // Мастер, для которого доступна эта дата

}

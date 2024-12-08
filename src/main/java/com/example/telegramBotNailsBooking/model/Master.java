package com.example.telegramBotNailsBooking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "masters")
public class Master {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String socialLink;

    @Enumerated(EnumType.STRING)
    private Status status; // Enum: ACTIVE, INACTIVE

    private Long chatId;

    private String phoneNumber;

    // Enum для статусов
    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
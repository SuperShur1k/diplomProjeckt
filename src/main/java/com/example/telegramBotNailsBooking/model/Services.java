package com.example.telegramBotNailsBooking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "services")
public class Services {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Название услуги
    private String nameRu;
    private String nameUk;
    private String nameEn;

    // Описание услуги
    private String descriptionRu;
    private String descriptionUk;
    private String descriptionEn;

    private Double price; // Цена услуги

    @ManyToOne
    @JoinColumn(name = "master_id", nullable = false)
    private Master master; // Связь с мастером
}

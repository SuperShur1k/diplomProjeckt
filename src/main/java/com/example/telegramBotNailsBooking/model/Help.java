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
@Table(name = "help_requests")
public class Help {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // Пользователь, создавший запрос

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = true)
    private Users admin; // Администратор, который отвечает за запрос (nullable, пока запрос не назначен)

    @Column(name = "help_question", nullable = false)
    private String helpQuestion; // Вопрос, заданный пользователем

    @Column(name = "admin_response", nullable = true)
    private String adminResponse; // Ответ администратора

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Время создания запроса

    @Column(name = "closed_at")
    private LocalDateTime closedAt; // Время закрытия запроса

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HelpStatus status;

    public enum HelpStatus {
        WAIT,
        OPEN,
        CLOSED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // Устанавливаем текущее время при создании
    }
}
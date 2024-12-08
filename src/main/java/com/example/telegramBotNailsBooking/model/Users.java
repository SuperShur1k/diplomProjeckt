package com.example.telegramBotNailsBooking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 50)  // Минимум 2 символа, максимум 50 символов
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)  // Минимум 2 символа, максимум 50 символов
    private String lastName;

    @NotNull
    @Column(unique = true)
    @Pattern(regexp = "^\\+\\d{10,15}$", message = "Invalid phone number. Must start with + and followed by 10–15 digits.")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role; // Enum: CLIENT, ADMIN

    @Column(unique = true)
    private Long chatId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum Role {
        CLIENT,
        ADMIN,
        MASTER
    }

    @NotNull
    @Column(name = "language")
    private String language;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

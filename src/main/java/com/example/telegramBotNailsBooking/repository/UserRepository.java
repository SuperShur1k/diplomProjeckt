package com.example.telegramBotNailsBooking.repository;


import com.example.telegramBotNailsBooking.model.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<Users, Long> {
    Users findByChatId(Long chatId);
    Users findByPhoneNumber(String phoneNumber);

    @Query("SELECT u.language FROM Users u WHERE u.chatId = :chatId")
    String findLanguageCodeByChatId(@Param("chatId") Long chatId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByChatId(Long chatId);

    @Modifying
    @Query("UPDATE Users u SET u.firstName = :newFirstName WHERE u.chatId = :chatId")
    @Transactional
    void updateFirstName(@Param("chatId") Long chatId, @Param("newFirstName") String newFirstName);

    @Modifying
    @Query("UPDATE Users u SET u.lastName = :newLastName WHERE u.chatId = :chatId")
    @Transactional
    void updateLastName(@Param("chatId") Long chatId, @Param("newLastName") String newLastName);

    @Modifying
    @Transactional
    @Query("UPDATE Users u SET u.language = :languageCode WHERE u.chatId = :chatId")
    void updateLanguageCodeByChatId(@Param("chatId") Long chatId, @Param("languageCode") String languageCode);

    @Modifying
    @Transactional
    @Query("UPDATE Users u SET u.phoneNumber = :phoneNumber WHERE u.chatId = :chatId")
    void updatePhoneNumberByChatId(@Param("chatId") Long chatId, @Param("phoneNumber") String phoneNumber);

}

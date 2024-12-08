package com.example.telegramBotNailsBooking.repository;

import com.example.telegramBotNailsBooking.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Метод для поиска отзывов по ID мастера
    List<Review> findByMasterId(Long masterId);

    List<Review> findTop5ByMasterIdOrderByCreatedAtDesc(Long masterId);
}

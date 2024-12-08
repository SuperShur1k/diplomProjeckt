package com.example.telegramBotNailsBooking.repository;

import com.example.telegramBotNailsBooking.model.Services; // Оставляем Service
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Services, Long> {
    @Query("SELECT COUNT(s) > 0 FROM Services s WHERE (s.nameRu = :name OR s.nameUk = :name OR s.nameEn = :name) AND s.master.id = :masterId")
    boolean existsByNameAndMasterId(@Param("name") String name, @Param("masterId") Long masterId);

    @Query("SELECT s FROM Services s WHERE (s.nameRu = :name OR s.nameUk = :name OR s.nameEn = :name) AND s.master.id = :masterId")
    Optional<Services> findByNameAndMasterId(@Param("name") String name, @Param("masterId") Long masterId);

    @Query("DELETE FROM Services s WHERE (s.nameRu = :name OR s.nameUk = :name OR s.nameEn = :name) AND s.master.id = :masterId")
    void deleteByNameAndMasterId(@Param("name") String name, @Param("masterId") Long masterId);

    List<Services> findByMasterId(Long masterId);
}

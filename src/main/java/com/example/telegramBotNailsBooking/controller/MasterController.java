package com.example.telegramBotNailsBooking.controller;

import com.example.telegramBotNailsBooking.model.Master;
import com.example.telegramBotNailsBooking.repository.MasterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/masters")
public class MasterController {

    private final MasterRepository masterRepository;

    public MasterController(MasterRepository masterRepository) {
        this.masterRepository = masterRepository;
    }

    // Получить всех мастеров (GET)
    @GetMapping
    public List<Master> getAllMasters() {
        return masterRepository.findAll();
    }

    // Получить мастера по ID (GET)
    @GetMapping("/{id}")
    public ResponseEntity<Master> getMasterById(@PathVariable Long id) {
        Optional<Master> master = masterRepository.findById(id);
        return master.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Создать мастера (POST)
    @PostMapping
    public ResponseEntity<Master> createMaster(@RequestBody Master master) {
        Master savedMaster = masterRepository.save(master);
        return ResponseEntity.ok(savedMaster);
    }

    // Удалить мастера (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaster(@PathVariable Long id) {
        if (masterRepository.existsById(id)) {
            masterRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

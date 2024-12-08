package com.example.telegramBotNailsBooking.controller;

import com.example.telegramBotNailsBooking.model.Help;
import com.example.telegramBotNailsBooking.repository.HelpRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/help")
public class HelpController {

    private final HelpRepository helpRepository;

    public HelpController(HelpRepository helpRepository) {
        this.helpRepository = helpRepository;
    }

    // Получить все запросы (GET)
    @GetMapping
    public List<Help> getAllHelpRequests() {
        return helpRepository.findAll();
    }

    // Получить запрос по ID (GET)
    @GetMapping("/{id}")
    public ResponseEntity<Help> getHelpRequestById(@PathVariable Long id) {
        Optional<Help> help = helpRepository.findById(id);
        return help.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Создать новый запрос (POST)
    @PostMapping
    public ResponseEntity<Help> createHelpRequest(@RequestBody Help help) {
        Help savedHelp = helpRepository.save(help);
        return ResponseEntity.ok(savedHelp);
    }

    // Обновить статус запроса (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Help> updateHelpRequest(@PathVariable Long id, @RequestBody Help updatedHelp) {
        if (helpRepository.existsById(id)) {
            updatedHelp.setId(id);
            Help savedHelp = helpRepository.save(updatedHelp);
            return ResponseEntity.ok(savedHelp);
        }
        return ResponseEntity.notFound().build();
    }

    // Удалить запрос (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHelpRequest(@PathVariable Long id) {
        if (helpRepository.existsById(id)) {
            helpRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

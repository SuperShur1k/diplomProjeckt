package com.example.telegramBotNailsBooking.controller;

import com.example.telegramBotNailsBooking.model.Services;
import com.example.telegramBotNailsBooking.repository.ServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    // Получить все услуги (GET)
    @GetMapping
    public List<Services> getAllServices() {
        return serviceRepository.findAll();
    }

    // Получить услугу по ID (GET)
    @GetMapping("/{id}")
    public ResponseEntity<Services> getServiceById(@PathVariable Long id) {
        Optional<Services> service = serviceRepository.findById(id);
        return service.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Создать услугу (POST)
    @PostMapping
    public ResponseEntity<Services> createService(@RequestBody Services service) {
        Services savedService = serviceRepository.save(service);
        return ResponseEntity.ok(savedService);
    }

    // Удалить услугу (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        if (serviceRepository.existsById(id)) {
            serviceRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

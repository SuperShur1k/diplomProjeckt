package com.example.telegramBotNailsBooking.controller;

import com.example.telegramBotNailsBooking.model.*;
import com.example.telegramBotNailsBooking.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@Secured("ROLE_ADMIN")
public class AdminController {

    private final UserRepository userRepository;
    private final MasterRepository masterRepository;
    private final ServiceRepository serviceRepository;
    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    public AdminController(UserRepository userRepository,
                           MasterRepository masterRepository,
                           ServiceRepository serviceRepository,
                           ReviewRepository reviewRepository,
                           AppointmentRepository appointmentRepository) {
        this.userRepository = userRepository;
        this.masterRepository = masterRepository;
        this.serviceRepository = serviceRepository;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // **Управление пользователями**
    @GetMapping("/users")
    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // **Управление мастерами**
    @GetMapping("/masters")
    public List<Master> getAllMasters() {
        return masterRepository.findAll();
    }

    @PostMapping("/masters")
    public ResponseEntity<Master> createMaster(@RequestBody Master master) {
        Master savedMaster = masterRepository.save(master);
        return ResponseEntity.ok(savedMaster);
    }

    @PutMapping("/masters/{id}")
    public ResponseEntity<Master> updateMaster(@PathVariable Long id, @RequestBody Master master) {
        if (masterRepository.existsById(id)) {
            master.setId(id);
            Master updatedMaster = masterRepository.save(master);
            return ResponseEntity.ok(updatedMaster);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/masters/{id}")
    public ResponseEntity<Void> deleteMaster(@PathVariable Long id) {
        if (masterRepository.existsById(id)) {
            masterRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // **Управление услугами**
    @GetMapping("/services")
    public List<Services> getAllServices() {
        return serviceRepository.findAll();
    }

    @PostMapping("/services")
    public ResponseEntity<Services> createService(@RequestBody Services service) {
        Services savedService = serviceRepository.save(service);
        return ResponseEntity.ok(savedService);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<Services> updateService(@PathVariable Long id, @RequestBody Services service) {
        if (serviceRepository.existsById(id)) {
            service.setId(id);
            Services updatedService = serviceRepository.save(service);
            return ResponseEntity.ok(updatedService);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        if (serviceRepository.existsById(id)) {
            serviceRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // **Управление отзывами**
    @GetMapping("/reviews")
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // **Управление записями**
    @GetMapping("/appointments")
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        if (appointmentRepository.existsById(id)) {
            appointmentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

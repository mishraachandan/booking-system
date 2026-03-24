package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.entity.Cinema;
import com.mishraachandan.booking_system.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    @GetMapping
    public ResponseEntity<List<Cinema>> getCinemasByCity(@RequestParam Long cityId) {
        return ResponseEntity.ok(cinemaService.getCinemasByCity(cityId));
    }

    @PostMapping
    public ResponseEntity<Cinema> createCinema(@RequestBody Cinema cinema) {
        return ResponseEntity.ok(cinemaService.createCinema(cinema));
    }
}

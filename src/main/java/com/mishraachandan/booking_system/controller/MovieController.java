package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.pojo.MovieResponse;
import com.mishraachandan.booking_system.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(movieService.getMovie(movieId));
    }
}

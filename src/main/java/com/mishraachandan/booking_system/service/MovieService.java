package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Movie;
import com.mishraachandan.booking_system.dto.pojo.MovieResponse;
import com.mishraachandan.booking_system.repository.MovieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(MovieResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieResponse getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movie not found: " + id));
        return MovieResponse.fromEntity(movie);
    }
}

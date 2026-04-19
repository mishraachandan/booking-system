package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Movie;
import com.mishraachandan.booking_system.dto.entity.Screen;
import com.mishraachandan.booking_system.dto.entity.Show;
import com.mishraachandan.booking_system.dto.pojo.CreateShowRequest;
import com.mishraachandan.booking_system.repository.MovieRepository;
import com.mishraachandan.booking_system.repository.ScreenRepository;
import com.mishraachandan.booking_system.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;

    public List<Show> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId);
    }

    public List<Show> getShowsByScreen(Long screenId) {
        return showRepository.findByScreenId(screenId);
    }

    public List<Show> getAllShows() {
        return showRepository.findAll();
    }

    public List<Show> getShowsByCityId(Long cityId) {
        return showRepository.findByCityId(cityId);
    }

    public Show createShow(CreateShowRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + request.getMovieId()));
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + request.getScreenId()));

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        return showRepository.save(show);
    }

    public Show getShowById(Long showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));
    }
}

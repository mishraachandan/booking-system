package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Show;
import com.mishraachandan.booking_system.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;

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

    public Show createShow(Show show) {
        return showRepository.save(show);
    }

    public Show getShowById(Long showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));
    }
}

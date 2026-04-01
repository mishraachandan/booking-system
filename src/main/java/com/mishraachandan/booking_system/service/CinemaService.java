package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Cinema;
import com.mishraachandan.booking_system.repository.CinemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CinemaService {

    private final CinemaRepository cinemaRepository;

    public List<Cinema> getCinemasByCity(Long cityId) {
        return cinemaRepository.findByCityId(cityId);
    }

    public Cinema createCinema(Cinema cinema) {
        return cinemaRepository.save(cinema);
    }
}

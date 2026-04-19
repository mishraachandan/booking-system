package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Cinema;
import com.mishraachandan.booking_system.dto.entity.City;
import com.mishraachandan.booking_system.dto.pojo.CreateCinemaRequest;
import com.mishraachandan.booking_system.repository.CinemaRepository;
import com.mishraachandan.booking_system.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CityRepository cityRepository;

    public List<Cinema> getCinemasByCity(Long cityId) {
        return cinemaRepository.findByCityId(cityId);
    }

    public Cinema createCinema(CreateCinemaRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new IllegalArgumentException("City not found: " + request.getCityId()));

        Cinema cinema = new Cinema();
        cinema.setName(request.getName());
        cinema.setAddress(request.getAddress());
        cinema.setCity(city);
        return cinemaRepository.save(cinema);
    }
}

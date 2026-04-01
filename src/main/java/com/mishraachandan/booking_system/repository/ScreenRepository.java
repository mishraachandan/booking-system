package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByCinemaId(Long cinemaId);
}

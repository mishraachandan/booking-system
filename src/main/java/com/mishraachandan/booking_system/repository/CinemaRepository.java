package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    List<Cinema> findByCityId(Long cityId);
}

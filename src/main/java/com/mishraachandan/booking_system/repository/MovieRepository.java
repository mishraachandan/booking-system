package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
}

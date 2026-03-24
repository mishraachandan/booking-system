package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
}

package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.AddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, Long> {

    List<AddOn> findByAvailableTrueOrderByCategoryAscPriceAsc();
}

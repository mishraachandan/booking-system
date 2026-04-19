package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieId(Long movieId);
    List<Show> findByScreenId(Long screenId);

    @Query("SELECT s FROM Show s WHERE s.screen.cinema.city.id = :cityId")
    List<Show> findByCityId(@Param("cityId") Long cityId);
}

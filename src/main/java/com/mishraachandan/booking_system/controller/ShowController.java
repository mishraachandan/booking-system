package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.config.AuthenticatedUser;
import com.mishraachandan.booking_system.dto.entity.Show;
import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.pojo.CreateShowRequest;
import com.mishraachandan.booking_system.dto.pojo.LockSeatsRequest;
import com.mishraachandan.booking_system.dto.pojo.ShowSeatResponse;
import com.mishraachandan.booking_system.service.ShowSeatLockService;
import com.mishraachandan.booking_system.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;
    private final ShowSeatLockService showSeatLockService;

    /**
     * Get all shows, optionally filtered by city.
     */
    @GetMapping
    public ResponseEntity<List<Show>> getAllShows(@RequestParam(required = false) Long cityId) {
        if (cityId != null && cityId > 0) {
            return ResponseEntity.ok(showService.getShowsByCityId(cityId));
        }
        return ResponseEntity.ok(showService.getAllShows());
    }

    /**
     * Get shows by movie ID.
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<Show>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId));
    }

    /**
     * Get all seats for a show as a flat DTO (no lazy loading, no circular refs).
     */
    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponse>> getShowSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(showSeatLockService.getAllShowSeatResponses(showId));
    }

    /**
     * Get only available seats for a show.
     */
    @GetMapping("/{showId}/seats/available")
    public ResponseEntity<List<ShowSeat>> getAvailableShowSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(showSeatLockService.getAvailableShowSeats(showId));
    }

    /**
     * Lock seats for a user (temporary hold).
     * userId is extracted from the JWT via @AuthenticationPrincipal.
     */
    @PostMapping("/{showId}/seats/lock")
    public ResponseEntity<Map<String, Object>> lockSeats(
            @PathVariable Long showId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody LockSeatsRequest request) {

        boolean success = showSeatLockService.lockShowSeats(request.getShowSeatIds(), principal.getUserId());

        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message",
                    "Seats locked successfully. You have 8 minutes to complete booking."));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "One or more seats are not available"));
        }
    }

    /**
     * Unlock a specific seat (user deselects).
     */
    @PostMapping("/seats/{showSeatId}/unlock")
    public ResponseEntity<Void> unlockSeat(@PathVariable Long showSeatId) {
        showSeatLockService.unlockShowSeat(showSeatId);
        return ResponseEntity.ok().build();
    }

    /**
     * Create a new show. Requires ADMIN role (enforced by SecurityConfig).
     */
    @PostMapping
    public ResponseEntity<Show> createShow(@Valid @RequestBody CreateShowRequest request) {
        return ResponseEntity.ok(showService.createShow(request));
    }
}

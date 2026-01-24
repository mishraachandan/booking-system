package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.entity.Seat;
import com.mishraachandan.booking_system.service.SeatLockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatLockService seatLockService;

    public SeatController(SeatLockService seatLockService) {
        this.seatLockService = seatLockService;
    }

    /**
     * Get available seats for a resource.
     */
    @GetMapping("/available/{resourceId}")
    public ResponseEntity<List<Seat>> getAvailableSeats(@PathVariable Long resourceId) {
        List<Seat> seats = seatLockService.getAvailableSeats(resourceId);
        return ResponseEntity.ok(seats);
    }

    /**
     * Lock a seat for a user.
     */
    @PostMapping("/lock")
    public ResponseEntity<Map<String, Object>> lockSeat(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {

        Long resourceId = Long.valueOf(request.get("resourceId").toString());
        String seatNumber = request.get("seatNumber").toString();

        boolean success = seatLockService.lockSeat(resourceId, seatNumber, userId);

        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Seat locked successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seat is not available"));
        }
    }

    /**
     * Unlock a seat (user cancels selection).
     */
    @PostMapping("/unlock/{seatId}")
    public ResponseEntity<Void> unlockSeat(@PathVariable Long seatId) {
        seatLockService.unlockSeat(seatId);
        return ResponseEntity.ok().build();
    }
}

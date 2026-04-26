package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.pojo.AnalyticsOverview;
import com.mishraachandan.booking_system.dto.pojo.OccupancyEntry;
import com.mishraachandan.booking_system.dto.pojo.RevenuePoint;
import com.mishraachandan.booking_system.dto.pojo.TopShowEntry;
import com.mishraachandan.booking_system.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Admin-only analytics API. All endpoints are read-only aggregate queries over
 * the existing bookings / show_seats / booking_addons tables — no data mutation.
 *
 * Route protection is handled in {@link com.mishraachandan.booking_system.config.SecurityConfig}
 * via the {@code /api/v1/admin/**} matcher.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Returns a headline KPI set. Defaults to "last 30 days" if no range given.
     */
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverview> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDateTime[] win = window(from, to);
        return ResponseEntity.ok(analyticsService.overview(win[0], win[1]));
    }

    /**
     * Gross revenue grouped by day (createdAt).
     */
    @GetMapping("/revenue/daily")
    public ResponseEntity<List<RevenuePoint>> revenueDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate t = to != null ? to : LocalDate.now();
        return ResponseEntity.ok(analyticsService.revenueByDay(f, t));
    }

    /**
     * Gross revenue grouped by cinema, descending.
     */
    @GetMapping("/revenue/by-cinema")
    public ResponseEntity<List<RevenuePoint>> revenueByCinema(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDateTime[] win = window(from, to);
        return ResponseEntity.ok(analyticsService.revenueByCinema(win[0], win[1]));
    }

    /**
     * Gross revenue grouped by movie, descending.
     */
    @GetMapping("/revenue/by-movie")
    public ResponseEntity<List<RevenuePoint>> revenueByMovie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDateTime[] win = window(from, to);
        return ResponseEntity.ok(analyticsService.revenueByMovie(win[0], win[1]));
    }

    /**
     * Top-N performing shows by booked-seat revenue.
     */
    @GetMapping("/top-shows")
    public ResponseEntity<List<TopShowEntry>> topShows(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        LocalDateTime f = from != null ? from.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime t = to != null ? to.atTime(LocalTime.MAX) : LocalDate.now().plusDays(30).atTime(LocalTime.MAX);
        int lim = limit == null ? 10 : limit;
        return ResponseEntity.ok(analyticsService.topShows(f, t, lim));
    }

    /**
     * Per-show seat occupancy. Defaults to shows whose startTime falls in the
     * window [today-7d, today+30d] so the dashboard shows recent + upcoming.
     */
    @GetMapping("/occupancy")
    public ResponseEntity<List<OccupancyEntry>> occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDateTime f = from != null ? from.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime t = to != null ? to.atTime(LocalTime.MAX) : LocalDate.now().plusDays(30).atTime(LocalTime.MAX);
        return ResponseEntity.ok(analyticsService.occupancy(f, t));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private LocalDateTime[] window(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate t = to != null ? to : LocalDate.now();
        return new LocalDateTime[] { f.atStartOfDay(), t.atTime(LocalTime.MAX) };
    }
}

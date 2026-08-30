package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.pojo.AnalyticsOverview;
import com.mishraachandan.booking_system.dto.pojo.OccupancyEntry;
import com.mishraachandan.booking_system.dto.pojo.RevenuePoint;
import com.mishraachandan.booking_system.dto.pojo.TopShowEntry;
import com.mishraachandan.booking_system.service.AnalyticsService;
import com.mishraachandan.booking_system.service.AuditLogService;
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
    private final com.mishraachandan.booking_system.repository.BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

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

    @GetMapping("/audit-logs")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<com.mishraachandan.booking_system.dto.entity.AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(org.springframework.data.domain.PageRequest.of(page, size)));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private LocalDateTime[] window(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate t = to != null ? to : LocalDate.now();
        return new LocalDateTime[] { f.atStartOfDay(), t.atTime(LocalTime.MAX) };
    }

    @GetMapping("/export/bookings")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> exportBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "csv") String format) {
            
        LocalDateTime[] win = window(from, to);
        List<com.mishraachandan.booking_system.dto.entity.Booking> bookings = bookingRepository.findByStartTimeBetween(win[0], win[1]);

        org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody stream = out -> {
            try (java.io.Writer writer = new java.io.OutputStreamWriter(out);
                 com.opencsv.CSVWriter csvWriter = new com.opencsv.CSVWriter(writer)) {
                 
                String[] header = {"ID", "User Email", "Movie", "Show Time", "Seats", "Add-ons Total", "Total Amount", "Status", "Created At"};
                csvWriter.writeNext(header);
                
                for (com.mishraachandan.booking_system.dto.entity.Booking b : bookings) {
                    String userEmail = b.getUser() != null ? b.getUser().getEmail() : "";
                    String movie = b.getShow() != null && b.getShow().getMovie() != null ? b.getShow().getMovie().getTitle() : "";
                    String showTime = b.getShow() != null && b.getShow().getStartTime() != null ? b.getShow().getStartTime().toString() : "";
                    String seats = b.getNumberOfTickets() != null ? b.getNumberOfTickets().toString() : "0";
                    String addonsTotal = "0"; // Simplify as per prompt or skip if hard to fetch
                    String totalAmount = bookingRepository.findTotalAmountForBooking(b.getId()).map(Object::toString).orElse("0");
                    String status = b.getStatus() != null ? b.getStatus().toString() : "";
                    String createdAt = b.getCreatedAt() != null ? b.getCreatedAt().toString() : "";
                    
                    csvWriter.writeNext(new String[]{
                        b.getId().toString(), userEmail, movie, showTime, seats, addonsTotal, totalAmount, status, createdAt
                    });
                }
            }
        };

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookings-export.csv\"")
                .body(stream);
    }
}

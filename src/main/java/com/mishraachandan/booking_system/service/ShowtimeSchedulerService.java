package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.*;
import com.mishraachandan.booking_system.dto.status.SeatType;
import com.mishraachandan.booking_system.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeSchedulerService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    @PostConstruct
    public void init() {
        try {
            ensureRollingShowtimes();
        } catch (Exception e) {
            log.error("Failed to initialize rolling showtimes on startup: {}", e.getMessage());
        }
    }

    /**
     * Daily cron job at 1:00 AM to generate future showtimes for the rolling 4-day window.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledShowtimeRefresh() {
        log.info("Running daily scheduled showtime generator...");
        ensureRollingShowtimes();
    }

    @Transactional
    public void ensureRollingShowtimes() {
        List<Movie> movies = movieRepository.findAll();
        List<Screen> screens = screenRepository.findAll();

        if (movies.isEmpty() || screens.isEmpty()) {
            log.info("No movies or screens found in database. Skipping showtime generation.");
            return;
        }

        int[] showHours = {10, 14, 19};
        int movieIdx = 0;
        int createdShowsCount = 0;

        // Ensure physical seats exist for each screen
        for (Screen screen : screens) {
            ensurePhysicalSeatsForScreen(screen);
        }

        // Schedule for today (day 0) + next 3 days (total 4 days)
        for (int dayOffset = 0; dayOffset <= 3; dayOffset++) {
            LocalDate targetDate = LocalDate.now().plusDays(dayOffset);

            for (Screen screen : screens) {
                List<Seat> physicalSeats = seatRepository.findByScreenId(screen.getId());
                if (physicalSeats.isEmpty()) continue;

                for (int hour : showHours) {
                    LocalDateTime startTime = LocalDateTime.of(targetDate, LocalTime.of(hour, 0));
                    LocalDateTime endTime = startTime.plusMinutes(150);

                    // Skip if start time is in the past for today
                    if (startTime.isBefore(LocalDateTime.now())) {
                        continue;
                    }

                    // Check if a show already exists on this screen at this approximate time
                    boolean exists = showRepository.findByScreenId(screen.getId()).stream()
                            .anyMatch(s -> Math.abs(java.time.Duration.between(s.getStartTime(), startTime).toMinutes()) < 60);

                    if (!exists) {
                        Movie movie = movies.get(movieIdx % movies.size());
                        movieIdx++;

                        Show show = Show.builder()
                                .movie(movie)
                                .screen(screen)
                                .startTime(startTime)
                                .endTime(endTime)
                                .build();
                        show = showRepository.save(show);

                        List<ShowSeat> showSeats = new ArrayList<>();
                        for (Seat seat : physicalSeats) {
                            BigDecimal price = getPriceForSeatType(seat.getSeatType());
                            showSeats.add(ShowSeat.builder()
                                    .show(show)
                                    .seat(seat)
                                    .price(price)
                                    .status(SeatStatus.AVAILABLE)
                                    .build());
                        }
                        showSeatRepository.saveAll(showSeats);
                        createdShowsCount++;
                    }
                }
            }
        }

        if (createdShowsCount > 0) {
            log.info("Successfully generated {} rolling future shows with seating inventory.", createdShowsCount);
        }
    }

    private void ensurePhysicalSeatsForScreen(Screen screen) {
        List<Seat> existing = seatRepository.findByScreenId(screen.getId());
        if (!existing.isEmpty()) return;

        List<Seat> newSeats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D"};

        for (String row : rows) {
            for (int num = 1; num <= 5; num++) {
                SeatType type = switch (row) {
                    case "A" -> SeatType.VIP;
                    case "B" -> SeatType.PREMIUM;
                    default -> SeatType.REGULAR;
                };

                newSeats.add(Seat.builder()
                        .screen(screen)
                        .seatNumber(row + num)
                        .seatType(type)
                        .build());
            }
        }
        seatRepository.saveAll(newSeats);
    }

    private BigDecimal getPriceForSeatType(SeatType seatType) {
        if (seatType == null) return new BigDecimal("200.00");
        return switch (seatType) {
            case VIP -> new BigDecimal("500.00");
            case PREMIUM -> new BigDecimal("350.00");
            default -> new BigDecimal("200.00");
        };
    }
}

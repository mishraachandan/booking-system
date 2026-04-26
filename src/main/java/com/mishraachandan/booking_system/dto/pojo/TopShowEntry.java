package com.mishraachandan.booking_system.dto.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the top-performing-shows leaderboard.
 */
public class TopShowEntry {
    private Long showId;
    private String movieTitle;
    private String cinemaName;
    private LocalDateTime startTime;
    private long totalSeats;
    private long bookedSeats;
    private BigDecimal revenue;
    private double occupancyPercent;

    public TopShowEntry() {}

    public TopShowEntry(Long showId,
                        String movieTitle,
                        String cinemaName,
                        LocalDateTime startTime,
                        long totalSeats,
                        long bookedSeats,
                        BigDecimal revenue,
                        double occupancyPercent) {
        this.showId = showId;
        this.movieTitle = movieTitle;
        this.cinemaName = cinemaName;
        this.startTime = startTime;
        this.totalSeats = totalSeats;
        this.bookedSeats = bookedSeats;
        this.revenue = revenue;
        this.occupancyPercent = occupancyPercent;
    }

    public Long getShowId() { return showId; }
    public String getMovieTitle() { return movieTitle; }
    public String getCinemaName() { return cinemaName; }
    public LocalDateTime getStartTime() { return startTime; }
    public long getTotalSeats() { return totalSeats; }
    public long getBookedSeats() { return bookedSeats; }
    public BigDecimal getRevenue() { return revenue; }
    public double getOccupancyPercent() { return occupancyPercent; }
}

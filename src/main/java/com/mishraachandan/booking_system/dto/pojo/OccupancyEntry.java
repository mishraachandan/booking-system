package com.mishraachandan.booking_system.dto.pojo;

import java.time.LocalDateTime;

/**
 * Per-show seat occupancy snapshot.
 */
public class OccupancyEntry {
    private Long showId;
    private String movieTitle;
    private String cinemaName;
    private String screenName;
    private LocalDateTime startTime;
    private long totalSeats;
    private long bookedSeats;
    private long lockedSeats;
    private double occupancyPercent;

    public OccupancyEntry() {}

    public OccupancyEntry(Long showId,
                          String movieTitle,
                          String cinemaName,
                          String screenName,
                          LocalDateTime startTime,
                          long totalSeats,
                          long bookedSeats,
                          long lockedSeats,
                          double occupancyPercent) {
        this.showId = showId;
        this.movieTitle = movieTitle;
        this.cinemaName = cinemaName;
        this.screenName = screenName;
        this.startTime = startTime;
        this.totalSeats = totalSeats;
        this.bookedSeats = bookedSeats;
        this.lockedSeats = lockedSeats;
        this.occupancyPercent = occupancyPercent;
    }

    public Long getShowId() { return showId; }
    public String getMovieTitle() { return movieTitle; }
    public String getCinemaName() { return cinemaName; }
    public String getScreenName() { return screenName; }
    public LocalDateTime getStartTime() { return startTime; }
    public long getTotalSeats() { return totalSeats; }
    public long getBookedSeats() { return bookedSeats; }
    public long getLockedSeats() { return lockedSeats; }
    public double getOccupancyPercent() { return occupancyPercent; }
}

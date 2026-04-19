package com.mishraachandan.booking_system.dto.pojo;

import com.mishraachandan.booking_system.dto.status.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Flat DTO for Booking responses — avoids Hibernate proxy / circular reference issues.
 */
public class BookingResponse {

    private Long bookingId;
    private BookingStatus status;
    private Integer numberOfTickets;
    private String notes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;

    // User info
    private Long userId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;

    // Show info (null for generic event bookings)
    private Long showId;
    private String movieTitle;
    private String movieGenre;
    private Integer movieDurationMinutes;
    private String screenName;
    private String cinemaName;
    private String cityName;
    private LocalDateTime showStartTime;

    // Resource info (null for show-based bookings)
    private Long resourceId;
    private String resourceName;

    // Seat / add-on totals + attached add-ons
    private BigDecimal seatTotal;
    private BigDecimal addOnTotal;
    private BigDecimal grandTotal;
    private List<BookingAddOnResponse> addOns = Collections.emptyList();

    public BookingResponse() {}

    // Constructor for show-based bookings
    public BookingResponse(Long bookingId, BookingStatus status, Integer numberOfTickets, String notes,
                           LocalDateTime startTime, LocalDateTime endTime, LocalDateTime createdAt,
                           Long userId, String userFirstName, String userLastName, String userEmail,
                           Long showId, String movieTitle, String movieGenre, Integer movieDurationMinutes,
                           String screenName, String cinemaName, String cityName, LocalDateTime showStartTime) {
        this.bookingId = bookingId;
        this.status = status;
        this.numberOfTickets = numberOfTickets;
        this.notes = notes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.userId = userId;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
        this.userEmail = userEmail;
        this.showId = showId;
        this.movieTitle = movieTitle;
        this.movieGenre = movieGenre;
        this.movieDurationMinutes = movieDurationMinutes;
        this.screenName = screenName;
        this.cinemaName = cinemaName;
        this.cityName = cityName;
        this.showStartTime = showStartTime;
    }

    // Static factory from Booking entity (for newly created bookings)
    public static BookingResponse fromBooking(
            com.mishraachandan.booking_system.dto.entity.Booking booking) {
        BookingResponse r = new BookingResponse();
        r.bookingId = booking.getId();
        r.status = booking.getStatus();
        r.numberOfTickets = booking.getNumberOfTickets();
        r.notes = booking.getNotes();
        r.startTime = booking.getStartTime();
        r.endTime = booking.getEndTime();
        r.createdAt = booking.getCreatedAt();

        if (booking.getUser() != null) {
            r.userId = booking.getUser().getId();
            r.userFirstName = booking.getUser().getFirstName();
            r.userLastName = booking.getUser().getLastName();
            r.userEmail = booking.getUser().getEmail();
        }

        if (booking.getShow() != null) {
            r.showId = booking.getShow().getId();
            r.showStartTime = booking.getShow().getStartTime();
            if (booking.getShow().getMovie() != null) {
                r.movieTitle = booking.getShow().getMovie().getTitle();
                r.movieGenre = booking.getShow().getMovie().getGenre();
                r.movieDurationMinutes = booking.getShow().getMovie().getDurationMinutes();
            }
            if (booking.getShow().getScreen() != null) {
                r.screenName = booking.getShow().getScreen().getName();
                if (booking.getShow().getScreen().getCinema() != null) {
                    r.cinemaName = booking.getShow().getScreen().getCinema().getName();
                    if (booking.getShow().getScreen().getCinema().getCity() != null) {
                        r.cityName = booking.getShow().getScreen().getCinema().getCity().getName();
                    }
                }
            }
        }

        if (booking.getResource() != null) {
            r.resourceId = booking.getResource().getId();
            r.resourceName = booking.getResource().getName();
        }

        return r;
    }

    // Getters
    public Long getBookingId() { return bookingId; }
    public BookingStatus getStatus() { return status; }
    public Integer getNumberOfTickets() { return numberOfTickets; }
    public String getNotes() { return notes; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUserId() { return userId; }
    public String getUserFirstName() { return userFirstName; }
    public String getUserLastName() { return userLastName; }
    public String getUserEmail() { return userEmail; }
    public Long getShowId() { return showId; }
    public String getMovieTitle() { return movieTitle; }
    public String getMovieGenre() { return movieGenre; }
    public Integer getMovieDurationMinutes() { return movieDurationMinutes; }
    public String getScreenName() { return screenName; }
    public String getCinemaName() { return cinemaName; }
    public String getCityName() { return cityName; }
    public LocalDateTime getShowStartTime() { return showStartTime; }
    public Long getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }

    public BigDecimal getSeatTotal() { return seatTotal; }
    public void setSeatTotal(BigDecimal seatTotal) { this.seatTotal = seatTotal; }
    public BigDecimal getAddOnTotal() { return addOnTotal; }
    public void setAddOnTotal(BigDecimal addOnTotal) { this.addOnTotal = addOnTotal; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }
    public List<BookingAddOnResponse> getAddOns() { return addOns; }
    public void setAddOns(List<BookingAddOnResponse> addOns) {
        this.addOns = addOns != null ? addOns : Collections.emptyList();
    }
}

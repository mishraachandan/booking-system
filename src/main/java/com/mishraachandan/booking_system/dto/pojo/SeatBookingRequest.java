package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO for booking specific seats.
 * Accepts a list of seat numbers (e.g., ["A1", "A2", "B3"]).
 * Maximum 10 seats can be booked in a single request.
 */
public class SeatBookingRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotEmpty(message = "At least one seat number is required")
    @Size(min = 1, max = 10, message = "You can book between 1 and 10 seats at a time")
    private List<String> seatNumbers;

    private String notes;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

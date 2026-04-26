package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO for booking specific show seats.
 * Accepts a showId and a list of showSeat IDs.
 * Maximum 10 seats per request.
 */
@Data
public class ShowSeatBookingRequest {

    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(min = 1, max = 10, message = "You can book between 1 and 10 seats at a time")
    private List<Long> showSeatIds;

    private String notes;

    /**
     * Optional list of food/beverage/combo add-ons to attach to this booking.
     * Sizes and per-line quantities are validated by {@link BookingAddOnLine}
     * and the {@code @Size} cap below.
     */
    @Valid
    @Size(max = 20, message = "You can attach at most 20 add-on line items")
    private List<BookingAddOnLine> addOns;
}

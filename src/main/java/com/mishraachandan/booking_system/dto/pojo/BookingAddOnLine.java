package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * A single add-on line item in a booking request: which add-on and how many.
 */
@Data
public class BookingAddOnLine {

    @NotNull(message = "addOnId is required")
    private Long addOnId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 20, message = "quantity must be at most 20")
    private Integer quantity;
}

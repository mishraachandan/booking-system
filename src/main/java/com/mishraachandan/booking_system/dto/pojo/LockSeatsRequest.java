package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for locking one or more ShowSeats. Enforces an upper bound on the
 * list size to prevent a caller from locking a large number of seats in
 * a single request (DoS vector).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockSeatsRequest {

    @NotNull
    @NotEmpty
    @Size(max = 20, message = "You can lock at most 20 seats in a single request")
    private List<@NotNull Long> showSeatIds;
}

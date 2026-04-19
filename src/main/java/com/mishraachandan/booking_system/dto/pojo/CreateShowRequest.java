package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating a Show. {@code movieId} and {@code screenId} are
 * resolved server-side — clients cannot attach arbitrary nested
 * {@link com.mishraachandan.booking_system.dto.entity.Movie} or
 * {@link com.mishraachandan.booking_system.dto.entity.Screen} state.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateShowRequest {

    @NotNull
    private Long movieId;

    @NotNull
    private Long screenId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;
}

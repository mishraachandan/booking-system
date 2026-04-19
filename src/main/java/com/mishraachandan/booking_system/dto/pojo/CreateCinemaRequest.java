package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a Cinema. Only the fields we want clients to set are
 * exposed — the {@code cityId} is resolved server-side to a {@link
 * com.mishraachandan.booking_system.dto.entity.City} entity, preventing
 * clients from attaching arbitrary nested state.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCinemaRequest {

    @NotBlank
    @Size(min = 1, max = 150)
    private String name;

    @Size(max = 500)
    private String address;

    @NotNull
    private Long cityId;
}

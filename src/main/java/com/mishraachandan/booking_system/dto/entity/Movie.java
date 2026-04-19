package com.mishraachandan.booking_system.dto.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String language;
    private String genre;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "poster_url", length = 512)
    private String posterUrl;

    /** YouTube (or other) trailer URL, surfaced on the movie detail page. */
    @Column(name = "trailer_url", length = 512)
    private String trailerUrl;

    /**
     * Comma-separated list of cast members. A denormalised string is
     * deliberate: keeps the schema simple and the detail UI treats it as
     * "Actor A, Actor B, Actor C".
     */
    @Column(name = "cast_members", columnDefinition = "TEXT")
    private String castMembers;

    /** Aggregate rating on a 0.0-10.0 scale. Nullable for new releases. */
    @Column(precision = 3, scale = 1)
    private java.math.BigDecimal rating;
}

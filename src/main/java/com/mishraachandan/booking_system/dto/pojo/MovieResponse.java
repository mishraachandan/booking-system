package com.mishraachandan.booking_system.dto.pojo;

import com.mishraachandan.booking_system.dto.entity.Movie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Flat DTO used by the movie detail / movie list endpoints.
 * Splits the denormalised {@code castMembers} string into a list for the UI.
 */
public class MovieResponse {

    private Long id;
    private String title;
    private String description;
    private String language;
    private String genre;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private List<String> cast;
    private BigDecimal rating;

    public MovieResponse() {}

    public static MovieResponse fromEntity(Movie m) {
        MovieResponse r = new MovieResponse();
        r.id = m.getId();
        r.title = m.getTitle();
        r.description = m.getDescription();
        r.language = m.getLanguage();
        r.genre = m.getGenre();
        r.durationMinutes = m.getDurationMinutes();
        r.releaseDate = m.getReleaseDate();
        r.posterUrl = m.getPosterUrl();
        r.trailerUrl = m.getTrailerUrl();
        r.rating = m.getRating();
        r.cast = parseCast(m.getCastMembers());
        return r;
    }

    private static List<String> parseCast(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLanguage() { return language; }
    public String getGenre() { return genre; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public String getPosterUrl() { return posterUrl; }
    public String getTrailerUrl() { return trailerUrl; }
    public List<String> getCast() { return cast; }
    public BigDecimal getRating() { return rating; }
}

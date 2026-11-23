package com.cinebook.dto;

import com.cinebook.entity.Movie;

import java.time.LocalDate;

public record MovieResponse(Long id, String title, String description, String genre, String language,
                            int durationMinutes, String certificate, Double rating, String posterUrl,
                            LocalDate releaseDate, boolean active) {
    public static MovieResponse from(Movie m) {
        return new MovieResponse(m.getId(), m.getTitle(), m.getDescription(), m.getGenre(), m.getLanguage(),
                m.getDurationMinutes(), m.getCertificate(), m.getRating(), m.getPosterUrl(), m.getReleaseDate(), m.isActive());
    }
}

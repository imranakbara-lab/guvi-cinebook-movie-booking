package com.cinebook.dto;

import com.cinebook.entity.Show;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowResponse(Long id, Long movieId, String movieTitle, String posterUrl, String genre, String language,
                           String certificate, int durationMinutes, Long theaterId, String theaterName, String city,
                           String address, LocalDateTime startTime, LocalDateTime endTime, BigDecimal price,
                           BigDecimal premiumPrice, int capacity, long availableSeats) {
    public static ShowResponse from(Show s, long bookedSeats) {
        var m = s.getMovie();
        var t = s.getTheater();
        return new ShowResponse(s.getId(), m.getId(), m.getTitle(), m.getPosterUrl(), m.getGenre(), m.getLanguage(),
                m.getCertificate(), m.getDurationMinutes(), t.getId(), t.getName(), t.getCity(), t.getAddress(),
                s.getStartTime(), s.getEndTime(), s.getPrice(), s.getPremiumPrice(), t.capacity(),
                Math.max(0, t.capacity() - bookedSeats));
    }
}

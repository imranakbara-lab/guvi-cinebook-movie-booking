package com.cinebook.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One screening of a movie at a theater. */
@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumPrice;

    public LocalDateTime getEndTime() { return startTime.plusMinutes(movie.getDurationMinutes()); }

    public BigDecimal priceFor(String seat) {
        return theater.isPremium(seat) ? premiumPrice : price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public Theater getTheater() { return theater; }
    public void setTheater(Theater theater) { this.theater = theater; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getPremiumPrice() { return premiumPrice; }
    public void setPremiumPrice(BigDecimal premiumPrice) { this.premiumPrice = premiumPrice; }
}

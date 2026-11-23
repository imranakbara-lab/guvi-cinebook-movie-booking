package com.cinebook.entity;

import jakarta.persistence.*;

/**
 * A seat that is taken (held or confirmed) for a show.
 * The unique constraint on (show, seat) is what makes double booking impossible,
 * even when two people click "Book" at the same moment.
 */
@Entity
@Table(name = "booked_seats", uniqueConstraints = @UniqueConstraint(name = "uk_show_seat", columnNames = {"show_id", "seat_label"}))
public class BookedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "seat_label", nullable = false, length = 10)
    private String seatLabel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    public BookedSeat() {}

    public BookedSeat(Long showId, String seatLabel, Booking booking) {
        this.showId = showId;
        this.seatLabel = seatLabel;
        this.booking = booking;
    }

    public Long getId() { return id; }
    public Long getShowId() { return showId; }
    public String getSeatLabel() { return seatLabel; }
    public Booking getBooking() { return booking; }
}

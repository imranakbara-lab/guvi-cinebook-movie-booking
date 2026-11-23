package com.cinebook;

import com.cinebook.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestData {

    private TestData() {}

    public static User user(long id, String email, Role role) {
        User u = new User("User " + id, email, "ENC", "9876543210", role);
        u.setId(id);
        return u;
    }

    /** 3 rows (A, B, C) x 4 seats; row C is premium. */
    public static Theater theater() {
        Theater t = new Theater();
        t.setId(1L);
        t.setName("Screen 1");
        t.setCity("Erode");
        t.setTotalRows(3);
        t.setSeatsPerRow(4);
        t.setPremiumRows(1);
        return t;
    }

    public static Movie movie() {
        Movie m = new Movie();
        m.setId(2L);
        m.setTitle("Velocity");
        m.setGenre("Action");
        m.setLanguage("Tamil");
        m.setDurationMinutes(120);
        m.setActive(true);
        m.setReleaseDate(LocalDate.of(2026, 1, 1));
        return m;
    }

    /** Regular seats cost 100, premium 150. */
    public static Show show(LocalDateTime start) {
        Show s = new Show();
        s.setId(10L);
        s.setMovie(movie());
        s.setTheater(theater());
        s.setStartTime(start);
        s.setPrice(new BigDecimal("100"));
        s.setPremiumPrice(new BigDecimal("150"));
        return s;
    }

    public static Booking booking(User user, Show show, BookingStatus status, LocalDateTime createdAt) {
        Booking b = new Booking();
        b.setId(100L);
        b.setReference("CBTEST01");
        b.setUser(user);
        b.setShow(show);
        b.setSeatLabels("A1,A2");
        b.setAmount(new BigDecimal("200"));
        b.setStatus(status);
        b.setCreatedAt(createdAt);
        b.setHoldExpiresAt(createdAt.plusMinutes(10));
        b.getLockedSeats().add(new BookedSeat(show.getId(), "A1", b));
        b.getLockedSeats().add(new BookedSeat(show.getId(), "A2", b));
        if (status == BookingStatus.CONFIRMED) {
            b.setPaymentStatus(PaymentStatus.PAID);
            b.setConfirmedAt(createdAt.plusMinutes(2));
        }
        return b;
    }
}

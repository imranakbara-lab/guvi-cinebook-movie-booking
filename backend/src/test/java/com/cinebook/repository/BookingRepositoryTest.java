package com.cinebook.repository;

import com.cinebook.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Repository tests on H2 - including the database rule that prevents double booking. */
@DataJpaTest
class BookingRepositoryTest {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookedSeatRepository bookedSeatRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private TheaterRepository theaterRepository;
    @Autowired private UserRepository userRepository;

    private Show show(boolean activeMovie, LocalDateTime start) {
        Movie m = new Movie();
        m.setTitle("Film");
        m.setGenre("Drama");
        m.setDurationMinutes(120);
        m.setActive(activeMovie);
        movieRepository.save(m);
        Theater t = new Theater();
        t.setName("Screen");
        t.setCity("Erode");
        t.setTotalRows(5);
        t.setSeatsPerRow(5);
        theaterRepository.save(t);
        Show s = new Show();
        s.setMovie(m);
        s.setTheater(t);
        s.setStartTime(start);
        s.setPrice(BigDecimal.TEN);
        s.setPremiumPrice(BigDecimal.TEN);
        return showRepository.save(s);
    }

    private Booking booking(User u, Show s, String ref, String... seats) {
        Booking b = new Booking();
        b.setReference(ref);
        b.setUser(u);
        b.setShow(s);
        b.setSeatLabels(String.join(",", seats));
        b.setAmount(BigDecimal.TEN);
        b.setCreatedAt(LocalDateTime.now());
        b.setHoldExpiresAt(LocalDateTime.now().plusMinutes(10));
        for (String seat : seats) b.getLockedSeats().add(new BookedSeat(s.getId(), seat, b));
        return b;
    }

    @Test
    void sameSeatTwice_isRejectedByDatabase() {
        Show s = show(true, LocalDateTime.now().plusDays(1));
        User u = userRepository.save(new User("A", "a@mail.com", "x", null, Role.USER));
        bookingRepository.saveAndFlush(booking(u, s, "CB000001", "A1", "A2"));

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(booking(u, s, "CB000002", "A2", "A3")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seatQueries_andReleaseByOrphanRemoval() {
        Show s = show(true, LocalDateTime.now().plusDays(1));
        User u = userRepository.save(new User("A", "a@mail.com", "x", null, Role.USER));
        Booking b = bookingRepository.saveAndFlush(booking(u, s, "CB000003", "B1", "B2"));

        assertThat(bookedSeatRepository.findSeatLabelsByShowId(s.getId())).containsExactlyInAnyOrder("B1", "B2");
        assertThat(bookedSeatRepository.findTaken(s.getId(), List.of("B2", "B3"))).containsExactly("B2");

        b.getLockedSeats().clear();
        b.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(b);
        assertThat(bookedSeatRepository.countByShowId(s.getId())).isZero();
    }

    @Test
    void findActiveBetween_skipsInactiveMoviesAndOtherDays() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(10);
        show(true, tomorrow);
        show(false, tomorrow.plusHours(1));
        show(true, tomorrow.plusDays(3));

        assertThat(showRepository.findActiveBetween(tomorrow.withHour(0), tomorrow.withHour(23))).hasSize(1);
    }

    @Test
    void expiredHoldQuery() {
        Show s = show(true, LocalDateTime.now().plusDays(1));
        User u = userRepository.save(new User("A", "a@mail.com", "x", null, Role.USER));
        Booking old = booking(u, s, "CB000004", "C1");
        old.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        bookingRepository.saveAndFlush(old);
        bookingRepository.saveAndFlush(booking(u, s, "CB000005", "C2"));

        assertThat(bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING_PAYMENT, LocalDateTime.now()))
                .extracting(Booking::getReference).containsExactly("CB000004");
    }
}

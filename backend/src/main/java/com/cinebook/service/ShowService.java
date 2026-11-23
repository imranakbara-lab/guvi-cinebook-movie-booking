package com.cinebook.service;

import com.cinebook.dto.SeatMapResponse;
import com.cinebook.dto.ShowRequest;
import com.cinebook.dto.ShowResponse;
import com.cinebook.entity.*;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookedSeatRepository;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Show module: showtimes per theater, seat availability and admin scheduling. */
@Service
public class ShowService {

    /** Gap needed between two shows on the same screen (cleaning / ads). */
    static final int CLEANING_MINUTES = 15;

    private final ShowRepository showRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final BookingRepository bookingRepository;
    private final MovieService movieService;
    private final TheaterService theaterService;
    private final Clock clock;

    public ShowService(ShowRepository showRepository, BookedSeatRepository bookedSeatRepository,
                       BookingRepository bookingRepository, MovieService movieService,
                       TheaterService theaterService, Clock clock) {
        this.showRepository = showRepository;
        this.bookedSeatRepository = bookedSeatRepository;
        this.bookingRepository = bookingRepository;
        this.movieService = movieService;
        this.theaterService = theaterService;
        this.clock = clock;
    }

    /** Upcoming shows of a movie, optionally on one date and/or in one city. */
    @Transactional(readOnly = true)
    public List<ShowResponse> showsForMovie(Long movieId, LocalDate date, String city) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime from = date == null || date.atStartOfDay().isBefore(now) ? now : date.atStartOfDay();
        LocalDateTime to = date == null ? now.plusDays(30) : date.plusDays(1).atStartOfDay();
        return showRepository.findActiveBetween(from, to).stream()
                .filter(s -> s.getMovie().getId().equals(movieId))
                .filter(s -> city == null || city.isBlank() || s.getTheater().getCity().equalsIgnoreCase(city.trim()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShowResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public SeatMapResponse seatMap(Long id) {
        Show s = find(id);
        Theater t = s.getTheater();
        List<String> rows = new ArrayList<>();
        for (int r = 0; r < t.getTotalRows(); r++) rows.add(Theater.rowLabel(r));
        List<String> premium = rows.subList(Math.max(0, rows.size() - t.getPremiumRows()), rows.size());
        List<String> taken = bookedSeatRepository.findSeatLabelsByShowId(id);
        return new SeatMapResponse(ShowResponse.from(s, taken.size()), rows, t.getSeatsPerRow(), List.copyOf(premium), taken);
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public List<ShowResponse> all(boolean upcomingOnly) {
        List<Show> shows = upcomingOnly
                ? showRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime.now(clock).minusHours(3))
                : showRepository.findAllByOrderByStartTimeDesc();
        return shows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShowResponse create(ShowRequest req) {
        Show s = new Show();
        apply(s, req, null);
        return toResponse(showRepository.save(s));
    }

    @Transactional
    public ShowResponse update(Long id, ShowRequest req) {
        Show s = find(id);
        if (hasActiveBookings(id) && (!s.getTheater().getId().equals(req.theaterId()) || !s.getMovie().getId().equals(req.movieId()))) {
            throw new BadRequestException("Movie or theater cannot change after tickets are booked");
        }
        apply(s, req, id);
        return toResponse(showRepository.save(s));
    }

    @Transactional
    public void delete(Long id) {
        Show s = find(id);
        if (hasActiveBookings(id)) throw new BadRequestException("This show has bookings - it cannot be deleted");
        bookingRepository.deleteByShowId(id);
        showRepository.delete(s);
    }

    // ---------- helpers ----------

    Show find(Long id) {
        return showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show not found with id " + id));
    }

    private boolean hasActiveBookings(Long showId) {
        return bookingRepository.existsByShowIdAndStatusIn(showId, EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT));
    }

    private void apply(Show s, ShowRequest r, Long currentId) {
        Movie movie = movieService.find(r.movieId());
        Theater theater = theaterService.find(r.theaterId());
        LocalDateTime end = r.startTime().plusMinutes(movie.getDurationMinutes() + CLEANING_MINUTES);
        // any show starting up to 6h before could still be running
        for (Show other : showRepository.findPossiblyOverlapping(theater.getId(), r.startTime().minusHours(6), end)) {
            if (other.getId().equals(currentId)) continue;
            LocalDateTime otherEnd = other.getEndTime().plusMinutes(CLEANING_MINUTES);
            if (otherEnd.isAfter(r.startTime())) {
                throw new BadRequestException("Screen is busy: '" + other.getMovie().getTitle() + "' runs from "
                        + other.getStartTime().toLocalTime() + " to " + other.getEndTime().toLocalTime());
            }
        }
        s.setMovie(movie);
        s.setTheater(theater);
        s.setStartTime(r.startTime().withSecond(0).withNano(0));
        s.setPrice(r.price());
        s.setPremiumPrice(r.premiumPrice() == null ? r.price() : r.premiumPrice());
    }

    private ShowResponse toResponse(Show s) {
        return ShowResponse.from(s, bookedSeatRepository.countByShowId(s.getId()));
    }
}

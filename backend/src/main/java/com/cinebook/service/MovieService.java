package com.cinebook.service;

import com.cinebook.dto.MovieRequest;
import com.cinebook.dto.MovieResponse;
import com.cinebook.entity.BookingStatus;
import com.cinebook.entity.Movie;
import com.cinebook.entity.Show;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Movie module: browse by date, genre, theater or city; admin CRUD. */
@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public MovieService(MovieRepository movieRepository, ShowRepository showRepository,
                        BookingRepository bookingRepository, Clock clock) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    /**
     * Movies users can book. With a date / theater / city filter only movies that have
     * upcoming shows matching it are returned.
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> browse(LocalDate date, String genre, Long theaterId, String city, String q) {
        List<Movie> movies;
        if (date != null || theaterId != null || hasText(city)) {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime from = date == null ? now : max(date.atStartOfDay(), now);
            LocalDateTime to = date == null ? now.plusDays(30) : date.plusDays(1).atStartOfDay();
            Map<Long, Movie> unique = new LinkedHashMap<>();
            for (Show s : showRepository.findActiveBetween(from, to)) {
                if (theaterId != null && !s.getTheater().getId().equals(theaterId)) continue;
                if (hasText(city) && !s.getTheater().getCity().equalsIgnoreCase(city.trim())) continue;
                unique.putIfAbsent(s.getMovie().getId(), s.getMovie());
            }
            movies = new ArrayList<>(unique.values());
        } else {
            movies = movieRepository.findByActiveTrueOrderByReleaseDateDesc();
        }
        return movies.stream()
                .filter(m -> !hasText(genre) || m.getGenre().equalsIgnoreCase(genre.trim()))
                .filter(m -> !hasText(q) || (m.getTitle() + " " + m.getGenre() + " " + Objects.toString(m.getLanguage(), ""))
                        .toLowerCase().contains(q.trim().toLowerCase()))
                .map(MovieResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> genres() {
        return movieRepository.findGenres();
    }

    @Transactional(readOnly = true)
    public MovieResponse get(Long id) {
        Movie m = find(id);
        if (!m.isActive()) throw new ResourceNotFoundException("Movie not found with id " + id);
        return MovieResponse.from(m);
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public List<MovieResponse> all() {
        return movieRepository.findAllByOrderByTitleAsc().stream().map(MovieResponse::from).toList();
    }

    @Transactional
    public MovieResponse create(MovieRequest req) {
        Movie m = new Movie();
        apply(m, req);
        return MovieResponse.from(movieRepository.save(m));
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest req) {
        Movie m = find(id);
        apply(m, req);
        return MovieResponse.from(movieRepository.save(m));
    }

    /** A movie with bookings cannot be deleted (history must stay) - deactivate it instead. */
    @Transactional
    public void delete(Long id) {
        Movie m = find(id);
        if (bookingRepository.existsByShowMovieIdAndStatusIn(id, EnumSet.allOf(BookingStatus.class))) {
            throw new BadRequestException("This movie has bookings. Mark it inactive instead of deleting it.");
        }
        showRepository.deleteAll(showRepository.findByMovieId(id));
        movieRepository.delete(m);
    }

    Movie find(Long id) {
        return movieRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movie not found with id " + id));
    }

    private void apply(Movie m, MovieRequest r) {
        m.setTitle(r.title().trim());
        m.setDescription(trim(r.description()));
        m.setGenre(r.genre().trim());
        m.setLanguage(trim(r.language()));
        m.setDurationMinutes(r.durationMinutes());
        m.setCertificate(trim(r.certificate()));
        m.setRating(r.rating());
        m.setPosterUrl(trim(r.posterUrl()));
        m.setReleaseDate(r.releaseDate());
        m.setActive(r.active());
    }

    private static String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) { return a.isAfter(b) ? a : b; }
}

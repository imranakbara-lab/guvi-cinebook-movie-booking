package com.cinebook.config;

import com.cinebook.entity.*;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowRepository;
import com.cinebook.repository.TheaterRepository;
import com.cinebook.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/**
 * Creates the first Admin account and, for demos, sample theaters, movies and a week of shows.
 * Shows are topped up on each start so the demo always has upcoming showtimes.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ShowRepository showRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${app.admin.email}") private String adminEmail;
    @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.seed-sample-data:true}") private boolean seedSamples;

    public DataSeeder(UserRepository userRepository, MovieRepository movieRepository, TheaterRepository theaterRepository,
                      ShowRepository showRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.theaterRepository = theaterRepository;
        this.showRepository = showRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.save(new User("Admin", adminEmail, passwordEncoder.encode(adminPassword), null, Role.ADMIN));
            log.info("Default admin created: {}", adminEmail);
        }
        if (!seedSamples) return;
        if (movieRepository.count() == 0) seedCatalog();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!showRepository.existsByStartTimeAfter(now.plusDays(2))) seedShows(now.toLocalDate());
    }

    private void seedCatalog() {
        theaterRepository.saveAll(List.of(
                theater("CineBook Grand - Screen 1", "Erode", "Perundurai Road, Erode", 10, 14, 2),
                theater("CineBook Grand - Screen 2", "Erode", "Perundurai Road, Erode", 8, 12, 2),
                theater("Star Multiplex - Audi 1", "Coimbatore", "Avinashi Road, Coimbatore", 12, 16, 3)));
        movieRepository.saveAll(List.of(
                movie("The Last Monsoon", "Drama", "Tamil", 142, "UA", 8.4,
                        "A retired schoolteacher returns to his drought-hit village and finds hope in an unlikely friendship.",
                        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600"),
                movie("Velocity", "Action", "Tamil", 156, "UA", 7.6,
                        "An undercover cop races against time across Chennai to stop a heist that could bring down the city's banks.",
                        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600"),
                movie("Laugh Out Loud", "Comedy", "English", 118, "U", 7.1,
                        "Three friends start a wedding-planning business and every wedding goes hilariously wrong.",
                        "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=600"),
                movie("Echoes in the Dark", "Thriller", "Telugu", 131, "A", 7.9,
                        "A sound engineer hears a voice in an old recording that leads her to a decades-old disappearance.",
                        "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=600"),
                movie("Star Voyagers", "Animation", "English", 104, "U", 8.0,
                        "Two kids build a rocket from scrap and accidentally launch themselves into a galaxy-wide adventure.",
                        "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600")));
        log.info("Sample theaters and movies created");
    }

    /** Four shows a day on every screen for the next 7 days, rotating the movies. */
    private void seedShows(LocalDate today) {
        List<Theater> theaters = theaterRepository.findAll();
        List<Movie> movies = movieRepository.findByActiveTrueOrderByReleaseDateDesc();
        if (theaters.isEmpty() || movies.isEmpty()) return;
        LocalTime[] slots = {LocalTime.of(10, 0), LocalTime.of(13, 45), LocalTime.of(17, 30), LocalTime.of(21, 15)};
        LocalDateTime now = LocalDateTime.now(clock);
        int created = 0;
        for (int d = 0; d < 7; d++) {
            for (int t = 0; t < theaters.size(); t++) {
                for (int i = 0; i < slots.length; i++) {
                    LocalDateTime start = today.plusDays(d).atTime(slots[i]);
                    if (start.isBefore(now.plusMinutes(30))) continue;
                    Theater theater = theaters.get(t);
                    boolean exists = showRepository.findPossiblyOverlapping(theater.getId(), start.minusMinutes(1), start.plusMinutes(1)).size() > 0;
                    if (exists) continue;
                    Show s = new Show();
                    s.setTheater(theater);
                    s.setMovie(movies.get((d + t + i) % movies.size()));
                    s.setStartTime(start);
                    boolean evening = slots[i].getHour() >= 17;
                    s.setPrice(BigDecimal.valueOf(evening ? 190 : 150));
                    s.setPremiumPrice(BigDecimal.valueOf(evening ? 290 : 240));
                    showRepository.save(s);
                    created++;
                }
            }
        }
        log.info("Created {} sample shows", created);
    }

    private static Theater theater(String name, String city, String address, int rows, int perRow, int premium) {
        Theater t = new Theater();
        t.setName(name);
        t.setCity(city);
        t.setAddress(address);
        t.setTotalRows(rows);
        t.setSeatsPerRow(perRow);
        t.setPremiumRows(premium);
        return t;
    }

    private static Movie movie(String title, String genre, String lang, int minutes, String cert, double rating,
                               String description, String poster) {
        Movie m = new Movie();
        m.setTitle(title);
        m.setGenre(genre);
        m.setLanguage(lang);
        m.setDurationMinutes(minutes);
        m.setCertificate(cert);
        m.setRating(rating);
        m.setDescription(description);
        m.setPosterUrl(poster);
        m.setReleaseDate(LocalDate.now().minusDays(minutes % 20));
        m.setActive(true);
        return m;
    }
}

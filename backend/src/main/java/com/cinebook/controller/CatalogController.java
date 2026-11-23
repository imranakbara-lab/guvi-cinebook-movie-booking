package com.cinebook.controller;

import com.cinebook.dto.*;
import com.cinebook.service.MovieService;
import com.cinebook.service.ShowService;
import com.cinebook.service.TheaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Public browsing: movies, theaters, showtimes and seat availability. */
@RestController
@RequestMapping("/api")
@Tag(name = "Browse", description = "Movies, theaters, shows and seat availability (public)")
public class CatalogController {

    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ShowService showService;

    public CatalogController(MovieService movieService, TheaterService theaterService, ShowService showService) {
        this.movieService = movieService;
        this.theaterService = theaterService;
        this.showService = showService;
    }

    @GetMapping("/movies")
    @Operation(summary = "Browse movies by date, genre, theater, city or search text")
    public List<MovieResponse> movies(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam(required = false) String genre,
                                      @RequestParam(required = false) Long theaterId,
                                      @RequestParam(required = false) String city,
                                      @RequestParam(required = false) String q) {
        return movieService.browse(date, genre, theaterId, city, q);
    }

    @GetMapping("/movies/{id}")
    @Operation(summary = "Movie details")
    public MovieResponse movie(@PathVariable Long id) { return movieService.get(id); }

    @GetMapping("/movies/{id}/shows")
    @Operation(summary = "Upcoming shows of a movie, optionally for one date and city")
    public List<ShowResponse> showsForMovie(@PathVariable Long id,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @RequestParam(required = false) String city) {
        return showService.showsForMovie(id, date, city);
    }

    @GetMapping("/genres")
    @Operation(summary = "All genres")
    public List<String> genres() { return movieService.genres(); }

    @GetMapping("/theaters")
    @Operation(summary = "All theaters")
    public List<TheaterResponse> theaters() { return theaterService.all(); }

    @GetMapping("/cities")
    @Operation(summary = "Cities that have theaters")
    public List<String> cities() { return theaterService.cities(); }

    @GetMapping("/shows/{id}")
    @Operation(summary = "Show details")
    public ShowResponse show(@PathVariable Long id) { return showService.get(id); }

    @GetMapping("/shows/{id}/seats")
    @Operation(summary = "Seat layout and taken seats (poll this to see seats taken by others in real time)")
    public SeatMapResponse seats(@PathVariable Long id) { return showService.seatMap(id); }
}

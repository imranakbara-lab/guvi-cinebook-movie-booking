package com.cinebook.controller;

import com.cinebook.dto.*;
import com.cinebook.security.UserPrincipal;
import com.cinebook.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Movies, theaters, shows, bookings and users (ADMIN only)")
public class AdminController {

    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ShowService showService;
    private final BookingService bookingService;
    private final UserService userService;

    public AdminController(MovieService movieService, TheaterService theaterService, ShowService showService,
                           BookingService bookingService, UserService userService) {
        this.movieService = movieService;
        this.theaterService = theaterService;
        this.showService = showService;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    // ----- Movies -----
    @GetMapping("/movies") @Operation(summary = "All movies incl. inactive")
    public List<MovieResponse> movies() { return movieService.all(); }

    @PostMapping("/movies") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Add a movie")
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest r) { return movieService.create(r); }

    @PutMapping("/movies/{id}") @Operation(summary = "Update a movie")
    public MovieResponse updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest r) { return movieService.update(id, r); }

    @DeleteMapping("/movies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "Delete a movie without bookings")
    public void deleteMovie(@PathVariable Long id) { movieService.delete(id); }

    // ----- Theaters -----
    @PostMapping("/theaters") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Add a theater with seat layout")
    public TheaterResponse createTheater(@Valid @RequestBody TheaterRequest r) { return theaterService.create(r); }

    @PutMapping("/theaters/{id}") @Operation(summary = "Update a theater")
    public TheaterResponse updateTheater(@PathVariable Long id, @Valid @RequestBody TheaterRequest r) { return theaterService.update(id, r); }

    @DeleteMapping("/theaters/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "Delete a theater without shows")
    public void deleteTheater(@PathVariable Long id) { theaterService.delete(id); }

    // ----- Shows -----
    @GetMapping("/shows") @Operation(summary = "Shows (upcoming by default) with available seats")
    public List<ShowResponse> shows(@RequestParam(defaultValue = "true") boolean upcoming) { return showService.all(upcoming); }

    @PostMapping("/shows") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Schedule a show (checks screen clashes)")
    public ShowResponse createShow(@Valid @RequestBody ShowRequest r) { return showService.create(r); }

    @PutMapping("/shows/{id}") @Operation(summary = "Update a show")
    public ShowResponse updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequest r) { return showService.update(id, r); }

    @DeleteMapping("/shows/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "Delete a show without bookings")
    public void deleteShow(@PathVariable Long id) { showService.delete(id); }

    // ----- Bookings -----
    @GetMapping("/bookings") @Operation(summary = "All bookings, optionally for one show")
    public List<BookingResponse> bookings(@RequestParam(required = false) Long showId) { return bookingService.all(showId); }

    @PostMapping("/bookings/{id}/cancel") @Operation(summary = "Cancel any booking (refund if paid)")
    public BookingResponse cancelBooking(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        return bookingService.cancel(id, me);
    }

    // ----- Users -----
    @GetMapping("/users") @Operation(summary = "List users")
    public List<UserResponse> users() { return userService.listAll(); }

    @PutMapping("/users/{id}") @Operation(summary = "Edit a user")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest r) { return userService.update(id, r); }

    @DeleteMapping("/users/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "Delete a user and their bookings")
    public void deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) { userService.delete(id, me.getId()); }
}

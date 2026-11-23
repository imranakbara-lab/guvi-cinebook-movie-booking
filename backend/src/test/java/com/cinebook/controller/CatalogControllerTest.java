package com.cinebook.controller;

import com.cinebook.config.SecurityConfig;
import com.cinebook.dto.MovieResponse;
import com.cinebook.dto.SeatMapResponse;
import com.cinebook.dto.ShowResponse;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.security.CustomUserDetailsService;
import com.cinebook.security.JwtService;
import com.cinebook.service.MovieService;
import com.cinebook.service.ShowService;
import com.cinebook.service.TheaterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class CatalogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private MovieService movieService;
    @MockBean private TheaterService theaterService;
    @MockBean private ShowService showService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    private ShowResponse show() {
        return new ShowResponse(10L, 2L, "Velocity", null, "Action", "Tamil", "UA", 120, 1L, "Screen 1", "Erode", null,
                LocalDateTime.of(2026, 3, 2, 18, 0), LocalDateTime.of(2026, 3, 2, 20, 0),
                new BigDecimal("150"), new BigDecimal("250"), 12, 10);
    }

    @Test
    void browseMovies_isPublicAndPassesFilters() throws Exception {
        when(movieService.browse(eq(LocalDate.of(2026, 3, 2)), eq("Action"), eq(1L), isNull(), isNull())).thenReturn(List.of(
                new MovieResponse(2L, "Velocity", "d", "Action", "Tamil", 120, "UA", 7.6, null, null, true)));

        mockMvc.perform(get("/api/movies").param("date", "2026-03-02").param("genre", "Action").param("theaterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Velocity"));
    }

    @Test
    void seatMap_isPublic() throws Exception {
        when(showService.seatMap(10L)).thenReturn(new SeatMapResponse(show(), List.of("A", "B", "C"), 4, List.of("C"), List.of("A1", "B2")));

        mockMvc.perform(get("/api/shows/10/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.takenSeats[1]").value("B2"))
                .andExpect(jsonPath("$.premiumRows[0]").value("C"))
                .andExpect(jsonPath("$.show.availableSeats").value(10));
    }

    @Test
    void unknownMovie_returns404() throws Exception {
        when(movieService.get(99L)).thenThrow(new ResourceNotFoundException("Movie not found with id 99"));

        mockMvc.perform(get("/api/movies/99")).andExpect(status().isNotFound());
    }

    @Test
    void badDate_returns400() throws Exception {
        mockMvc.perform(get("/api/movies").param("date", "not-a-date")).andExpect(status().isBadRequest());
    }
}

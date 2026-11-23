package com.cinebook.controller;

import com.cinebook.config.SecurityConfig;
import com.cinebook.dto.*;
import com.cinebook.exception.BadRequestException;
import com.cinebook.security.CustomUserDetailsService;
import com.cinebook.security.JwtService;
import com.cinebook.security.UserPrincipal;
import com.cinebook.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private MovieService movieService;
    @MockBean private TheaterService theaterService;
    @MockBean private ShowService showService;
    @MockBean private BookingService bookingService;
    @MockBean private UserService userService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    private final UserPrincipal admin = new UserPrincipal(1L, "admin@mail.com", "x", "ADMIN");
    private final UserPrincipal normalUser = new UserPrincipal(5L, "u@mail.com", "x", "USER");

    private static final String MOVIE = """
            {"title":"New Film","genre":"Drama","language":"Tamil","durationMinutes":130,"certificate":"UA","rating":8.1,"active":true}
            """;

    @Test
    void createMovie_asAdmin_returns201() throws Exception {
        when(movieService.create(any(MovieRequest.class)))
                .thenReturn(new MovieResponse(7L, "New Film", null, "Drama", "Tamil", 130, "UA", 8.1, null, null, true));

        mockMvc.perform(post("/api/admin/movies").with(user(admin)).contentType(MediaType.APPLICATION_JSON).content(MOVIE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void createMovie_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/movies").with(user(normalUser)).contentType(MediaType.APPLICATION_JSON).content(MOVIE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(movieService);
    }

    @Test
    void createMovie_invalid_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/movies").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"genre\":\"Drama\",\"durationMinutes\":5,\"rating\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.durationMinutes").exists())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
    }

    @Test
    void createTheater_validatesLayout() throws Exception {
        mockMvc.perform(post("/api/admin/theaters").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Screen\",\"city\":\"Erode\",\"totalRows\":30,\"seatsPerRow\":10,\"premiumRows\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.totalRows").exists());
    }

    @Test
    void createShow_inPast_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/shows").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":1,\"theaterId\":1,\"startTime\":\"2020-01-01T10:00:00\",\"price\":150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.startTime").exists());
    }

    @Test
    void createShow_clash_returns400WithMessage() throws Exception {
        when(showService.create(any(ShowRequest.class))).thenThrow(new BadRequestException("Screen is busy"));

        mockMvc.perform(post("/api/admin/shows").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":1,\"theaterId\":1,\"startTime\":\"2099-01-01T10:00:00\",\"price\":150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Screen is busy"));
    }

    @Test
    void deleteShow_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/shows/5").with(user(admin))).andExpect(status().isNoContent());
        verify(showService).delete(5L);
    }
}

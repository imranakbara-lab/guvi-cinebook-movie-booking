package com.cinebook.controller;

import com.cinebook.config.SecurityConfig;
import com.cinebook.dto.*;
import com.cinebook.entity.BookingStatus;
import com.cinebook.entity.PaymentStatus;
import com.cinebook.exception.ConflictException;
import com.cinebook.security.CustomUserDetailsService;
import com.cinebook.security.JwtService;
import com.cinebook.security.UserPrincipal;
import com.cinebook.service.BookingService;
import com.cinebook.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private BookingService bookingService;
    @MockBean private PaymentService paymentService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    private final UserPrincipal me = new UserPrincipal(5L, "u@mail.com", "x", "USER");

    private BookingResponse booking(BookingStatus status) {
        ShowResponse show = new ShowResponse(10L, 2L, "Velocity", null, "Action", "Tamil", "UA", 120, 1L, "Screen 1",
                "Erode", null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                BigDecimal.TEN, BigDecimal.TEN, 12, 10);
        return new BookingResponse(1L, "CBABC123", status, PaymentStatus.PENDING, null, null, List.of("A1", "A2"),
                new BigDecimal("300"), LocalDateTime.now(), LocalDateTime.now().plusMinutes(10), 600, null, null, true,
                "U", "u@mail.com", show);
    }

    @Test
    void hold_returns201() throws Exception {
        when(bookingService.hold(any(BookingRequest.class), any(UserPrincipal.class))).thenReturn(booking(BookingStatus.PENDING_PAYMENT));

        mockMvc.perform(post("/api/bookings").with(user(me)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":10,\"seats\":[\"A1\",\"A2\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.holdSecondsLeft").value(600));
    }

    @Test
    void hold_requiresLogin() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{\"showId\":10,\"seats\":[\"A1\"]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(bookingService);
    }

    @Test
    void hold_tooManySeats_returns400() throws Exception {
        mockMvc.perform(post("/api/bookings").with(user(me)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":10,\"seats\":[\"A1\",\"A2\",\"A3\",\"A4\",\"A5\",\"A6\",\"A7\",\"A8\",\"A9\",\"A10\",\"A11\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.seats").exists());
    }

    @Test
    void hold_seatTaken_returns409() throws Exception {
        when(bookingService.hold(any(BookingRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ConflictException("Seat(s) A1 are no longer available"));

        mockMvc.perform(post("/api/bookings").with(user(me)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":10,\"seats\":[\"A1\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Seat(s) A1 are no longer available"));
    }

    @Test
    void paymentOrder_and_confirm() throws Exception {
        when(paymentService.createOrder(eq(1L), any(UserPrincipal.class))).thenReturn(
                new PaymentOrderResponse(1L, "SIMULATED", null, "sim_order_1", 30000, "INR", "U", "u@mail.com", "Velocity"));
        when(paymentService.confirm(eq(1L), any(), any(UserPrincipal.class))).thenReturn(booking(BookingStatus.CONFIRMED));

        mockMvc.perform(post("/api/bookings/1/payment-order").with(user(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("SIMULATED"))
                .andExpect(jsonPath("$.amountInPaise").value(30000));
        mockMvc.perform(post("/api/bookings/1/payment-confirm").with(user(me)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void myBookings_and_cancel() throws Exception {
        when(bookingService.myBookings(any(UserPrincipal.class))).thenReturn(List.of(booking(BookingStatus.CONFIRMED)));
        when(bookingService.cancel(eq(1L), any(UserPrincipal.class))).thenReturn(booking(BookingStatus.CANCELLED));

        mockMvc.perform(get("/api/bookings/my").with(user(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reference").value("CBABC123"));
        mockMvc.perform(post("/api/bookings/1/cancel").with(user(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}

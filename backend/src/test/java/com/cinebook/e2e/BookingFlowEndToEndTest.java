package com.cinebook.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end user workflow on the real application (H2, real security and services):
 * admin sets up a movie, theater and show -> user browses, holds seats -> another user cannot take them ->
 * user pays (simulated) -> booking confirmed -> history -> cancel -> refund and seats released.
 */
@SpringBootTest(properties = {"app.seed-sample-data=false", "spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class BookingFlowEndToEndTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expected) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String res = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return res.isBlank() ? null : json.readTree(res);
    }

    private String register(String name, String email) throws Exception {
        return call(post("/api/auth/register"), null, Map.of("name", name, "email", email, "password", "secret123"), 201)
                .get("token").asText();
    }

    @Test
    void fullBookingWorkflow() throws Exception {
        // 1. Admin creates theater, movie and a show tomorrow evening
        String admin = call(post("/api/auth/login"), null, Map.of("email", "admin@cinebook.com", "password", "Admin@123"), 200)
                .get("token").asText();
        long theaterId = call(post("/api/admin/theaters"), admin, Map.of("name", "Screen 1", "city", "Erode",
                "totalRows", 5, "seatsPerRow", 8, "premiumRows", 1), 201).get("id").asLong();
        long movieId = call(post("/api/admin/movies"), admin, Map.of("title", "Velocity", "genre", "Action",
                "language", "Tamil", "durationMinutes", 140, "certificate", "UA", "rating", 7.5, "active", true), 201).get("id").asLong();
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
        long showId = call(post("/api/admin/shows"), admin, Map.of("movieId", movieId, "theaterId", theaterId,
                "startTime", start.toString(), "price", 150, "premiumPrice", 250), 201).get("id").asLong();

        // A clashing show on the same screen is refused
        call(post("/api/admin/shows"), admin, Map.of("movieId", movieId, "theaterId", theaterId,
                "startTime", start.plusHours(1).toString(), "price", 150), 400);

        // 2. Public browsing by date and genre
        JsonNode movies = call(get("/api/movies").param("date", start.toLocalDate().toString()).param("genre", "action"), null, null, 200);
        assertThat(movies.get(0).get("title").asText()).isEqualTo("Velocity");
        assertThat(call(get("/api/movies/" + movieId + "/shows").param("date", start.toLocalDate().toString()), null, null, 200)).hasSize(1);

        // 3. User holds two seats (one premium in row E)
        String priya = register("Priya", "priya@mail.com");
        JsonNode hold = call(post("/api/bookings"), priya, Map.of("showId", showId, "seats", List.of("A1", "E1")), 201);
        long bookingId = hold.get("id").asLong();
        assertThat(hold.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(hold.get("amount").decimalValue()).isEqualByComparingTo("400");

        // Seats are locked immediately - another user gets 409
        String ravi = register("Ravi", "ravi@mail.com");
        call(post("/api/bookings"), ravi, Map.of("showId", showId, "seats", List.of("E1", "E2")), 409);
        assertThat(call(get("/api/shows/" + showId + "/seats"), null, null, 200).get("takenSeats")).hasSize(2);

        // 4. Pay (simulated) and confirm
        JsonNode order = call(post("/api/bookings/" + bookingId + "/payment-order"), priya, null, 200);
        assertThat(order.get("provider").asText()).isEqualTo("SIMULATED");
        assertThat(order.get("amountInPaise").asLong()).isEqualTo(40000);
        call(post("/api/bookings/" + bookingId + "/payment-order"), ravi, null, 403);
        JsonNode confirmed = call(post("/api/bookings/" + bookingId + "/payment-confirm"), priya, Map.of(), 200);
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmed.get("paymentStatus").asText()).isEqualTo("PAID");

        // 5. Booking history; admin sees it too
        assertThat(call(get("/api/bookings/my"), priya, null, 200).get(0).get("reference").asText()).startsWith("CB");
        assertThat(call(get("/api/admin/bookings").param("showId", String.valueOf(showId)), admin, null, 200)).hasSize(1);
        call(get("/api/admin/bookings"), priya, null, 403);

        // 6. Cancel -> refunded and seats free again; Ravi can now book E1
        JsonNode cancelled = call(post("/api/bookings/" + bookingId + "/cancel"), priya, null, 200);
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("paymentStatus").asText()).isEqualTo("REFUNDED");
        assertThat(call(get("/api/shows/" + showId + "/seats"), null, null, 200).get("takenSeats")).isEmpty();
        call(post("/api/bookings"), ravi, Map.of("showId", showId, "seats", List.of("E1")), 201);

        // 7. Profile management
        call(post("/api/bookings/" + bookingId + "/cancel"), priya, null, 400);   // already cancelled
        JsonNode profile = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/me"), priya,
                Map.of("name", "Priya S", "phone", "9876543210"), 200);
        assertThat(profile.get("name").asText()).isEqualTo("Priya S");
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/me/password"), priya,
                Map.of("currentPassword", "wrong", "newPassword", "newsecret1"), 400);
    }
}

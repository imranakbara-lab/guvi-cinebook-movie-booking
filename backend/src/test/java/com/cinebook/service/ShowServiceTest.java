package com.cinebook.service;

import com.cinebook.TestData;
import com.cinebook.dto.SeatMapResponse;
import com.cinebook.dto.ShowRequest;
import com.cinebook.dto.ShowResponse;
import com.cinebook.entity.Show;
import com.cinebook.exception.BadRequestException;
import com.cinebook.repository.BookedSeatRepository;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 9, 0);

    @Mock private ShowRepository showRepository;
    @Mock private BookedSeatRepository bookedSeatRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private MovieService movieService;
    @Mock private TheaterService theaterService;

    private ShowService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ShowService(showRepository, bookedSeatRepository, bookingRepository, movieService, theaterService, clock);
    }

    @Test
    void create_clashingWithRunningShow_rejected() {
        Show existing = TestData.show(NOW.plusDays(1).withHour(10));            // 10:00 - 12:00 (+15 min cleaning)
        when(movieService.find(2L)).thenReturn(TestData.movie());
        when(theaterService.find(1L)).thenReturn(TestData.theater());
        when(showRepository.findPossiblyOverlapping(eq(1L), any(), any())).thenReturn(List.of(existing));

        ShowRequest req = new ShowRequest(2L, 1L, NOW.plusDays(1).withHour(12), new BigDecimal("120"), null);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(BadRequestException.class).hasMessageContaining("Screen is busy");
    }

    @Test
    void create_afterCleaningGap_ok() {
        Show existing = TestData.show(NOW.plusDays(1).withHour(10));
        when(movieService.find(2L)).thenReturn(TestData.movie());
        when(theaterService.find(1L)).thenReturn(TestData.theater());
        when(showRepository.findPossiblyOverlapping(eq(1L), any(), any())).thenReturn(List.of(existing));
        when(showRepository.save(any(Show.class))).thenAnswer(inv -> { Show s = inv.getArgument(0); s.setId(11L); return s; });

        ShowResponse res = service.create(new ShowRequest(2L, 1L, NOW.plusDays(1).withHour(12).withMinute(15), new BigDecimal("120"), null));

        assertThat(res.premiumPrice()).isEqualByComparingTo("120");   // defaults to regular price
        assertThat(res.availableSeats()).isEqualTo(12);
    }

    @Test
    void seatMap_listsRowsPremiumAndTaken() {
        when(showRepository.findById(10L)).thenReturn(Optional.of(TestData.show(NOW.plusDays(1))));
        when(bookedSeatRepository.findSeatLabelsByShowId(10L)).thenReturn(List.of("A1", "C4"));

        SeatMapResponse map = service.seatMap(10L);

        assertThat(map.rows()).containsExactly("A", "B", "C");
        assertThat(map.premiumRows()).containsExactly("C");
        assertThat(map.takenSeats()).containsExactly("A1", "C4");
        assertThat(map.show().availableSeats()).isEqualTo(10);
    }

    @Test
    void delete_withBookings_rejected() {
        when(showRepository.findById(10L)).thenReturn(Optional.of(TestData.show(NOW.plusDays(1))));
        when(bookingRepository.existsByShowIdAndStatusIn(eq(10L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L)).isInstanceOf(BadRequestException.class);
        verify(showRepository, never()).delete(any());
    }

    @Test
    void showsForMovie_filtersByMovieAndCity() {
        Show match = TestData.show(NOW.plusHours(5));
        Show otherCity = TestData.show(NOW.plusHours(6));
        otherCity.getTheater().setCity("Chennai");
        when(showRepository.findActiveBetween(any(), any())).thenReturn(List.of(match, otherCity));

        assertThat(service.showsForMovie(2L, NOW.toLocalDate(), "erode")).hasSize(1);
    }
}

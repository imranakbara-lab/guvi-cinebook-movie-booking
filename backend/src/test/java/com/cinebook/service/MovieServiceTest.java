package com.cinebook.service;

import com.cinebook.TestData;
import com.cinebook.dto.MovieRequest;
import com.cinebook.dto.MovieResponse;
import com.cinebook.entity.Movie;
import com.cinebook.entity.Show;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 9, 0);

    @Mock private MovieRepository movieRepository;
    @Mock private ShowRepository showRepository;
    @Mock private BookingRepository bookingRepository;

    private MovieService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new MovieService(movieRepository, showRepository, bookingRepository, clock);
    }

    private Movie comedy() {
        Movie m = TestData.movie();
        m.setId(3L);
        m.setTitle("Laugh Out Loud");
        m.setGenre("Comedy");
        return m;
    }

    @Test
    void browse_withoutFilters_returnsActiveMoviesMatchingGenre() {
        when(movieRepository.findByActiveTrueOrderByReleaseDateDesc()).thenReturn(List.of(TestData.movie(), comedy()));

        assertThat(service.browse(null, "comedy", null, null, null)).extracting(MovieResponse::title).containsExactly("Laugh Out Loud");
    }

    @Test
    void browse_byDateAndTheater_usesShows() {
        Show s1 = TestData.show(NOW.plusHours(3));
        Show s2 = TestData.show(NOW.plusHours(6));            // same movie twice -> listed once
        when(showRepository.findActiveBetween(any(), any())).thenReturn(List.of(s1, s2));

        List<MovieResponse> res = service.browse(NOW.toLocalDate(), null, 1L, null, null);

        assertThat(res).extracting(MovieResponse::title).containsExactly("Velocity");
        verify(movieRepository, never()).findByActiveTrueOrderByReleaseDateDesc();
    }

    @Test
    void browse_searchText() {
        when(movieRepository.findByActiveTrueOrderByReleaseDateDesc()).thenReturn(List.of(TestData.movie(), comedy()));

        assertThat(service.browse(null, null, null, null, "velo")).hasSize(1);
    }

    @Test
    void create_trimsAndSaves() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        MovieResponse res = service.create(new MovieRequest(" New Film ", "d", "Drama", "Tamil", 130, "UA", 8.0, "", null, true));

        assertThat(res.title()).isEqualTo("New Film");
        assertThat(res.posterUrl()).isNull();
    }

    @Test
    void delete_withBookings_rejected() {
        when(movieRepository.findById(2L)).thenReturn(Optional.of(TestData.movie()));
        when(bookingRepository.existsByShowMovieIdAndStatusIn(eq(2L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(BadRequestException.class).hasMessageContaining("inactive");
    }

    @Test
    void get_inactiveMovie_notFound() {
        Movie m = TestData.movie();
        m.setActive(false);
        when(movieRepository.findById(2L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.get(2L)).isInstanceOf(ResourceNotFoundException.class);
    }
}

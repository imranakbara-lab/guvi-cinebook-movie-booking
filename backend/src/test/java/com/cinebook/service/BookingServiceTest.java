package com.cinebook.service;

import com.cinebook.TestData;
import com.cinebook.dto.BookingRequest;
import com.cinebook.dto.BookingResponse;
import com.cinebook.entity.*;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ConflictException;
import com.cinebook.exception.ForbiddenException;
import com.cinebook.repository.BookedSeatRepository;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowRepository;
import com.cinebook.repository.UserRepository;
import com.cinebook.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

    @Mock private BookingRepository bookingRepository;
    @Mock private BookedSeatRepository bookedSeatRepository;
    @Mock private ShowRepository showRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    private BookingService service;
    private User user;
    private final UserPrincipal me = new UserPrincipal(5L, "u@mail.com", "x", "USER");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new BookingService(bookingRepository, bookedSeatRepository, showRepository, userRepository,
                emailService, clock, 10, 60);
        user = TestData.user(5L, "u@mail.com", Role.USER);
    }

    private void stubHappyHold(Show show) {
        when(showRepository.findById(10L)).thenReturn(Optional.of(show));
        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING_PAYMENT, NOW)).thenReturn(List.of());
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        when(bookedSeatRepository.findTaken(eq(10L), anyList())).thenReturn(List.of());
    }

    @Test
    void hold_locksSeatsAndPricesPremiumRow() {
        Show show = TestData.show(NOW.plusDays(1));
        stubHappyHold(show);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> { Booking b = inv.getArgument(0); b.setId(1L); return b; });

        BookingResponse res = service.hold(new BookingRequest(10L, List.of("a1", "C2", "A1")), me);

        assertThat(res.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(res.seats()).containsExactly("A1", "C2");                 // normalised + de-duplicated
        assertThat(res.amount()).isEqualByComparingTo(new BigDecimal("250"));  // 100 regular + 150 premium
        assertThat(res.holdSecondsLeft()).isEqualTo(600);
        assertThat(res.reference()).startsWith("CB").hasSize(8);
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getLockedSeats()).extracting(BookedSeat::getSeatLabel).containsExactly("A1", "C2");
    }

    @Test
    void hold_seatAlreadyTaken_conflict() {
        Show show = TestData.show(NOW.plusDays(1));
        when(showRepository.findById(10L)).thenReturn(Optional.of(show));
        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(any(), any())).thenReturn(List.of());
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        when(bookedSeatRepository.findTaken(eq(10L), anyList())).thenReturn(List.of("A2"));

        assertThatThrownBy(() -> service.hold(new BookingRequest(10L, List.of("A1", "A2")), me))
                .isInstanceOf(ConflictException.class).hasMessageContaining("A2");
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void hold_invalidSeat_rejected() {
        when(showRepository.findById(10L)).thenReturn(Optional.of(TestData.show(NOW.plusDays(1))));

        assertThatThrownBy(() -> service.hold(new BookingRequest(10L, List.of("Z9")), me))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Z9");
    }

    @Test
    void hold_startedShow_rejected() {
        when(showRepository.findById(10L)).thenReturn(Optional.of(TestData.show(NOW.minusMinutes(5))));

        assertThatThrownBy(() -> service.hold(new BookingRequest(10L, List.of("A1")), me))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("started");
    }

    @Test
    void hold_releasesMyOlderHoldForSameShow() {
        Show show = TestData.show(NOW.plusDays(1));
        Booking old = TestData.booking(user, show, BookingStatus.PENDING_PAYMENT, NOW.minusMinutes(2));
        when(showRepository.findById(10L)).thenReturn(Optional.of(show));
        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(any(), any())).thenReturn(List.of());
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(old));
        when(bookedSeatRepository.findTaken(eq(10L), anyList())).thenReturn(List.of());
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.hold(new BookingRequest(10L, List.of("A1")), me);

        assertThat(old.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(old.getLockedSeats()).isEmpty();
    }

    @Test
    void expireHolds_releasesSeatsOfUnpaidBookings() {
        Booking stale = TestData.booking(user, TestData.show(NOW.plusDays(1)), BookingStatus.PENDING_PAYMENT, NOW.minusMinutes(15));
        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING_PAYMENT, NOW)).thenReturn(List.of(stale));

        assertThat(service.expireHolds()).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(stale.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(stale.getLockedSeats()).isEmpty();
    }

    @Test
    void confirm_marksPaidAndSendsEmail() {
        Booking b = TestData.booking(user, TestData.show(NOW.plusDays(1)), BookingStatus.PENDING_PAYMENT, NOW.minusMinutes(1));
        when(bookingRepository.save(b)).thenReturn(b);

        service.confirm(b, "SIMULATED", "sim_order", "sim_pay");

        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(b.getLockedSeats()).hasSize(2);       // seats stay locked
        verify(emailService).sendBookingConfirmed(argThat(t -> t.reference().equals("CBTEST01") && t.seats().equals("A1, A2")));
    }

    @Test
    void cancel_confirmedBooking_refundsAndReleasesSeats() {
        Booking b = TestData.booking(user, TestData.show(NOW.plusDays(1)), BookingStatus.CONFIRMED, NOW.minusDays(1));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(b));

        BookingResponse res = service.cancel(100L, me);

        assertThat(res.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(res.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(b.getLockedSeats()).isEmpty();
        verify(emailService).sendBookingCancelled(any(), eq(true));
    }

    @Test
    void cancel_tooCloseToShow_rejected() {
        Booking b = TestData.booking(user, TestData.show(NOW.plusMinutes(30)), BookingStatus.CONFIRMED, NOW.minusDays(1));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancel(100L, me)).isInstanceOf(BadRequestException.class).hasMessageContaining("60 minutes");
    }

    @Test
    void cancel_adminCanCancelUntilShowStarts() {
        Booking b = TestData.booking(user, TestData.show(NOW.plusMinutes(30)), BookingStatus.CONFIRMED, NOW.minusDays(1));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(b));

        assertThat(service.cancel(100L, new UserPrincipal(1L, "admin@mail.com", "x", "ADMIN")).status())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_someoneElsesBooking_forbidden() {
        Booking b = TestData.booking(user, TestData.show(NOW.plusDays(1)), BookingStatus.CONFIRMED, NOW.minusDays(1));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancel(100L, new UserPrincipal(6L, "o@mail.com", "x", "USER")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void myBookings_flagsCancellable() {
        Booking b = TestData.booking(user, TestData.show(NOW.plusDays(1)), BookingStatus.CONFIRMED, NOW.minusDays(1));
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(b));

        assertThat(service.myBookings(me)).singleElement().satisfies(r -> {
            assertThat(r.cancellable()).isTrue();
            assertThat(r.show().movieTitle()).isEqualTo("Velocity");
        });
    }
}

package com.cinebook.service;

import com.cinebook.dto.BookingRequest;
import com.cinebook.dto.BookingResponse;
import com.cinebook.entity.*;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ConflictException;
import com.cinebook.exception.ForbiddenException;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookedSeatRepository;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowRepository;
import com.cinebook.repository.UserRepository;
import com.cinebook.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Seat booking + booking lifecycle.
 * 1. hold(): locks the chosen seats for a few minutes (status PENDING_PAYMENT).
 * 2. PaymentService confirms it (CONFIRMED) - or the hold expires and the seats are released (EXPIRED).
 * 3. cancel(): releases seats and refunds a paid booking (CANCELLED).
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final DateTimeFormatter SHOW_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, hh:mm a");

    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Clock clock;
    private final int holdMinutes;
    private final int cancelBeforeMinutes;
    private final SecureRandom random = new SecureRandom();

    public BookingService(BookingRepository bookingRepository, BookedSeatRepository bookedSeatRepository,
                          ShowRepository showRepository, UserRepository userRepository, EmailService emailService,
                          Clock clock,
                          @Value("${app.booking.hold-minutes:10}") int holdMinutes,
                          @Value("${app.booking.cancel-before-minutes:60}") int cancelBeforeMinutes) {
        this.bookingRepository = bookingRepository;
        this.bookedSeatRepository = bookedSeatRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.clock = clock;
        this.holdMinutes = holdMinutes;
        this.cancelBeforeMinutes = cancelBeforeMinutes;
    }

    /** Locks the seats for this user. Fails with 409 if any seat is already held or booked. */
    @Transactional
    public BookingResponse hold(BookingRequest req, UserPrincipal me) {
        LocalDateTime now = now();
        Show show = showRepository.findById(req.showId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id " + req.showId()));
        if (!show.getStartTime().isAfter(now)) throw new BadRequestException("This show has already started");
        if (!show.getMovie().isActive()) throw new BadRequestException("This movie is no longer available");

        List<String> seats = normalise(req.seats());
        Theater theater = show.getTheater();
        List<String> invalid = seats.stream().filter(s -> !theater.isValidSeat(s)).toList();
        if (!invalid.isEmpty()) throw new BadRequestException("Invalid seat(s): " + String.join(", ", invalid));

        expireHolds();
        // a user who goes back and picks again should not block themselves
        bookingRepository.findByUserIdOrderByCreatedAtDesc(me.getId()).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT && b.getShow().getId().equals(show.getId()))
                .forEach(b -> release(b, BookingStatus.EXPIRED, now));
        bookedSeatRepository.flush();

        List<String> taken = bookedSeatRepository.findTaken(show.getId(), seats);
        if (!taken.isEmpty()) {
            throw new ConflictException("Seat(s) " + String.join(", ", taken) + " are no longer available");
        }

        User user = userRepository.findById(me.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Booking b = new Booking();
        b.setReference(newReference());
        b.setUser(user);
        b.setShow(show);
        b.setSeatLabels(String.join(",", seats));
        b.setAmount(seats.stream().map(show::priceFor).reduce(BigDecimal.ZERO, BigDecimal::add));
        b.setCreatedAt(now);
        b.setHoldExpiresAt(now.plusMinutes(holdMinutes));
        seats.forEach(seat -> b.getLockedSeats().add(new BookedSeat(show.getId(), seat, b)));
        // flush now so the unique (show, seat) constraint fires inside this request if someone else won the race
        Booking saved = bookingRepository.saveAndFlush(b);
        return BookingResponse.from(saved, now, false);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id, UserPrincipal me) {
        Booking b = find(id);
        if (!me.isAdmin() && !b.getUser().getId().equals(me.getId())) throw new ForbiddenException("This is not your booking");
        return toResponse(b);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(UserPrincipal me) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(me.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> all(Long showId) {
        List<Booking> list = showId == null ? bookingRepository.findAllByOrderByCreatedAtDesc()
                : bookingRepository.findByShowIdOrderByCreatedAtDesc(showId);
        return list.stream().map(this::toResponse).toList();
    }

    /** Users can cancel until shortly before the show; admins any time before it starts. Paid bookings are refunded. */
    @Transactional
    public BookingResponse cancel(Long id, UserPrincipal me) {
        Booking b = find(id);
        if (!me.isAdmin() && !b.getUser().getId().equals(me.getId())) throw new ForbiddenException("This is not your booking");
        LocalDateTime now = now();
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.EXPIRED) {
            throw new BadRequestException("This booking is already " + b.getStatus().name().toLowerCase());
        }
        if (!canCancel(b, now, me.isAdmin())) {
            throw new BadRequestException("Bookings can only be cancelled up to " + cancelBeforeMinutes + " minutes before the show");
        }
        boolean refund = b.getPaymentStatus() == PaymentStatus.PAID;
        release(b, BookingStatus.CANCELLED, now);
        if (refund) b.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(b);
        if (refund || b.getConfirmedAt() != null) emailService.sendBookingCancelled(ticket(b), refund);
        return toResponse(b);
    }

    /** Called by PaymentService after a successful payment. */
    @Transactional
    public Booking confirm(Booking b, String provider, String orderId, String paymentId) {
        LocalDateTime now = now();
        b.setStatus(BookingStatus.CONFIRMED);
        b.setPaymentStatus(PaymentStatus.PAID);
        b.setPaymentProvider(provider);
        b.setPaymentOrderId(orderId);
        b.setPaymentId(paymentId);
        b.setConfirmedAt(now);
        Booking saved = bookingRepository.save(b);
        emailService.sendBookingConfirmed(ticket(saved));
        return saved;
    }

    /** Releases seats of unpaid bookings whose hold time is over. Runs every minute and before each new hold. */
    @Scheduled(fixedDelayString = "${app.booking.expiry-check-ms:60000}", initialDelay = 30000)
    @Transactional
    public int expireHolds() {
        LocalDateTime now = now();
        List<Booking> stale = bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING_PAYMENT, now);
        stale.forEach(b -> release(b, BookingStatus.EXPIRED, now));
        if (!stale.isEmpty()) {
            bookingRepository.saveAll(stale);
            log.info("Released seats of {} expired booking hold(s)", stale.size());
        }
        return stale.size();
    }

    // ---------- helpers ----------

    Booking find(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
    }

    LocalDateTime now() { return LocalDateTime.now(clock); }

    private void release(Booking b, BookingStatus newStatus, LocalDateTime now) {
        b.getLockedSeats().clear();          // orphanRemoval deletes the seat locks
        b.setStatus(newStatus);
        if (newStatus == BookingStatus.CANCELLED) b.setCancelledAt(now);
        if (newStatus == BookingStatus.EXPIRED && b.getPaymentStatus() == PaymentStatus.PENDING) b.setPaymentStatus(PaymentStatus.FAILED);
    }

    boolean canCancel(Booking b, LocalDateTime now, boolean admin) {
        if (b.getStatus() == BookingStatus.PENDING_PAYMENT) return true;
        if (b.getStatus() != BookingStatus.CONFIRMED) return false;
        LocalDateTime limit = admin ? b.getShow().getStartTime() : b.getShow().getStartTime().minusMinutes(cancelBeforeMinutes);
        return now.isBefore(limit);
    }

    private BookingResponse toResponse(Booking b) {
        LocalDateTime now = now();
        return BookingResponse.from(b, now, canCancel(b, now, false));
    }

    static EmailService.Ticket ticket(Booking b) {
        Show s = b.getShow();
        return new EmailService.Ticket(b.getUser().getEmail(), b.getUser().getName(), b.getReference(), b.getId(),
                s.getMovie().getTitle(), s.getTheater().getName(), s.getTheater().getCity(),
                s.getStartTime().format(SHOW_TIME), b.getSeatLabels().replace(",", ", "), b.getAmount().toPlainString());
    }

    private static List<String> normalise(List<String> seats) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : seats) {
            if (s == null || s.isBlank()) continue;
            set.add(s.trim().toUpperCase(Locale.ROOT));
        }
        if (set.isEmpty()) throw new BadRequestException("Select at least one seat");
        return new ArrayList<>(set);
    }

    private String newReference() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("CB");
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            ref = sb.toString();
        } while (bookingRepository.existsByReference(ref));
        return ref;
    }
}

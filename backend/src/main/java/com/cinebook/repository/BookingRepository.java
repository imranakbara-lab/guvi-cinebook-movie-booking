package com.cinebook.repository;

import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByShowIdOrderByCreatedAtDesc(Long showId);
    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime time);
    boolean existsByShowIdAndStatusIn(Long showId, Collection<BookingStatus> statuses);
    boolean existsByShowMovieIdAndStatusIn(Long movieId, Collection<BookingStatus> statuses);
    boolean existsByShowTheaterIdAndStatusIn(Long theaterId, Collection<BookingStatus> statuses);
    boolean existsByReference(String reference);
    void deleteByUserId(Long userId);
    void deleteByShowId(Long showId);
}

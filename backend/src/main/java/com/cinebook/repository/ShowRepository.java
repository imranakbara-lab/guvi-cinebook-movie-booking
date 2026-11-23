package com.cinebook.repository;

import com.cinebook.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    /** Shows of active movies starting in [from, to). Further filters are applied in ShowService. */
    @Query("select s from Show s where s.startTime >= :from and s.startTime < :to and s.movie.active = true order by s.startTime")
    List<Show> findActiveBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Show> findByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime from);

    List<Show> findAllByOrderByStartTimeDesc();

    boolean existsByStartTimeAfter(LocalDateTime time);

    boolean existsByMovieId(Long movieId);

    List<Show> findByMovieId(Long movieId);

    boolean existsByTheaterId(Long theaterId);

    /** Shows on the same screen whose time overlaps [start, end). */
    @Query("select s from Show s where s.theater.id = :theaterId and s.startTime < :end and s.startTime > :earliestStart")
    List<Show> findPossiblyOverlapping(@Param("theaterId") Long theaterId, @Param("earliestStart") LocalDateTime earliestStart,
                                       @Param("end") LocalDateTime end);
}

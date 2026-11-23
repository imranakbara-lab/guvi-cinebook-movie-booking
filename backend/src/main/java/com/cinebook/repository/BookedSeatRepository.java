package com.cinebook.repository;

import com.cinebook.entity.BookedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookedSeatRepository extends JpaRepository<BookedSeat, Long> {

    @Query("select b.seatLabel from BookedSeat b where b.showId = :showId")
    List<String> findSeatLabelsByShowId(@Param("showId") Long showId);

    @Query("select b.seatLabel from BookedSeat b where b.showId = :showId and b.seatLabel in :labels")
    List<String> findTaken(@Param("showId") Long showId, @Param("labels") Collection<String> labels);

    long countByShowId(Long showId);
}

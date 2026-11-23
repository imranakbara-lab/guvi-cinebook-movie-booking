package com.cinebook.repository;

import com.cinebook.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByActiveTrueOrderByReleaseDateDesc();
    List<Movie> findAllByOrderByTitleAsc();

    @Query("select distinct m.genre from Movie m where m.active = true order by m.genre")
    List<String> findGenres();
}

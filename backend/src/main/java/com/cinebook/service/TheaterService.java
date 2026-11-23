package com.cinebook.service;

import com.cinebook.dto.TheaterRequest;
import com.cinebook.dto.TheaterResponse;
import com.cinebook.entity.Theater;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.ShowRepository;
import com.cinebook.repository.TheaterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Theater module: theaters with location and seat layout. */
@Service
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final ShowRepository showRepository;

    public TheaterService(TheaterRepository theaterRepository, ShowRepository showRepository) {
        this.theaterRepository = theaterRepository;
        this.showRepository = showRepository;
    }

    @Transactional(readOnly = true)
    public List<TheaterResponse> all() {
        return theaterRepository.findAllByOrderByCityAscNameAsc().stream().map(TheaterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> cities() {
        return theaterRepository.findAll().stream().map(Theater::getCity).distinct().sorted().toList();
    }

    @Transactional
    public TheaterResponse create(TheaterRequest req) {
        Theater t = new Theater();
        apply(t, req);
        return TheaterResponse.from(theaterRepository.save(t));
    }

    /** The seat layout can only change while the theater has no shows (seat numbers are used by bookings). */
    @Transactional
    public TheaterResponse update(Long id, TheaterRequest req) {
        Theater t = find(id);
        boolean layoutChanged = t.getTotalRows() != req.totalRows() || t.getSeatsPerRow() != req.seatsPerRow();
        if (layoutChanged && showRepository.existsByTheaterId(id)) {
            throw new BadRequestException("Seat layout cannot change while this theater has shows");
        }
        apply(t, req);
        return TheaterResponse.from(theaterRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        Theater t = find(id);
        if (showRepository.existsByTheaterId(id)) throw new BadRequestException("Delete this theater's shows first");
        theaterRepository.delete(t);
    }

    Theater find(Long id) {
        return theaterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theater not found with id " + id));
    }

    private void apply(Theater t, TheaterRequest r) {
        if (r.premiumRows() > r.totalRows()) throw new BadRequestException("Premium rows cannot be more than total rows");
        t.setName(r.name().trim());
        t.setCity(r.city().trim());
        t.setAddress(r.address() == null ? null : r.address().trim());
        t.setTotalRows(r.totalRows());
        t.setSeatsPerRow(r.seatsPerRow());
        t.setPremiumRows(r.premiumRows());
    }
}

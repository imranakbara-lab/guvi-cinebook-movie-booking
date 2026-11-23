package com.cinebook.dto;

import java.util.List;

/** Everything the seat picker needs: layout, which seats are taken and prices. */
public record SeatMapResponse(ShowResponse show, List<String> rows, int seatsPerRow, List<String> premiumRows,
                              List<String> takenSeats) {}

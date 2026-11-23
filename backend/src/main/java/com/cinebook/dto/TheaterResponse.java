package com.cinebook.dto;

import com.cinebook.entity.Theater;

public record TheaterResponse(Long id, String name, String city, String address, int totalRows, int seatsPerRow,
                              int premiumRows, int capacity) {
    public static TheaterResponse from(Theater t) {
        return new TheaterResponse(t.getId(), t.getName(), t.getCity(), t.getAddress(), t.getTotalRows(),
                t.getSeatsPerRow(), t.getPremiumRows(), t.capacity());
    }
}

package com.cinebook.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** A cinema screen. Seats are laid out in rows A, B, C... with numbered seats 1..seatsPerRow. */
@Entity
@Table(name = "theaters")
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    private String address;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int seatsPerRow;

    /** The last N rows are premium seats (charged at the show's premium price). */
    private int premiumRows;

    public static String rowLabel(int index) {
        return index < 26 ? String.valueOf((char) ('A' + index)) : "R" + (index + 1);
    }

    /** All seat labels in this theater, e.g. A1..A10, B1..B10. */
    public List<String> allSeats() {
        List<String> seats = new ArrayList<>();
        for (int r = 0; r < totalRows; r++) {
            for (int s = 1; s <= seatsPerRow; s++) seats.add(rowLabel(r) + s);
        }
        return seats;
    }

    public boolean isValidSeat(String label) {
        return label != null && allSeats().contains(label);
    }

    public boolean isPremium(String label) {
        for (int r = Math.max(0, totalRows - premiumRows); r < totalRows; r++) {
            String row = rowLabel(r);
            if (label.startsWith(row) && label.substring(row.length()).chars().allMatch(Character::isDigit)) return true;
        }
        return false;
    }

    public int capacity() { return totalRows * seatsPerRow; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getSeatsPerRow() { return seatsPerRow; }
    public void setSeatsPerRow(int seatsPerRow) { this.seatsPerRow = seatsPerRow; }
    public int getPremiumRows() { return premiumRows; }
    public void setPremiumRows(int premiumRows) { this.premiumRows = premiumRows; }
}

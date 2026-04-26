package com.mishraachandan.booking_system.dto.pojo;

import java.math.BigDecimal;

/**
 * Generic (label, revenue, bookings) row used by the analytics charts.
 * Label is the grouping dimension — a date string, cinema name, movie title, …
 */
public class RevenuePoint {
    private String label;
    private BigDecimal revenue;
    private long bookings;

    public RevenuePoint() {}

    public RevenuePoint(String label, BigDecimal revenue, long bookings) {
        this.label = label;
        this.revenue = revenue;
        this.bookings = bookings;
    }

    public String getLabel() { return label; }
    public BigDecimal getRevenue() { return revenue; }
    public long getBookings() { return bookings; }
}

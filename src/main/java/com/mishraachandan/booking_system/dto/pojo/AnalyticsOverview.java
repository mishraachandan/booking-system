package com.mishraachandan.booking_system.dto.pojo;

import java.math.BigDecimal;

/**
 * KPI summary for the admin analytics dashboard.
 *
 * All monetary fields are gross (before refunds). The refund rate is expressed
 * as a fraction of {@code totalBookings} (0.0 – 1.0).
 */
public class AnalyticsOverview {
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;
    private BigDecimal averageTicketPrice;
    private double refundRate;

    public AnalyticsOverview() {}

    public AnalyticsOverview(long totalBookings,
                             long confirmedBookings,
                             long cancelledBookings,
                             BigDecimal totalRevenue,
                             BigDecimal averageTicketPrice,
                             double refundRate) {
        this.totalBookings = totalBookings;
        this.confirmedBookings = confirmedBookings;
        this.cancelledBookings = cancelledBookings;
        this.totalRevenue = totalRevenue;
        this.averageTicketPrice = averageTicketPrice;
        this.refundRate = refundRate;
    }

    public long getTotalBookings() { return totalBookings; }
    public long getConfirmedBookings() { return confirmedBookings; }
    public long getCancelledBookings() { return cancelledBookings; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getAverageTicketPrice() { return averageTicketPrice; }
    public double getRefundRate() { return refundRate; }
}

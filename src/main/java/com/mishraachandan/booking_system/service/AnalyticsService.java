package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.pojo.AnalyticsOverview;
import com.mishraachandan.booking_system.dto.pojo.OccupancyEntry;
import com.mishraachandan.booking_system.dto.pojo.RevenuePoint;
import com.mishraachandan.booking_system.dto.pojo.TopShowEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate queries that back the admin analytics dashboard.
 *
 * Every query is read-only and scoped to a [from, to] window (inclusive on
 * both ends). Revenue is derived from BOOKED / confirmed bookings only —
 * payments do not need to be SUCCESS for revenue (payment may still be
 * AWAITING), because the contract of the dashboard is "revenue = sum of
 * seat prices of confirmed bookings + add-on snapshots".
 *
 * Admin-gated via the controller; this service performs no auth checks.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    @PersistenceContext
    private EntityManager em;

    // ─── Overview KPIs ────────────────────────────────────────────────────────

    public AnalyticsOverview overview(LocalDateTime from, LocalDateTime to) {
        // Total / confirmed / cancelled bookings in the window
        Object[] counts = (Object[]) em.createQuery("""
                SELECT
                    COUNT(b),
                    SUM(CASE WHEN b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CONFIRMED
                             THEN 1 ELSE 0 END),
                    SUM(CASE WHEN b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CANCELLED
                             THEN 1 ELSE 0 END)
                FROM Booking b
                WHERE b.createdAt BETWEEN :from AND :to
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        long total = counts[0] == null ? 0L : ((Number) counts[0]).longValue();
        long confirmed = counts[1] == null ? 0L : ((Number) counts[1]).longValue();
        long cancelled = counts[2] == null ? 0L : ((Number) counts[2]).longValue();

        // Gross revenue = sum of seat prices of confirmed bookings + add-on snapshots
        BigDecimal seatRevenue = (BigDecimal) em.createQuery("""
                SELECT COALESCE(SUM(ss.price), 0)
                FROM ShowSeat ss, Booking b
                WHERE ss.bookingId = b.id
                  AND b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CONFIRMED
                  AND b.createdAt BETWEEN :from AND :to
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        BigDecimal addOnRevenue = (BigDecimal) em.createQuery("""
                SELECT COALESCE(SUM(ba.unitPrice * ba.quantity), 0)
                FROM BookingAddOn ba, Booking b
                WHERE ba.bookingId = b.id
                  AND b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CONFIRMED
                  AND b.createdAt BETWEEN :from AND :to
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        BigDecimal totalRevenue = seatRevenue.add(addOnRevenue);
        BigDecimal avgTicket = confirmed == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(confirmed), 2, RoundingMode.HALF_UP);
        double refundRate = total == 0 ? 0.0 : (double) cancelled / (double) total;

        return new AnalyticsOverview(total, confirmed, cancelled, totalRevenue, avgTicket,
                Math.round(refundRate * 10_000.0) / 10_000.0);
    }

    // ─── Revenue: daily ───────────────────────────────────────────────────────

    public List<RevenuePoint> revenueByDay(LocalDate from, LocalDate to) {
        LocalDateTime f = from.atStartOfDay();
        LocalDateTime t = to.atTime(LocalTime.MAX);

        // Seat revenue per day
        List<Object[]> seatRows = em.createQuery("""
                SELECT CAST(b.createdAt AS LocalDate), COALESCE(SUM(ss.price), 0), COUNT(DISTINCT b.id)
                FROM ShowSeat ss, Booking b
                WHERE ss.bookingId = b.id
                  AND b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CONFIRMED
                  AND b.createdAt BETWEEN :from AND :to
                GROUP BY CAST(b.createdAt AS LocalDate)
                """, Object[].class)
                .setParameter("from", f).setParameter("to", t)
                .getResultList();

        // Add-on revenue per day
        List<Object[]> addOnRows = em.createQuery("""
                SELECT CAST(b.createdAt AS LocalDate), COALESCE(SUM(ba.unitPrice * ba.quantity), 0)
                FROM BookingAddOn ba, Booking b
                WHERE ba.bookingId = b.id
                  AND b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CONFIRMED
                  AND b.createdAt BETWEEN :from AND :to
                GROUP BY CAST(b.createdAt AS LocalDate)
                """, Object[].class)
                .setParameter("from", f).setParameter("to", t)
                .getResultList();

        java.util.Map<LocalDate, BigDecimal> seat = new java.util.HashMap<>();
        java.util.Map<LocalDate, Long> bookings = new java.util.HashMap<>();
        for (Object[] r : seatRows) {
            seat.put((LocalDate) r[0], (BigDecimal) r[1]);
            bookings.put((LocalDate) r[0], ((Number) r[2]).longValue());
        }
        java.util.Map<LocalDate, BigDecimal> addOn = new java.util.HashMap<>();
        for (Object[] r : addOnRows) {
            addOn.put((LocalDate) r[0], (BigDecimal) r[1]);
        }

        java.util.TreeSet<LocalDate> allDays = new java.util.TreeSet<>();
        allDays.addAll(seat.keySet());
        allDays.addAll(addOn.keySet());

        List<RevenuePoint> out = new ArrayList<>();
        for (LocalDate d : allDays) {
            BigDecimal rev = seat.getOrDefault(d, BigDecimal.ZERO)
                    .add(addOn.getOrDefault(d, BigDecimal.ZERO));
            out.add(new RevenuePoint(d.toString(), rev, bookings.getOrDefault(d, 0L)));
        }
        return out;
    }

    // ─── Revenue: by cinema ──────────────────────────────────────────────────

    public List<RevenuePoint> revenueByCinema(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = em.createQuery("""
                SELECT c.name,
                       COALESCE(SUM(ss.price), 0),
                       COUNT(DISTINCT b.id)
                FROM ShowSeat ss, Booking b
                JOIN b.show sh
                JOIN sh.screen scr
                JOIN scr.cinema c
                WHERE ss.bookingId = b.id
                  AND b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CONFIRMED
                  AND b.createdAt BETWEEN :from AND :to
                GROUP BY c.name
                ORDER BY SUM(ss.price) DESC
                """, Object[].class)
                .setParameter("from", from).setParameter("to", to)
                .getResultList();

        List<RevenuePoint> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            out.add(new RevenuePoint((String) r[0], (BigDecimal) r[1], ((Number) r[2]).longValue()));
        }
        return out;
    }

    // ─── Revenue: by movie ───────────────────────────────────────────────────

    public List<RevenuePoint> revenueByMovie(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = em.createQuery("""
                SELECT m.title,
                       COALESCE(SUM(ss.price), 0),
                       COUNT(DISTINCT b.id)
                FROM ShowSeat ss, Booking b
                JOIN b.show sh
                JOIN sh.movie m
                WHERE ss.bookingId = b.id
                  AND b.status = com.mishraachandan.booking_system.dto.status.BookingStatus.CONFIRMED
                  AND b.createdAt BETWEEN :from AND :to
                GROUP BY m.title
                ORDER BY SUM(ss.price) DESC
                """, Object[].class)
                .setParameter("from", from).setParameter("to", to)
                .getResultList();

        List<RevenuePoint> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            out.add(new RevenuePoint((String) r[0], (BigDecimal) r[1], ((Number) r[2]).longValue()));
        }
        return out;
    }

    // ─── Top shows ────────────────────────────────────────────────────────────

    public List<TopShowEntry> topShows(LocalDateTime from, LocalDateTime to, int limit) {
        int lim = Math.max(1, Math.min(limit, 50));
        List<Object[]> rows = em.createQuery("""
                SELECT sh.id, m.title, c.name, sh.startTime,
                       COUNT(ss),
                       SUM(CASE WHEN ss.status = com.mishraachandan.booking_system.dto.entity.SeatStatus.BOOKED
                                THEN 1 ELSE 0 END),
                       COALESCE(SUM(CASE WHEN ss.status = com.mishraachandan.booking_system.dto.entity.SeatStatus.BOOKED
                                         THEN ss.price ELSE 0 END), 0)
                FROM ShowSeat ss
                JOIN ss.show sh
                JOIN sh.movie m
                JOIN sh.screen scr
                JOIN scr.cinema c
                WHERE sh.startTime BETWEEN :from AND :to
                GROUP BY sh.id, m.title, c.name, sh.startTime
                ORDER BY SUM(CASE WHEN ss.status = com.mishraachandan.booking_system.dto.entity.SeatStatus.BOOKED
                                  THEN ss.price ELSE 0 END) DESC
                """, Object[].class)
                .setParameter("from", from).setParameter("to", to)
                .setMaxResults(lim)
                .getResultList();

        List<TopShowEntry> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long total = ((Number) r[4]).longValue();
            long booked = ((Number) r[5]).longValue();
            BigDecimal revenue = (BigDecimal) r[6];
            double pct = total == 0 ? 0.0
                    : Math.round(((double) booked / (double) total) * 10_000.0) / 100.0;
            out.add(new TopShowEntry(
                    (Long) r[0],
                    (String) r[1],
                    (String) r[2],
                    (LocalDateTime) r[3],
                    total, booked, revenue, pct));
        }
        return out;
    }

    // ─── Occupancy ────────────────────────────────────────────────────────────

    public List<OccupancyEntry> occupancy(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = em.createQuery("""
                SELECT sh.id, m.title, c.name, scr.name, sh.startTime,
                       COUNT(ss),
                       SUM(CASE WHEN ss.status = com.mishraachandan.booking_system.dto.entity.SeatStatus.BOOKED
                                THEN 1 ELSE 0 END),
                       SUM(CASE WHEN ss.status = com.mishraachandan.booking_system.dto.entity.SeatStatus.LOCKED
                                THEN 1 ELSE 0 END)
                FROM ShowSeat ss
                JOIN ss.show sh
                JOIN sh.movie m
                JOIN sh.screen scr
                JOIN scr.cinema c
                WHERE sh.startTime BETWEEN :from AND :to
                GROUP BY sh.id, m.title, c.name, scr.name, sh.startTime
                ORDER BY sh.startTime
                """, Object[].class)
                .setParameter("from", from).setParameter("to", to)
                .getResultList();

        List<OccupancyEntry> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long total = ((Number) r[5]).longValue();
            long booked = ((Number) r[6]).longValue();
            long locked = ((Number) r[7]).longValue();
            double pct = total == 0 ? 0.0
                    : Math.round(((double) booked / (double) total) * 10_000.0) / 100.0;
            out.add(new OccupancyEntry(
                    (Long) r[0], (String) r[1], (String) r[2], (String) r[3],
                    (LocalDateTime) r[4], total, booked, locked, pct));
        }
        return out;
    }

    /**
     * Defensive: swallow any unexpected exception from an aggregate query and
     * surface it as an empty result + a warning. Keeps the dashboard resilient
     * if one metric breaks (e.g. no bookings in the window) without 500-ing
     * the entire page.
     */
    public <T> List<T> safeList(java.util.function.Supplier<List<T>> supplier, String metric) {
        try {
            return supplier.get();
        } catch (Exception e) {
            logger.warn("analytics metric {} failed: {}", metric, e.getMessage());
            return Collections.emptyList();
        }
    }
}

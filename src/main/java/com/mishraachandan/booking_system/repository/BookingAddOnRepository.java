package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.BookingAddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingAddOnRepository extends JpaRepository<BookingAddOn, Long> {

    List<BookingAddOn> findByBookingId(Long bookingId);

    List<BookingAddOn> findByBookingIdIn(List<Long> bookingIds);

    /** Total add-on amount (unitPrice * quantity) for a single booking. */
    @Query("""
        SELECT COALESCE(SUM(b.unitPrice * b.quantity), 0)
        FROM BookingAddOn b
        WHERE b.bookingId = :bookingId
    """)
    Optional<BigDecimal> findTotalAddOnAmountForBooking(@Param("bookingId") Long bookingId);
}

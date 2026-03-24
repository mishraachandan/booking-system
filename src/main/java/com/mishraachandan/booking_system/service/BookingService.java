package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.entity.BookableResource;
import com.mishraachandan.booking_system.dto.entity.Show;
import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.dto.status.BookingStatus;
import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.pojo.BookingRequest;
import com.mishraachandan.booking_system.dto.pojo.ShowSeatBookingRequest;
import com.mishraachandan.booking_system.repository.BookableResourceRepository;
import com.mishraachandan.booking_system.repository.BookingRepository;
import com.mishraachandan.booking_system.repository.ShowRepository;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import com.mishraachandan.booking_system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookableResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    public BookingService(BookingRepository bookingRepository,
            BookableResourceRepository resourceRepository,
            UserRepository userRepository,
            ShowRepository showRepository,
            ShowSeatRepository showSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
    }

    /**
     * Place a new generic booking for a user (Event-based, non-seated).
     */
    @Transactional
    public Booking placeBooking(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        BookableResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + request.getResourceId()));

        if (resource.getStartTime() != null && resource.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot book: Event has already started");
        }

        List<Booking> existingBookings = bookingRepository.findByResourceId(request.getResourceId());
        int currentBookedTickets = existingBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .mapToInt(Booking::getNumberOfTickets)
                .sum();

        int requestedTickets = request.getQuantity();
        int totalCapacity = resource.getCapacity() != null ? resource.getCapacity() : Integer.MAX_VALUE;

        if (currentBookedTickets + requestedTickets > totalCapacity) {
            throw new IllegalStateException(
                    "Not enough capacity. Available: " + (totalCapacity - currentBookedTickets));
        }

        Booking booking = Booking.builder()
                .user(user)
                .resource(resource)
                .numberOfTickets(requestedTickets)
                .notes(request.getNotes())
                .status(BookingStatus.CONFIRMED)
                .startTime(resource.getStartTime())
                .endTime(resource.getEndTime())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking {} created for user {} on resource {}", savedBooking.getId(), userId,
                request.getResourceId());

        return savedBooking;
    }

    /**
     * Book specific ShowSeats for a show.
     * Validates that seats are locked by the requesting user, marks them BOOKED,
     * and creates a booking in AWAITING_PAYMENT status.
     */
    @Transactional
    public Booking bookShowSeats(Long userId, ShowSeatBookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new IllegalArgumentException("Show not found: " + request.getShowId()));

        if (show.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot book: Show has already started");
        }

        List<ShowSeat> showSeats = showSeatRepository.findAllById(request.getShowSeatIds());
        if (showSeats.size() != request.getShowSeatIds().size()) {
            throw new IllegalArgumentException("Some ShowSeat IDs were not found");
        }

        // Validate each seat: must be LOCKED by this user
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (ShowSeat ss : showSeats) {
            if (ss.getStatus() == SeatStatus.BOOKED) {
                throw new IllegalStateException("Seat " + ss.getSeat().getSeatNumber() + " is already booked");
            }
            if (ss.getStatus() == SeatStatus.LOCKED && !userId.equals(ss.getLockedByUserId())) {
                throw new IllegalStateException("Seat " + ss.getSeat().getSeatNumber() + " is locked by another user");
            }
            if (ss.getStatus() == SeatStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Seat " + ss.getSeat().getSeatNumber() + " must be locked before booking. Please lock it first.");
            }
            totalPrice = totalPrice.add(ss.getPrice());
        }

        // Mark all ShowSeats as BOOKED
        for (ShowSeat ss : showSeats) {
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedAt(null);
            ss.setLockedByUserId(null);
        }
        showSeatRepository.saveAll(showSeats);

        // Create booking in AWAITING_PAYMENT status
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .numberOfTickets(showSeats.size())
                .notes(request.getNotes())
                .status(BookingStatus.AWAITING_PAYMENT)
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking {} created for user {} with {} seats on show {} (total: ₹{}). Status: AWAITING_PAYMENT",
                savedBooking.getId(), userId, showSeats.size(), request.getShowId(), totalPrice);

        return savedBooking;
    }

    /**
     * Confirm a booking after payment is successful.
     */
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Booking is not in AWAITING_PAYMENT status");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmed = bookingRepository.save(booking);
        logger.info("Booking {} confirmed", bookingId);
        return confirmed;
    }

    /**
     * Get all bookings for a user.
     */
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    /**
     * Cancel a booking.
     */
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to cancel this booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        logger.info("Booking {} cancelled by user {}", bookingId, userId);
    }
}

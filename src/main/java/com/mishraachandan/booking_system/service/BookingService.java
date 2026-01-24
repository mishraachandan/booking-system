package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.entity.BookableResource;
import com.mishraachandan.booking_system.dto.entity.Seat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.dto.status.BookingStatus;
import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.pojo.BookingRequest;
import com.mishraachandan.booking_system.dto.pojo.SeatBookingRequest;
import com.mishraachandan.booking_system.repository.BookableResourceRepository;
import com.mishraachandan.booking_system.repository.BookingRepository;
import com.mishraachandan.booking_system.repository.SeatRepository;
import com.mishraachandan.booking_system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookableResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;

    public BookingService(BookingRepository bookingRepository,
            BookableResourceRepository resourceRepository,
            UserRepository userRepository,
            SeatRepository seatRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
    }

    /**
     * Place a new booking for a user.
     */
    @Transactional
    public Booking placeBooking(Long userId, BookingRequest request) {
        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Fetch resource
        BookableResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + request.getResourceId()));

        // Check if event has already started
        if (resource.getStartTime() != null && resource.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot book: Event has already started");
        }

        // Capacity check: Count existing bookings for this resource
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

        // Create booking
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
     * Book specific seats for a user.
     */
    @Transactional
    public Booking bookSeats(Long userId, SeatBookingRequest request) {
        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Fetch resource
        BookableResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + request.getResourceId()));

        // Check if event has already started
        if (resource.getStartTime() != null && resource.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot book: Event has already started");
        }

        // Validate and lock all requested seats
        List<Seat> seatsToBook = new ArrayList<>();
        for (String seatNumber : request.getSeatNumbers()) {
            Seat seat = seatRepository.findByResourceIdAndSeatNumber(request.getResourceId(), seatNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatNumber));

            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new IllegalStateException("Seat " + seatNumber + " is already booked");
            }

            if (seat.getStatus() == SeatStatus.LOCKED && !userId.equals(seat.getLockedByUserId())) {
                throw new IllegalStateException("Seat " + seatNumber + " is locked by another user");
            }

            seatsToBook.add(seat);
        }

        // Mark all seats as BOOKED
        for (Seat seat : seatsToBook) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null);
            seat.setLockedByUserId(null);
            seatRepository.save(seat);
        }

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .resource(resource)
                .numberOfTickets(seatsToBook.size())
                .notes(request.getNotes())
                .status(BookingStatus.CONFIRMED)
                .startTime(resource.getStartTime())
                .endTime(resource.getEndTime())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking {} created for user {} with {} seats on resource {}",
                savedBooking.getId(), userId, seatsToBook.size(), request.getResourceId());

        return savedBooking;
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

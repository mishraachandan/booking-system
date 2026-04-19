package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.entity.BookableResource;
import com.mishraachandan.booking_system.dto.entity.Show;
import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.dto.status.BookingStatus;
import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.pojo.BookingRequest;
import com.mishraachandan.booking_system.dto.pojo.BookingResponse;
import com.mishraachandan.booking_system.dto.pojo.ShowSeatBookingRequest;
import com.mishraachandan.booking_system.repository.BookableResourceRepository;
import com.mishraachandan.booking_system.repository.BookingRepository;
import com.mishraachandan.booking_system.repository.ShowRepository;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import com.mishraachandan.booking_system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    // Auto-cancel bookings that are AWAITING_PAYMENT for more than this many minutes
    private static final int PAYMENT_TIMEOUT_MINUTES = 10;

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

    // ─── Generic Booking ─────────────────────────────────────────────────────────

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

        initializeBookingProxies(savedBooking);
        return savedBooking;
    }

    // ─── Show-Seat Booking ────────────────────────────────────────────────────────

    /**
     * Book specific ShowSeats for a show.
     * Validates that seats are locked by the requesting user, marks them BOOKED,
     * links them to the booking (for later seat-release if payment expires),
     * and creates a booking in AWAITING_PAYMENT status.
     */
    @Transactional
    public Booking bookShowSeats(Long userId, ShowSeatBookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new IllegalArgumentException("Show not found: " + request.getShowId()));

        if (show.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Cannot book: Show has already ended");
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

        // Create the booking first so we have an ID to link to ShowSeats
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

        // Mark all ShowSeats as BOOKED and link to booking (for release on expiry)
        for (ShowSeat ss : showSeats) {
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedAt(null);
            ss.setLockedByUserId(null);
            ss.setBookingId(savedBooking.getId());
        }
        showSeatRepository.saveAll(showSeats);

        logger.info("Booking {} created for user {} with {} seats on show {} (total: ₹{}). Status: AWAITING_PAYMENT",
                savedBooking.getId(), userId, showSeats.size(), request.getShowId(), totalPrice);

        initializeBookingProxies(savedBooking);
        return savedBooking;
    }

    // ─── Confirm Booking ─────────────────────────────────────────────────────────

    /**
     * Confirm a booking after payment is successfully verified.
     * Internal callers (e.g. the payment webhook / signature-verification
     * flow) should use this method directly once they have validated payment.
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
        
        initializeBookingProxies(confirmed);
        return confirmed;
    }

    /**
     * Owner-scoped variant for the HTTP confirm endpoint. Required because the
     * Spring Security filter chain only authorises "has role USER" on
     * /api/bookings/** — without this check, any logged-in user can POST
     * /api/bookings/{someoneElsesBookingId}/confirm and flip another user's
     * booking to CONFIRMED without paying.
     */
    @Transactional
    public Booking confirmBookingForUser(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to confirm this booking");
        }

        return confirmBooking(bookingId);
    }

    // ─── Seat Release ─────────────────────────────────────────────────────────────

    /**
     * Releases all ShowSeats belonging to a booking back to AVAILABLE.
     * Called when payment fails or a booking expires.
     */
    @Transactional
    public void releaseSeatsForBooking(Long bookingId) {
        int released = showSeatRepository.releaseByBookingId(bookingId);
        logger.info("Released {} seats for booking {}", released, bookingId);
    }

    // ─── Auto-Expiry Scheduled Task ───────────────────────────────────────────────

    /**
     * Runs every 5 minutes.
     * Finds bookings stuck in AWAITING_PAYMENT for more than PAYMENT_TIMEOUT_MINUTES,
     * releases their seats, and marks them as EXPIRED.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000) // every 5 minutes
    @Transactional
    public void cancelExpiredPaymentBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.AWAITING_PAYMENT, cutoff);

        if (expiredBookings.isEmpty()) return;

        logger.info("Auto-expiry: found {} bookings to expire", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            releaseSeatsForBooking(booking.getId());
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            logger.info("Booking {} expired and seats released", booking.getId());
        }
    }

    // ─── Queries ──────────────────────────────────────────────────────────────────

    public List<BookingResponse> getUserBookingsFlat(Long userId) {
        return bookingRepository.findBookingResponsesByUserId(userId);
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to cancel this booking");
        }

        // Release seats if booking was in AWAITING_PAYMENT
        if (booking.getStatus() == BookingStatus.AWAITING_PAYMENT) {
            releaseSeatsForBooking(bookingId);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        logger.info("Booking {} cancelled by user {}", bookingId, userId);
    }

    private void initializeBookingProxies(Booking booking) {
        if (booking.getUser() != null) {
            org.hibernate.Hibernate.initialize(booking.getUser());
        }
        if (booking.getShow() != null) {
            org.hibernate.Hibernate.initialize(booking.getShow());
            if (booking.getShow().getMovie() != null) {
                org.hibernate.Hibernate.initialize(booking.getShow().getMovie());
            }
            if (booking.getShow().getScreen() != null) {
                org.hibernate.Hibernate.initialize(booking.getShow().getScreen());
                if (booking.getShow().getScreen().getCinema() != null) {
                    org.hibernate.Hibernate.initialize(booking.getShow().getScreen().getCinema());
                    if (booking.getShow().getScreen().getCinema().getCity() != null) {
                        org.hibernate.Hibernate.initialize(booking.getShow().getScreen().getCinema().getCity());
                    }
                }
            }
        }
        if (booking.getResource() != null) {
            org.hibernate.Hibernate.initialize(booking.getResource());
        }
    }
}

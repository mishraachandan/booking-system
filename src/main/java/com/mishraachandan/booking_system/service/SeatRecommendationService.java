package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.dto.pojo.ShowSeatResponse;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommends the best available seat block for a given show.
 *
 * Scoring (higher = better):
 *   - Center proximity: seats in the middle columns score higher
 *   - Row preference: middle rows score higher (not front-most or back-most)
 *   - Adjacency: contiguous seats score higher (no splits within the group)
 *   - Tier preference: REGULAR > PREMIUM > VIP (for default recommendation)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatRecommendationService {

    private final ShowSeatRepository showSeatRepository;

    /**
     * Returns up to {@code count} recommended ShowSeat IDs for a show.
     * The result is a list of showSeatIds that form the best contiguous block.
     */
    @Transactional(readOnly = true)
    public List<Long> recommend(Long showId, int count) {
        if (count <= 0 || count > 10) {
            throw new IllegalArgumentException("count must be between 1 and 10");
        }

        List<ShowSeat> available = showSeatRepository.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
        if (available.isEmpty()) {
            return Collections.emptyList();
        }

        // Group seats by row (using seat number prefix: "A1" -> "A")
        Map<String, List<ShowSeat>> byRow = available.stream()
                .collect(Collectors.groupingBy(s -> extractRowLetter(s.getSeat().getSeatNumber())));

        // Determine total row count for center-row scoring
        List<String> sortedRows = byRow.keySet().stream().sorted().collect(Collectors.toList());
        int totalRows = sortedRows.size();
        int centerRowIdx = totalRows / 2;

        // Build candidate groups: for each row, find all contiguous blocks of `count` seats
        List<CandidateGroup> candidates = new ArrayList<>();

        for (Map.Entry<String, List<ShowSeat>> entry : byRow.entrySet()) {
            String rowLetter = entry.getKey();
            List<ShowSeat> rowSeats = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(s -> extractSeatIndex(s.getSeat().getSeatNumber())))
                    .collect(Collectors.toList());

            int rowIdx = sortedRows.indexOf(rowLetter);

            // Find contiguous blocks in this row
            List<List<ShowSeat>> blocks = findContiguousBlocks(rowSeats, count);
            for (List<ShowSeat> block : blocks) {
                double score = scoreBlock(block, rowIdx, centerRowIdx, totalRows);
                candidates.add(new CandidateGroup(block, score));
            }
        }

        if (candidates.isEmpty()) {
            // No perfect contiguous block — just return top N individually scored seats
            return available.stream()
                    .sorted(Comparator.comparingDouble(s -> -scoreSingle(s,
                            sortedRows.indexOf(extractRowLetter(s.getSeat().getSeatNumber())),
                            centerRowIdx, totalRows)))
                    .limit(count)
                    .map(ShowSeat::getId)
                    .collect(Collectors.toList());
        }

        // Return the highest-scoring block
        CandidateGroup best = candidates.stream()
                .max(Comparator.comparingDouble(c -> c.score))
                .orElseThrow();

        return best.seats.stream()
                .map(ShowSeat::getId)
                .collect(Collectors.toList());
    }

    /**
     * Returns full ShowSeatResponse DTOs for the recommended seat IDs.
     */
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> recommendAsResponse(Long showId, int count) {
        List<Long> ids = recommend(showId, count);
        if (ids.isEmpty()) return Collections.emptyList();
        Set<Long> idSet = new HashSet<>(ids);
        return showSeatRepository.findShowSeatResponsesByShowId(showId).stream()
                .filter(r -> idSet.contains(r.getShowSeatId()))
                .collect(Collectors.toList());
    }

    // ─── Block Finder ─────────────────────────────────────────────────────────

    private List<List<ShowSeat>> findContiguousBlocks(List<ShowSeat> rowSeats, int size) {
        List<List<ShowSeat>> blocks = new ArrayList<>();
        for (int i = 0; i <= rowSeats.size() - size; i++) {
            List<ShowSeat> block = rowSeats.subList(i, i + size);
            if (isContiguous(block)) {
                blocks.add(new ArrayList<>(block));
            }
        }
        return blocks;
    }

    private boolean isContiguous(List<ShowSeat> seats) {
        if (seats.size() <= 1) return true;
        for (int i = 1; i < seats.size(); i++) {
            int prev = extractSeatIndex(seats.get(i - 1).getSeat().getSeatNumber());
            int curr = extractSeatIndex(seats.get(i).getSeat().getSeatNumber());
            if (curr != prev + 1) return false;
        }
        return true;
    }

    // ─── Scoring ──────────────────────────────────────────────────────────────

    private double scoreBlock(List<ShowSeat> block, int rowIdx, int centerRowIdx, int totalRows) {
        double rowScore = rowProximityScore(rowIdx, centerRowIdx, totalRows);
        double colScore = block.stream()
                .mapToDouble(s -> columnCenterScore(extractSeatIndex(s.getSeat().getSeatNumber()), 10))
                .average().orElse(0.5);
        double tierScore = tierScore(block.get(0).getSeat().getSeatType().name());
        // Adjacency bonus: contiguous blocks always get a full bonus
        double adjacencyBonus = 0.2;
        return rowScore * 0.4 + colScore * 0.3 + tierScore * 0.1 + adjacencyBonus;
    }

    private double scoreSingle(ShowSeat s, int rowIdx, int centerRowIdx, int totalRows) {
        double rowScore = rowProximityScore(rowIdx, centerRowIdx, totalRows);
        double colScore = columnCenterScore(extractSeatIndex(s.getSeat().getSeatNumber()), 10);
        double tierScore = tierScore(s.getSeat().getSeatType().name());
        return rowScore * 0.4 + colScore * 0.3 + tierScore * 0.1;
    }

    /** Score how close a row is to the center (0 = edge, 1 = dead center) */
    private double rowProximityScore(int rowIdx, int centerRowIdx, int totalRows) {
        if (totalRows <= 1) return 1.0;
        double distance = Math.abs(rowIdx - centerRowIdx);
        return 1.0 - (distance / (double) centerRowIdx);
    }

    /** Score column center proximity (estimating 10 cols per row) */
    private double columnCenterScore(int colIdx, int totalCols) {
        double center = totalCols / 2.0;
        double distance = Math.abs(colIdx - center);
        return 1.0 - (distance / center);
    }

    /** REGULAR = 0.8 (value), PREMIUM = 0.5 (middle), VIP = 0.2 (expensive) */
    private double tierScore(String seatType) {
        if (seatType == null) return 0.5;
        return switch (seatType.toUpperCase()) {
            case "REGULAR" -> 0.8;
            case "PREMIUM" -> 0.5;
            case "VIP"     -> 0.2;
            default        -> 0.5;
        };
    }

    // ─── Seat Number Parsing ──────────────────────────────────────────────────

    private String extractRowLetter(String seatNumber) {
        if (seatNumber == null || seatNumber.isEmpty()) return "A";
        StringBuilder letters = new StringBuilder();
        for (char c : seatNumber.toCharArray()) {
            if (Character.isLetter(c)) letters.append(c);
            else break;
        }
        return letters.length() > 0 ? letters.toString() : "A";
    }

    private int extractSeatIndex(String seatNumber) {
        if (seatNumber == null) return 0;
        StringBuilder digits = new StringBuilder();
        for (char c : seatNumber.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
        }
        try {
            return digits.length() > 0 ? Integer.parseInt(digits.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ─── Internal DTO ─────────────────────────────────────────────────────────

    private record CandidateGroup(List<ShowSeat> seats, double score) {}
}

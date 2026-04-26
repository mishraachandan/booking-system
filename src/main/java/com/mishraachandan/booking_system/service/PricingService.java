package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.PricingRule;
import com.mishraachandan.booking_system.dto.pojo.PriceBreakdown;
import com.mishraachandan.booking_system.repository.PricingRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies table-driven dynamic pricing rules on top of a base seat price.
 *
 * Safety invariants:
 *   1. If {@code pricing.dynamic.enabled=false} (default), {@link #resolve(BigDecimal, LocalDateTime, LocalDateTime)}
 *      returns a pass-through — effective == base. The booking flow is therefore
 *      byte-identical to the pre-dynamic-pricing code path.
 *   2. If no rules are active, the resolver also returns a pass-through.
 *   3. Individual rule matching failures are swallowed and logged — a single
 *      bad rule cannot break the booking flow.
 */
@Service
public class PricingService {

    private static final Logger logger = LoggerFactory.getLogger(PricingService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PricingRuleRepository pricingRuleRepository;

    @Value("${pricing.dynamic.enabled:false}")
    private boolean enabled;

    public PricingService(PricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Resolves effective price for a seat whose base price is {@code base} on a
     * show that starts at {@code showStart}, evaluated at {@code at}.
     */
    public PriceBreakdown resolve(BigDecimal base, LocalDateTime showStart, LocalDateTime at) {
        if (base == null) {
            return PriceBreakdown.passthrough(ZERO);
        }
        if (!enabled) {
            return PriceBreakdown.passthrough(base);
        }
        List<PricingRule> rules;
        try {
            rules = pricingRuleRepository.findByActiveTrueOrderByPriorityAsc();
        } catch (Exception e) {
            logger.warn("pricing-rule lookup failed; falling back to base price: {}", e.getMessage());
            return PriceBreakdown.passthrough(base);
        }
        if (rules == null || rules.isEmpty()) {
            return PriceBreakdown.passthrough(base);
        }

        BigDecimal multiplier = BigDecimal.ONE;
        List<String> applied = new ArrayList<>();
        for (PricingRule r : rules) {
            try {
                if (matches(r, showStart, at)) {
                    BigDecimal m = r.getMultiplier();
                    if (m == null) {
                        continue;
                    }
                    multiplier = multiplier.multiply(m);
                    applied.add(r.getName());
                }
            } catch (Exception e) {
                logger.warn("rule {} evaluation failed, skipping: {}", r.getId(), e.getMessage());
            }
        }

        BigDecimal effective = base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        return new PriceBreakdown(base, effective, applied);
    }

    private boolean matches(PricingRule r, LocalDateTime showStart, LocalDateTime at) {
        if (showStart == null || at == null) {
            return false;
        }
        // Day-of-week filter (on show's local day)
        if (r.getDaysOfWeek() != null && !r.getDaysOfWeek().isBlank()) {
            Set<DayOfWeek> allowed = parseDays(r.getDaysOfWeek());
            if (!allowed.isEmpty() && !allowed.contains(showStart.getDayOfWeek())) {
                return false;
            }
        }
        // Hour-of-show filter (inclusive bounds)
        if (r.getStartHour() != null || r.getEndHour() != null) {
            int hour = showStart.getHour();
            int lo = r.getStartHour() == null ? 0 : r.getStartHour();
            int hi = r.getEndHour() == null ? 23 : r.getEndHour();
            if (lo <= hi) {
                if (hour < lo || hour > hi) {
                    return false;
                }
            } else {
                // wrap-around (e.g. 22→2) — accept hour ≥ lo OR hour ≤ hi
                if (!(hour >= lo || hour <= hi)) {
                    return false;
                }
            }
        }
        // Lead-time filter
        if (r.getMinLeadTimeHours() != null || r.getMaxLeadTimeHours() != null) {
            long leadHours = Math.max(0L, Duration.between(at, showStart).toHours());
            if (r.getMinLeadTimeHours() != null && leadHours < r.getMinLeadTimeHours()) {
                return false;
            }
            if (r.getMaxLeadTimeHours() != null && leadHours > r.getMaxLeadTimeHours()) {
                return false;
            }
        }
        return true;
    }

    private static Set<DayOfWeek> parseDays(String csv) {
        Set<DayOfWeek> out = new HashSet<>();
        for (String tok : Arrays.asList(csv.split(","))) {
            switch (tok.trim().toUpperCase()) {
                case "MON" -> out.add(DayOfWeek.MONDAY);
                case "TUE" -> out.add(DayOfWeek.TUESDAY);
                case "WED" -> out.add(DayOfWeek.WEDNESDAY);
                case "THU" -> out.add(DayOfWeek.THURSDAY);
                case "FRI" -> out.add(DayOfWeek.FRIDAY);
                case "SAT" -> out.add(DayOfWeek.SATURDAY);
                case "SUN" -> out.add(DayOfWeek.SUNDAY);
                default -> { /* ignore unknown tokens */ }
            }
        }
        return out;
    }
}

package com.mishraachandan.booking_system.dto.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Table-driven dynamic-pricing rule. Applied multiplicatively on top of
 * the base {@link ShowSeat#getPrice() ShowSeat.price} when the rule's
 * conditions match.
 *
 * All condition fields are nullable: a null field means "don't filter on this
 * dimension". Example rules:
 *   • weekend surge       → daysOfWeek="SAT,SUN",            multiplier=1.20
 *   • evening prime-time  → startHour=18, endHour=22,        multiplier=1.15
 *   • early-bird discount → minLeadTimeHours=48,             multiplier=0.80
 *   • late-night          → startHour=22, endHour=23,        multiplier=0.85
 *
 * The feature is gated behind the {@code pricing.dynamic.enabled} application
 * property. When the flag is off OR there are no active rules, the pricing
 * pipeline is a pass-through (effective = base). This guarantees no regression
 * for existing booking flows.
 */
@Entity
@Table(name = "pricing_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    /** If false, the rule is skipped at resolution time. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    /** Rules are applied in ascending priority order (stable). */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    /**
     * CSV of uppercase day names this rule matches. Null = any day.
     * Accepted tokens: MON,TUE,WED,THU,FRI,SAT,SUN.
     */
    @Column(name = "days_of_week", length = 64)
    private String daysOfWeek;

    /** Inclusive show-time hour bounds, 0-23. Null = any hour. */
    @Column(name = "start_hour")
    private Integer startHour;

    @Column(name = "end_hour")
    private Integer endHour;

    /**
     * Lead time = (show.startTime - now). If non-null, the rule only applies
     * when the caller's lead time is ≥ minLeadTimeHours AND ≤ maxLeadTimeHours.
     */
    @Column(name = "min_lead_time_hours")
    private Integer minLeadTimeHours;

    @Column(name = "max_lead_time_hours")
    private Integer maxLeadTimeHours;

    /**
     * Multiplicative adjustment: effective = base × multiplier.
     * 1.00 = no change, 1.20 = surge +20%, 0.80 = discount -20%.
     */
    @Column(nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal multiplier = BigDecimal.ONE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

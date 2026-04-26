package com.mishraachandan.booking_system.dto.pojo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of applying dynamic pricing rules to a base price.
 *
 * {@code base} is the original {@code ShowSeat.price}; {@code effective} is
 * what the user is actually charged. {@code appliedRules} is the ordered list
 * of rule names that contributed to the effective price (empty when pricing
 * was a pass-through).
 */
public class PriceBreakdown {
    private BigDecimal base;
    private BigDecimal effective;
    private List<String> appliedRules;

    public PriceBreakdown() {
        this.appliedRules = new ArrayList<>();
    }

    public PriceBreakdown(BigDecimal base, BigDecimal effective, List<String> appliedRules) {
        this.base = base;
        this.effective = effective;
        this.appliedRules = appliedRules == null ? Collections.emptyList() : appliedRules;
    }

    public static PriceBreakdown passthrough(BigDecimal base) {
        return new PriceBreakdown(base, base, Collections.emptyList());
    }

    public BigDecimal getBase() { return base; }
    public BigDecimal getEffective() { return effective; }
    public List<String> getAppliedRules() { return appliedRules; }

    public void setBase(BigDecimal base) { this.base = base; }
    public void setEffective(BigDecimal effective) { this.effective = effective; }
    public void setAppliedRules(List<String> appliedRules) { this.appliedRules = appliedRules; }
}

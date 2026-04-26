package com.mishraachandan.booking_system.dto.pojo;

import com.mishraachandan.booking_system.dto.entity.PricingRule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PricingRuleResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private Integer priority;
    private String daysOfWeek;
    private Integer startHour;
    private Integer endHour;
    private Integer minLeadTimeHours;
    private Integer maxLeadTimeHours;
    private BigDecimal multiplier;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PricingRuleResponse fromEntity(PricingRule r) {
        PricingRuleResponse out = new PricingRuleResponse();
        out.id = r.getId();
        out.name = r.getName();
        out.description = r.getDescription();
        out.active = r.getActive();
        out.priority = r.getPriority();
        out.daysOfWeek = r.getDaysOfWeek();
        out.startHour = r.getStartHour();
        out.endHour = r.getEndHour();
        out.minLeadTimeHours = r.getMinLeadTimeHours();
        out.maxLeadTimeHours = r.getMaxLeadTimeHours();
        out.multiplier = r.getMultiplier();
        out.createdAt = r.getCreatedAt();
        out.updatedAt = r.getUpdatedAt();
        return out;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Boolean getActive() { return active; }
    public Integer getPriority() { return priority; }
    public String getDaysOfWeek() { return daysOfWeek; }
    public Integer getStartHour() { return startHour; }
    public Integer getEndHour() { return endHour; }
    public Integer getMinLeadTimeHours() { return minLeadTimeHours; }
    public Integer getMaxLeadTimeHours() { return maxLeadTimeHours; }
    public BigDecimal getMultiplier() { return multiplier; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

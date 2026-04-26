package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Admin-facing create/update DTO for {@link com.mishraachandan.booking_system.dto.entity.PricingRule}.
 */
public class PricingRuleRequest {

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 512)
    private String description;

    private Boolean active;

    @Min(0)
    @Max(10_000)
    private Integer priority;

    /** CSV of MON/TUE/…/SUN. Null or blank = any day. */
    @Pattern(regexp = "^\\s*$|^(MON|TUE|WED|THU|FRI|SAT|SUN)(\\s*,\\s*(MON|TUE|WED|THU|FRI|SAT|SUN))*$",
            message = "daysOfWeek must be a CSV of MON,TUE,WED,THU,FRI,SAT,SUN")
    private String daysOfWeek;

    @Min(0) @Max(23)
    private Integer startHour;

    @Min(0) @Max(23)
    private Integer endHour;

    @Min(0) @Max(10_000)
    private Integer minLeadTimeHours;

    @Min(0) @Max(10_000)
    private Integer maxLeadTimeHours;

    @DecimalMin(value = "0.100", message = "multiplier must be ≥ 0.100")
    @DecimalMax(value = "5.000", message = "multiplier must be ≤ 5.000")
    private BigDecimal multiplier;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public Integer getStartHour() { return startHour; }
    public void setStartHour(Integer startHour) { this.startHour = startHour; }

    public Integer getEndHour() { return endHour; }
    public void setEndHour(Integer endHour) { this.endHour = endHour; }

    public Integer getMinLeadTimeHours() { return minLeadTimeHours; }
    public void setMinLeadTimeHours(Integer minLeadTimeHours) { this.minLeadTimeHours = minLeadTimeHours; }

    public Integer getMaxLeadTimeHours() { return maxLeadTimeHours; }
    public void setMaxLeadTimeHours(Integer maxLeadTimeHours) { this.maxLeadTimeHours = maxLeadTimeHours; }

    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
}

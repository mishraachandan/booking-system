package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.entity.PricingRule;
import com.mishraachandan.booking_system.dto.pojo.PriceBreakdown;
import com.mishraachandan.booking_system.dto.pojo.PricingRuleRequest;
import com.mishraachandan.booking_system.dto.pojo.PricingRuleResponse;
import com.mishraachandan.booking_system.repository.PricingRuleRepository;
import com.mishraachandan.booking_system.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for dynamic pricing rules. Admin-only via {@code /api/v1/admin/**}
 * security matcher. A read-only "preview" endpoint is also exposed here so the
 * admin UI can show what a rule does against a base price without having to
 * create a booking.
 */
@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
public class PricingRuleController {

    private final PricingRuleRepository pricingRuleRepository;
    private final PricingService pricingService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("enabled", pricingService.isEnabled()));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<PricingRuleResponse>> list() {
        return ResponseEntity.ok(
                pricingRuleRepository.findAll().stream()
                        .map(PricingRuleResponse::fromEntity)
                        .toList());
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<PricingRuleResponse> get(@PathVariable Long id) {
        return pricingRuleRepository.findById(id)
                .map(PricingRuleResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/rules")
    public ResponseEntity<PricingRuleResponse> create(@Valid @RequestBody PricingRuleRequest req) {
        PricingRule rule = PricingRule.builder()
                .name(req.getName())
                .description(req.getDescription())
                .active(req.getActive() == null ? Boolean.TRUE : req.getActive())
                .priority(req.getPriority() == null ? 100 : req.getPriority())
                .daysOfWeek(blankToNull(req.getDaysOfWeek()))
                .startHour(req.getStartHour())
                .endHour(req.getEndHour())
                .minLeadTimeHours(req.getMinLeadTimeHours())
                .maxLeadTimeHours(req.getMaxLeadTimeHours())
                .multiplier(req.getMultiplier() == null ? BigDecimal.ONE : req.getMultiplier())
                .build();
        PricingRule saved = pricingRuleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(PricingRuleResponse.fromEntity(saved));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<PricingRuleResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody PricingRuleRequest req) {
        return pricingRuleRepository.findById(id).map(rule -> {
            if (req.getName() != null) rule.setName(req.getName());
            rule.setDescription(req.getDescription());
            if (req.getActive() != null) rule.setActive(req.getActive());
            if (req.getPriority() != null) rule.setPriority(req.getPriority());
            rule.setDaysOfWeek(blankToNull(req.getDaysOfWeek()));
            rule.setStartHour(req.getStartHour());
            rule.setEndHour(req.getEndHour());
            rule.setMinLeadTimeHours(req.getMinLeadTimeHours());
            rule.setMaxLeadTimeHours(req.getMaxLeadTimeHours());
            if (req.getMultiplier() != null) rule.setMultiplier(req.getMultiplier());
            PricingRule saved = pricingRuleRepository.save(rule);
            return ResponseEntity.ok(PricingRuleResponse.fromEntity(saved));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!pricingRuleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        pricingRuleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Compute effective price for {@code basePrice} if the show started at
     * {@code showStart}, evaluated at {@code at} (defaults to now). Useful for
     * previewing a rule's effect in the admin UI.
     */
    @GetMapping("/preview")
    public ResponseEntity<PriceBreakdown> preview(
            @RequestParam BigDecimal basePrice,
            @RequestParam LocalDateTime showStart,
            @RequestParam(required = false) LocalDateTime at) {
        return ResponseEntity.ok(
                pricingService.resolve(basePrice, showStart, at == null ? LocalDateTime.now() : at));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

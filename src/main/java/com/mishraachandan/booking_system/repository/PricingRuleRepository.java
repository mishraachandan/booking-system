package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findByActiveTrueOrderByPriorityAsc();
}

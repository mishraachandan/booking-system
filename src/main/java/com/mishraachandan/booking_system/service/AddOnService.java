package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.AddOn;
import com.mishraachandan.booking_system.dto.pojo.AddOnResponse;
import com.mishraachandan.booking_system.repository.AddOnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddOnService {

    private final AddOnRepository addOnRepository;

    public AddOnService(AddOnRepository addOnRepository) {
        this.addOnRepository = addOnRepository;
    }

    @Transactional(readOnly = true)
    public List<AddOnResponse> getAvailableAddOns() {
        return addOnRepository.findByAvailableTrueOrderByCategoryAscPriceAsc()
                .stream()
                .map(AddOnResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddOn requireAvailable(Long id) {
        AddOn a = addOnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Add-on not found: " + id));
        if (!Boolean.TRUE.equals(a.getAvailable())) {
            throw new IllegalStateException("Add-on is not available: " + id);
        }
        return a;
    }
}

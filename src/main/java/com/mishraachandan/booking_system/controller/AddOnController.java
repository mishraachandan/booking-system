package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.pojo.AddOnResponse;
import com.mishraachandan.booking_system.service.AddOnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addons")
@RequiredArgsConstructor
public class AddOnController {

    private final AddOnService addOnService;

    /**
     * Public catalogue of food/beverage/combo add-ons currently available.
     */
    @GetMapping
    public ResponseEntity<List<AddOnResponse>> getAvailableAddOns() {
        return ResponseEntity.ok(addOnService.getAvailableAddOns());
    }
}

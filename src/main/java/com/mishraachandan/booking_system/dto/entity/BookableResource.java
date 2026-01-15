package com.mishraachandan.booking_system.dto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.mishraachandan.booking_system.dto.status.ResourceType;

@Entity
@Table(name = "bookable_resources")
public class BookableResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long id;  // PRIMARY KEY
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private ResourceType type;
    
    private String description;
    
    private Integer capacity = 1;
    
    private String location;
    
    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL)
    private List<Booking> bookings = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
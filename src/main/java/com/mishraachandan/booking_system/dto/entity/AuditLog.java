package com.mishraachandan.booking_system.dto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    private String action;           // e.g. BOOKING_CONFIRMED, SEAT_LOCKED, PAYMENT_SUCCESS
    private String entityType;       // e.g. Booking, ShowSeat
    private Long entityId;
    private Long userId;
    private String userEmail;
    private String ipAddress;
    
    @Column(columnDefinition="TEXT")
    private String details;          // JSON string of relevant data
    
    private String result;           // SUCCESS or FAILURE
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}

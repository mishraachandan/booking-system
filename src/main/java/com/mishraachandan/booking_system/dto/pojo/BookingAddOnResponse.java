package com.mishraachandan.booking_system.dto.pojo;

import com.mishraachandan.booking_system.dto.entity.BookingAddOn;

import java.math.BigDecimal;

public class BookingAddOnResponse {

    private Long id;
    private Long addOnId;
    private String name;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;

    public BookingAddOnResponse() {}

    public static BookingAddOnResponse fromEntity(BookingAddOn entity) {
        BookingAddOnResponse r = new BookingAddOnResponse();
        r.id = entity.getId();
        r.addOnId = entity.getAddOnId();
        r.name = entity.getName();
        r.unitPrice = entity.getUnitPrice();
        r.quantity = entity.getQuantity();
        r.lineTotal = entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity()));
        return r;
    }

    public Long getId() { return id; }
    public Long getAddOnId() { return addOnId; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
}

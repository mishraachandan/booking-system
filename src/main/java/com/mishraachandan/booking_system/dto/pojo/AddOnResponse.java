package com.mishraachandan.booking_system.dto.pojo;

import com.mishraachandan.booking_system.dto.entity.AddOn;

import java.math.BigDecimal;

public class AddOnResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String imageUrl;

    public AddOnResponse() {}

    public static AddOnResponse fromEntity(AddOn addOn) {
        AddOnResponse r = new AddOnResponse();
        r.id = addOn.getId();
        r.name = addOn.getName();
        r.description = addOn.getDescription();
        r.category = addOn.getCategory() != null ? addOn.getCategory().name() : null;
        r.price = addOn.getPrice();
        r.imageUrl = addOn.getImageUrl();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
}

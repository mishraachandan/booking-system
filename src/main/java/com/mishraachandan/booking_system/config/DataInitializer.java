package com.mishraachandan.booking_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.mishraachandan.booking_system.dto.entity.Category;
import com.mishraachandan.booking_system.dto.entity.BookableResource;
import com.mishraachandan.booking_system.dto.status.ResourceType;
import com.mishraachandan.booking_system.repository.CategoryRepository;
import com.mishraachandan.booking_system.repository.BookableResourceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookableResourceRepository resourceRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed categories
        Category movies = createCategoryIfNotExists("Movies",
                "Book tickets for the latest movies in theaters near you.");
        Category stream = createCategoryIfNotExists("Stream", "Live streaming events and online experiences.");
        Category music = createCategoryIfNotExists("Music Shows",
                "Concerts, live music performances, and music festivals.");
        Category comedy = createCategoryIfNotExists("Comedy Shows",
                "Stand-up comedy, improv shows, and comedy festivals.");
        Category plays = createCategoryIfNotExists("Plays", "Theater productions, musicals, and drama performances.");
        Category sports = createCategoryIfNotExists("Sports", "Live sports events, matches, and tournaments.");

        // Seed resources/events
        seedEvents(movies, stream, music, comedy, plays, sports);
    }

    private Category createCategoryIfNotExists(String name, String description) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category category = new Category(name, description);
            return categoryRepository.save(category);
        });
    }

    private void seedEvents(Category movies, Category stream, Category music, Category comedy, Category plays,
            Category sports) {
        // Movies
        createEventIfNotExists("Avatar: The Way of Water", "Epic science fiction film directed by James Cameron.",
                "PVR Directors Cut, Ambience Mall", new BigDecimal("450.00"), ResourceType.MOVIE, movies);
        createEventIfNotExists("Oppenheimer", "The story of American scientist J. Robert Oppenheimer.",
                "IMAX Select Citywalk", new BigDecimal("600.00"), ResourceType.MOVIE, movies);

        // Music
        createEventIfNotExists("Coldplay: Music of the Spheres", "Global tour of the iconic British band.",
                "DY Patil Stadium, Mumbai", new BigDecimal("2500.00"), ResourceType.EVENT, music);

        // Comedy
        createEventIfNotExists("Zakir Khan - Tathastu", "A beautiful storytelling journey by the Sakht Launda.",
                "Siri Fort Auditorium, Delhi", new BigDecimal("999.00"), ResourceType.EVENT, comedy);

        // Sports
        createEventIfNotExists("IPL: MI vs CSK", "The El Clasico of IPL.",
                "Wankhede Stadium, Mumbai", new BigDecimal("1500.00"), ResourceType.EVENT, sports);
    }

    private void createEventIfNotExists(String name, String description, String location, BigDecimal price,
            ResourceType type, Category category) {
        if (!resourceRepository.existsByName(name)) {
            BookableResource resource = new BookableResource();
            resource.setName(name);
            resource.setDescription(description);
            resource.setLocation(location);
            resource.setPrice(price);
            resource.setType(type);
            resource.setCategory(category);
            resource.setStartTime(LocalDateTime.now().plusDays(7));
            resource.setEndTime(LocalDateTime.now().plusDays(7).plusHours(3));
            resourceRepository.save(resource);
        }
    }
}

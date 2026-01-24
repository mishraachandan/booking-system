package com.mishraachandan.booking_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mishraachandan.booking_system.dto.entity.BookableResource;
import com.mishraachandan.booking_system.dto.entity.Category;

import java.util.List;

@Repository
public interface BookableResourceRepository extends JpaRepository<BookableResource, Long> {
    List<BookableResource> findByCategory(Category category);

    java.util.Optional<BookableResource> findByName(String name);

    boolean existsByName(String name);
}

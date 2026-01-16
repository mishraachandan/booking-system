package com.mishraachandan.booking_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mishraachandan.booking_system.dto.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

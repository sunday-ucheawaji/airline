package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.Passenger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, UUID> {

    List<Passenger> findByPrimaryUserId(UUID primaryUserId);

    Optional<Passenger> findByEmailAndPhoneAndDateOfBirth(
            String email, String phone, LocalDate dateOfBirth);

    Optional<Passenger> findByPassportNumber(String passportNumber);

    List<Passenger> findByEmail(String email);

    List<Passenger> findByLastNameContainingIgnoreCase(String lastName);

    @Query("SELECT p FROM Passenger p WHERE " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Passenger> findByNameContaining(@Param("name") String name);

    long countByPrimaryUserId(UUID primaryUserId);

    Page<Passenger> findByPrimaryUserId(UUID primaryUserId, Pageable pageable);

    @Query("SELECT p FROM Passenger p WHERE p.isActive = true")
    List<Passenger> findAllActive();
}

package com.sunday.services.repository;

import com.sunday.services.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    boolean existsByTripReference(String tripReference);
}

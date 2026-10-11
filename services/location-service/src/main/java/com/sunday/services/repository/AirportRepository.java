package com.sunday.services.repository;

import com.sunday.services.model.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, UUID> {

    Optional<Airport> findByIataCode(String iataCode);

    List<Airport> findByCityId(UUID cityId);

    boolean existsByCityId(UUID cityId);
}

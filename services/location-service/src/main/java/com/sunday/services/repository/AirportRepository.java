package com.sunday.services.repository;

import com.sunday.services.model.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long> {

    Optional<Airport> findByIataCode(String iataCode);

    List<Airport> findByCityId(Long cityId);
}

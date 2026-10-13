package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.services.model.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirlineRepository extends JpaRepository<Airline, UUID> {

    Optional<Airline> findByIataCode(String code);

    Optional<Airline> findByIcaoCode(String code);

    boolean existsByIataCode(String code);

    boolean existsByIcaoCode(String code);

    List<Airline> findByStatus(AirlineStatus status);
}

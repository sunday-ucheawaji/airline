package com.sunday.services.repository;

import com.sunday.services.model.Airport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, UUID> {

    Optional<Airport> findByIataCode(String iataCode);

    @EntityGraph(attributePaths = "city")
    List<Airport> findByCityId(UUID cityId);

    boolean existsByCityId(UUID cityId);

    @Query("SELECT a FROM Airport a JOIN FETCH a.city")
    Page<Airport> findAllWithCity(Pageable pageable);

    @Query("SELECT a FROM Airport a JOIN FETCH a.city WHERE a.id IN :ids")
    List<Airport> findAllWithCityByIdIn(List<UUID> ids);
}

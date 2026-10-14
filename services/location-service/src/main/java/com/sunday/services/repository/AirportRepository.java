package com.sunday.services.repository;

import com.sunday.services.model.Airport;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, UUID> {

    Optional<Airport> findByIataCode(String iataCode);

    List<Airport> findByIataCodeIn(Collection<String> iataCodes);

    @EntityGraph(attributePaths = "city")
    List<Airport> findByCityId(UUID cityId);

    boolean existsByCityId(UUID cityId);

    @Query("SELECT a FROM Airport a JOIN FETCH a.city")
    Page<Airport> findAllWithCity(Pageable pageable);

    @Query("SELECT a FROM Airport a JOIN FETCH a.city WHERE a.id IN :ids")
    List<Airport> findAllWithCityByIdIn(List<UUID> ids);

    @Query("""
            SELECT a FROM Airport a JOIN FETCH a.city c
            WHERE LOWER(a.iataCode) = :query
               OR LOWER(c.cityCode) = :query
               OR LOWER(c.name) LIKE CONCAT(:query, '%')
               OR LOWER(c.countryName) LIKE CONCAT(:query, '%')
               OR LOWER(a.name) LIKE CONCAT('%', :query, '%')
            ORDER BY
               CASE
                   WHEN LOWER(a.iataCode) = :query THEN 0
                   WHEN LOWER(c.cityCode) = :query THEN 1
                   WHEN LOWER(c.name) LIKE CONCAT(:query, '%') THEN 2
                   WHEN LOWER(a.name) LIKE CONCAT(:query, '%') THEN 3
                   ELSE 4
               END,
               a.analytics.annualPassengers DESC,
               a.iataCode
            """)
    List<Airport> search(String query, Limit limit);
}

package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.Ancillary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AncillaryRepository extends JpaRepository<Ancillary, UUID> {

    List<Ancillary> findByAirlineId(UUID airlineId);
}

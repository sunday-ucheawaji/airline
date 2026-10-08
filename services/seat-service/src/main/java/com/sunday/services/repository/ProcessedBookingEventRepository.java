package com.sunday.services.repository;

import com.sunday.services.model.ProcessedBookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedBookingEventRepository extends JpaRepository<ProcessedBookingEvent, Long> {
}

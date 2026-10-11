package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AirportRequest;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.util.CacheUtil;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.mapper.AirportMapper;
import com.sunday.services.model.Airport;
import com.sunday.services.model.City;
import com.sunday.services.repository.AirportRepository;
import com.sunday.services.repository.CityRepository;
import com.sunday.services.service.AirportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirportServiceImpl implements AirportService {

    private final AirportRepository airportRepository;
    private final CityRepository cityRepository;

    @Override
    @Transactional
    public AirportResponse createAirport(AirportRequest request){
        if (airportRepository.findByIataCode(request.getIataCode()).isPresent()) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRPORT_IATA_CODE_ALREADY_EXISTS, request.getIataCode()));
        }

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, request.getCityId())));

        Airport airport = AirportMapper.toEntity(request);
        airport.setCity(city);

        Airport savedAirport = airportRepository.save(airport);
        return AirportMapper.toResponse(savedAirport);
    }

    @Override
    @Transactional
    public List<AirportResponse> createBulkAirports(List<AirportRequest> requests) {
        List<AirportResponse> createdAirports = new ArrayList<>();
        List<String> skippedCodes = new ArrayList<>();

        for (AirportRequest request : requests) {
            if (airportRepository.findByIataCode(request.getIataCode()).isPresent()) {
                skippedCodes.add(request.getIataCode() + " (already exists)");
                continue;
            }

            Optional<City> cityOpt = cityRepository.findById(request.getCityId());
            if (cityOpt.isEmpty()) {
                skippedCodes.add(request.getIataCode() + " (city not found with id: " + request.getCityId() + ")");
                continue;
            }

            Airport airport = AirportMapper.toEntity(request);
            airport.setCity(cityOpt.get());

            Airport savedAirport = airportRepository.save(airport);
            createdAirports.add(AirportMapper.toResponse(savedAirport));
        }

        if (!skippedCodes.isEmpty()) {
            log.info("Bulk airport creation - skipped: {}", skippedCodes);
        }
        log.info("Bulk airport creation - created {} out of {} airports", createdAirports.size(), requests.size());

        return createdAirports;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheUtil.AIRPORTS, key = "#id")
    public AirportResponse getAirportById(UUID id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRPORT_NOT_FOUND_BY_ID, id)));
        return AirportMapper.toResponse(airport);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, AirportResponse> getAirportsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return airportRepository.findAllWithCityByIdIn(ids).stream()
                .collect(Collectors.toMap(Airport::getId, AirportMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AirportResponse> getAllAirports(Pageable pageable) {
        return airportRepository.findAllWithCity(pageable).map(AirportMapper::toResponse);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS, key = "#id"),
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS_BY_CITY, allEntries = true)
    })
    public AirportResponse updateAirport(UUID id, AirportRequest request) {
        Airport existingAirport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRPORT_NOT_FOUND_BY_ID, id)));

        if (request.getIataCode() != null
                && !existingAirport.getIataCode().equals(request.getIataCode())
                && airportRepository.findByIataCode(request.getIataCode()).isPresent()) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRPORT_IATA_CODE_TAKEN, request.getIataCode()));
        }

        if (request.getCityId() != null) {
            City newCity = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, request.getCityId())));
            existingAirport.setCity(newCity);
        }

        AirportMapper.updateEntity(request, existingAirport);

        Airport updatedAirport = airportRepository.save(existingAirport);
        return AirportMapper.toResponse(updatedAirport);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS, key = "#id"),
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS_BY_CITY, allEntries = true)
    })
    public void deleteAirport(UUID id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRPORT_NOT_FOUND_BY_ID, id)));
        airportRepository.delete(airport);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheUtil.AIRPORTS_BY_CITY, key = "#cityId")
    public List<AirportResponse> getAirportsByCityId(UUID cityId) {
        return airportRepository.findByCityId(cityId).stream()
                .map(AirportMapper::toResponse)
                .collect(Collectors.toList());
    }
}

package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AirportRequest;
import com.sunday.common_lib.payload.response.AirportBulkCreateResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirportServiceImpl implements AirportService {

    public static final int MAX_BULK_ROWS = 500;

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
    public AirportBulkCreateResponse createBulkAirports(List<AirportRequest> requests) {
        if (requests.isEmpty()) {
            return AirportBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }
        if (requests.size() > MAX_BULK_ROWS) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BULK_TOO_LARGE, MAX_BULK_ROWS));
        }

        Set<UUID> cityIds = requests.stream().map(AirportRequest::getCityId).collect(Collectors.toSet());
        Map<UUID, City> citiesById = cityRepository.findAllById(cityIds).stream()
                .collect(Collectors.toMap(City::getId, city -> city));

        Set<String> requestedCodes = requests.stream().map(AirportRequest::getIataCode).collect(Collectors.toSet());
        Set<String> existingCodes = airportRepository.findByIataCodeIn(requestedCodes).stream()
                .map(Airport::getIataCode)
                .collect(Collectors.toSet());

        List<AirportBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        List<Airport> toSave = new ArrayList<>();
        for (AirportRequest request : requests) {
            City city = citiesById.get(request.getCityId());
            String code = request.getIataCode();
            if (city == null) {
                skipped.add(skip(request, String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, request.getCityId())));
            } else if (existingCodes.contains(code)) {
                skipped.add(skip(request, String.format(ErrorMessageUtil.AIRPORT_IATA_CODE_ALREADY_EXISTS, code)));
            } else if (!seenCodes.add(code)) {
                skipped.add(skip(request, String.format(ErrorMessageUtil.AIRPORT_IATA_CODE_DUPLICATE_IN_REQUEST, code)));
            } else {
                Airport airport = AirportMapper.toEntity(request);
                airport.setCity(city);
                toSave.add(airport);
            }
        }

        List<AirportResponse> created = airportRepository.saveAll(toSave).stream()
                .map(AirportMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Bulk airport creation - created {} out of {} airports", created.size(), requests.size());
        return AirportBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    private static AirportBulkCreateResponse.SkippedRequest skip(AirportRequest request, String reason) {
        return AirportBulkCreateResponse.SkippedRequest.builder().request(request).reason(reason).build();
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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS, allEntries = true),
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS_BY_CITY, allEntries = true)
    })
    public List<AirportResponse> reassignAirportsToCity(UUID sourceCityId, UUID targetCityId) {
        if (sourceCityId.equals(targetCityId)) {
            throw new BadRequestException(ErrorMessageUtil.CITY_REASSIGN_SAME_CITY);
        }

        City targetCity = cityRepository.findById(targetCityId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, targetCityId)));
        if (!cityRepository.existsById(sourceCityId)) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, sourceCityId));
        }

        List<Airport> airports = airportRepository.findByCityId(sourceCityId);
        if (airports.isEmpty()) {
            return List.of();
        }

        airports.forEach(airport -> airport.setCity(targetCity));

        return airportRepository.saveAll(airports).stream()
                .map(AirportMapper::toResponse)
                .collect(Collectors.toList());
    }
}

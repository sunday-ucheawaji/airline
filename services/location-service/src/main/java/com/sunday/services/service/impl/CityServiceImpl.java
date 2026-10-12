package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.CityRequest;
import com.sunday.common_lib.payload.response.CityBulkCreateResponse;
import com.sunday.common_lib.payload.response.CityResponse;
import com.sunday.common_lib.util.CacheUtil;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.mapper.CityMapper;
import com.sunday.services.model.City;
import com.sunday.services.repository.AirportRepository;
import com.sunday.services.repository.CityRepository;
import com.sunday.services.service.CityService;
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
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CityServiceImpl implements CityService {

    public static final int MAX_BULK_ROWS = 500;

    private final CityRepository cityRepository;
    private final AirportRepository airportRepository;

    // ---------- Core CRUD ----------

    @Override
    public CityResponse createCity(CityRequest request) {
        validateCityRequest(request);

        if (cityRepository.existsByCityCode(request.getCityCode())) {
            throw new ConflictException(String.format(ErrorMessageUtil.CITY_CODE_ALREADY_EXISTS, request.getCityCode()));
        }

        City city = CityMapper.toEntity(request);
        City savedCity = cityRepository.save(city);

        log.info("City created: {} ({})", savedCity.getName(), savedCity.getCityCode());
        return CityMapper.toResponse(savedCity);
    }

    @Override
    public CityBulkCreateResponse createBulkCities(List<CityRequest> requests) {
        if (requests.isEmpty()) {
            return CityBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }
        if (requests.size() > MAX_BULK_ROWS) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BULK_TOO_LARGE, MAX_BULK_ROWS));
        }

        List<CityBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        List<CityRequest> valid = new ArrayList<>();
        for (CityRequest request : requests) {
            try {
                validateCityRequest(request);
                valid.add(request);
            } catch (BadRequestException e) {
                skipped.add(skip(request, e.getMessage()));
            }
        }

        Set<String> requestedCodes = valid.stream().map(CityRequest::getCityCode).collect(Collectors.toSet());
        Set<String> existingCodes = requestedCodes.isEmpty()
                ? Set.of()
                : cityRepository.findByCityCodeIn(requestedCodes).stream()
                        .map(City::getCityCode)
                        .collect(Collectors.toSet());

        Set<String> seenCodes = new HashSet<>();
        List<City> toSave = new ArrayList<>();
        for (CityRequest request : valid) {
            String code = request.getCityCode();
            if (existingCodes.contains(code)) {
                skipped.add(skip(request, String.format(ErrorMessageUtil.CITY_CODE_ALREADY_EXISTS, code)));
            } else if (!seenCodes.add(code)) {
                skipped.add(skip(request, String.format(ErrorMessageUtil.CITY_CODE_DUPLICATE_IN_REQUEST, code)));
            } else {
                toSave.add(CityMapper.toEntity(request));
            }
        }

        List<CityResponse> created = cityRepository.saveAll(toSave).stream()
                .map(CityMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Bulk city creation - created {} out of {} cities", created.size(), requests.size());
        return CityBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    private static CityBulkCreateResponse.SkippedRequest skip(CityRequest request, String reason) {
        return CityBulkCreateResponse.SkippedRequest.builder().request(request).reason(reason).build();
    }

    @Override
    @Cacheable(cacheNames = CacheUtil.CITIES, key = "#id")
    public CityResponse getCityById(UUID id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, id)));
        return CityMapper.toResponse(city);
    }


    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.CITIES, key = "#id"),
            // AirportResponse embeds CityResponse, so cached airports would otherwise show stale city data
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS, allEntries = true),
            @CacheEvict(cacheNames = CacheUtil.AIRPORTS_BY_CITY, allEntries = true)
    })
    public CityResponse updateCity(UUID id, CityRequest request) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, id)));

        validateCityRequest(request);

        if (cityRepository.existsByCityCodeAndIdNot(request.getCityCode(), id)) {
            throw new ConflictException(String.format(ErrorMessageUtil.CITY_CODE_ALREADY_EXISTS, request.getCityCode()));
        }

        City updatedCity = cityRepository.save(CityMapper.updateEntity(city, request));

        log.info("City updated: {} ({})", updatedCity.getName(), updatedCity.getCityCode());
        return CityMapper.toResponse(updatedCity);
    }

    @Override
    @CacheEvict(cacheNames = CacheUtil.CITIES, key = "#id")
    public void deleteCity(UUID id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CITY_NOT_FOUND_BY_ID, id)));

        if (airportRepository.existsByCityId(id)) {
            throw new ConflictException(String.format(ErrorMessageUtil.CITY_STILL_HAS_AIRPORTS, city.getCityCode()));
        }

        cityRepository.delete(city);
        log.info("City deleted: {} ({})", city.getName(), city.getCityCode());
    }

    @Override
    public Page<CityResponse> getAllCities(Pageable pageable) {
        return cityRepository.findAll(pageable).map(CityMapper::toResponse);
    }

    // ---------- Search & Query ----------

    @Override
    public Page<CityResponse> searchCities(String keyword, Pageable pageable) {
        return cityRepository.searchByKeyword(keyword, pageable)
                .map(CityMapper::toResponse);
    }

    @Override
    public Page<CityResponse> getCitiesByCountryCode(String countryCode, Pageable pageable) {
        return cityRepository.findByCountryCodeIgnoreCase(countryCode, pageable).map(CityMapper::toResponse);
    }



    // ---------- Validation ----------

    @Override
    public boolean cityExists(String cityCode) {
        return cityRepository.existsByCityCode(cityCode);
    }

    @Override
    public boolean validateCityCode(String cityCode) {
        return cityCode != null && cityCode.length() <= 10 && cityCode.matches("[A-Z0-9]{2,10}");
    }

    // ---------- Private Helpers ----------

    private void validateCityRequest(CityRequest request) {
        if (!validateCityCode(request.getCityCode())) {
            throw new BadRequestException(ErrorMessageUtil.CITY_CODE_FORMAT_INVALID);
        }

        if (request.getCountryCode() == null || !request.getCountryCode().matches("[A-Z]{2,5}")) {
            throw new BadRequestException(ErrorMessageUtil.CITY_COUNTRY_CODE_FORMAT_INVALID);
        }

        if (request.getTimeZoneId() == null) {
            throw new BadRequestException(ErrorMessageUtil.TIMEZONE_MANDATORY);
        }

        try {
            ZoneId.of(request.getTimeZoneId());
        } catch (DateTimeException ex) {
            throw new BadRequestException(String.format(ErrorMessageUtil.TIMEZONE_INVALID, request.getTimeZoneId()));
        }
    }
}

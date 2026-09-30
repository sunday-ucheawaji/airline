package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.request.FlightSearchRequest;
import com.sunday.common_lib.payload.response.*;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.client.LocationClient;
import com.sunday.services.client.PricingClient;
import com.sunday.services.client.SeatClient;
import com.sunday.services.mapper.FlightInstanceMapper;
import com.sunday.services.model.FlightInstance;
import com.sunday.services.repository.FlightInstanceRepository;
import com.sunday.services.service.FlightSearchService;
import com.sunday.services.specification.FlightInstanceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightSearchServiceImpl implements FlightSearchService {

    private final FlightInstanceRepository flightInstanceRepository;
    private final LocationClient locationClient;
    private final AirlineClient airlineClient;
    private final AirlineIntegrationService airlineIntegrationService;
    private final PricingClient pricingClient;
    private final SeatClient seatClient;

    @Override
    @Transactional(readOnly = true)
    public Page<FlightInstanceResponse> searchFlights(FlightSearchRequest request, Pageable pageable) {
        Pageable sortedPageable = applySort(pageable, request.getSortBy(), request.getSortOrder());
        Specification<FlightInstance> spec = FlightInstanceSpecification.buildSearchSpec(request);
        Page<FlightInstance> dbPage = flightInstanceRepository.findAll(spec, sortedPageable);

        if (dbPage.isEmpty()) {
            return Page.empty(sortedPageable);
        }

        List<FlightInstance> instances = new ArrayList<>(dbPage.getContent());
        Map<Long, FareResponse> fareMap = Collections.emptyMap();

        if (request.getCabinClass() != null) {
            CabinClassPriceFilterResult filterResult = filterByCabinClassAndPrice(instances, request);
            instances = filterResult.instances();
            fareMap = filterResult.fareMap();
            if (instances.isEmpty()) {
                return Page.empty(sortedPageable);
            }
        }

        List<FlightInstanceResponse> responses = enrichWithExternalData(instances, fareMap);
        return new PageImpl<>(responses, sortedPageable, dbPage.getTotalElements());
    }

    private record CabinClassPriceFilterResult(List<FlightInstance> instances, Map<Long, FareResponse> fareMap) {}

    private CabinClassPriceFilterResult filterByCabinClassAndPrice(List<FlightInstance> instances, FlightSearchRequest request) {
        Map<Long, Long> cabinClassIdByAircraftId = new HashMap<>();
        for (Long aircraftId : instances.stream().map(FlightInstance::getAircraftId).distinct().toList()) {
            cabinClassIdByAircraftId.put(aircraftId, resolveCabinClassId(request.getCabinClass(), aircraftId));
        }

        Map<Long, List<FlightInstance>> instancesByCabinClassId = instances.stream()
                .filter(fi -> cabinClassIdByAircraftId.get(fi.getAircraftId()) != null)
                .collect(Collectors.groupingBy(fi -> cabinClassIdByAircraftId.get(fi.getAircraftId())));

        Map<Long, FareResponse> fareMap = new HashMap<>();
        for (Map.Entry<Long, List<FlightInstance>> entry : instancesByCabinClassId.entrySet()) {
            List<Long> flightIds = entry.getValue().stream()
                    .map(fi -> fi.getFlight().getId())
                    .distinct()
                    .toList();
            fareMap.putAll(pricingClient.getLowestFarePerFlight(flightIds, entry.getKey()));
        }

        boolean hasPriceFilter = request.getMinPrice() != null || request.getMaxPrice() != null;
        List<FlightInstance> filtered = instances.stream()
                .filter(fi -> passesFareAndPriceFilter(fareMap.get(fi.getFlight().getId()), request, hasPriceFilter))
                .toList();

        return new CabinClassPriceFilterResult(filtered, fareMap);
    }

    private boolean passesFareAndPriceFilter(FareResponse fare, FlightSearchRequest request, boolean hasPriceFilter) {
        if (fare == null) return false;
        if (!hasPriceFilter) return true;
        Double price = fare.getTotalPrice();
        if (price == null) return false;
        if (request.getMinPrice() != null && price < request.getMinPrice()) return false;
        return request.getMaxPrice() == null || price <= request.getMaxPrice();
    }

    private Long resolveCabinClassId(CabinClassType cabinClassName, Long aircraftId) {
        try {
            CabinClassResponse cabin = seatClient.getCabinClassByAircraftIdAndName(cabinClassName, aircraftId);
            return cabin != null ? cabin.getId() : null;
        } catch (Exception e) {
            log.warn("seat-service call failed for aircraftId={}: {}", aircraftId, e.getMessage());
            return null;
        }
    }

    private List<FlightInstanceResponse> enrichWithExternalData(
            List<FlightInstance> instances,
            Map<Long, FareResponse> fareMap) {

        List<Long> distinctAirlineIds = instances.stream().map(FlightInstance::getAirlineId).distinct().toList();
        List<Long> distinctAircraftIds = instances.stream().map(FlightInstance::getAircraftId).distinct().toList();
        List<Long> distinctAirportIds = instances.stream()
                .flatMap(fi -> Stream.of(fi.getDepartureAirportId(), fi.getArrivalAirportId()))
                .distinct().toList();
        Map<Long, AirlineResponse> airlines = airlineClient.getAirlinesByIdsInternal(distinctAirlineIds);
        Map<Long, AircraftResponse> aircrafts = airlineIntegrationService.getAircraftsByIdsInternal(distinctAircraftIds);
        Map<Long, AirportResponse> airports = locationClient.getAirportsByIds(distinctAirportIds);

        List<FlightInstanceResponse> results = new ArrayList<>(instances.size());
        for (FlightInstance fi : instances) {
            try {
                FlightInstanceResponse response = FlightInstanceMapper.toResponse(
                        fi,
                        aircrafts.get(fi.getAircraftId()),
                        airlines.get(fi.getAirlineId()),
                        airports.get(fi.getDepartureAirportId()),
                        airports.get(fi.getArrivalAirportId()));
                response.setFare(fareMap.get(fi.getFlight().getId()));
                results.add(response);
            } catch (Exception e) {
                log.error("searchFlights: enrichment failed for FlightInstance id={} – skipping: {}",
                        fi.getId(), e.getMessage());
            }
        }
        return results;
    }

    private Pageable applySort(Pageable pageable, String sortBy, String sortOrder) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Sort sort = (sortBy == null || sortBy.isBlank())
                ? Sort.by(direction, "departureDateTime")
                : switch (sortBy.toLowerCase()) {
                    case "arrival"  -> Sort.by(direction, "arrivalDateTime");
                    case "duration" -> JpaSort.unsafe(direction,
                            "TIMESTAMPDIFF(MINUTE, departure_date_time, arrival_date_time)");
                    default         -> Sort.by(direction, "departureDateTime");
                };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}

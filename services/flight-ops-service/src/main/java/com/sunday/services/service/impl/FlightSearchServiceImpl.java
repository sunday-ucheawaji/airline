package com.sunday.services.service.impl;

import com.sunday.common_lib.payload.request.FareSearchRequest;
import com.sunday.common_lib.payload.request.FareSearchRequest.FlightDeparture;
import com.sunday.common_lib.payload.request.FlightSearchRequest;
import com.sunday.common_lib.payload.response.*;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.client.LocationClient;
import com.sunday.services.client.PricingClient;
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
        Map<FlightDeparture, FareResponse> fareMap = Collections.emptyMap();

        if (request.getCabinClass() != null) {
            CabinFareResult filterResult = filterByCabinAndPrice(instances, request);
            instances = filterResult.instances();
            fareMap = filterResult.fareMap();
            if (instances.isEmpty()) {
                return Page.empty(sortedPageable);
            }
        }

        List<FlightInstanceResponse> responses = enrichWithExternalData(instances, fareMap);
        return new PageImpl<>(responses, sortedPageable, dbPage.getTotalElements());
    }

    private record CabinFareResult(List<FlightInstance> instances, Map<FlightDeparture, FareResponse> fareMap) {}

    /** One pricing call for the whole page: the cheapest sellable fare per flight departure in the requested cabin. */
    private CabinFareResult filterByCabinAndPrice(List<FlightInstance> instances, FlightSearchRequest request) {
        List<FlightDeparture> departures = instances.stream()
                .map(FlightSearchServiceImpl::departureOf)
                .distinct()
                .toList();

        Map<FlightDeparture, FareResponse> fareMap = pricingClient
                .searchFares(new FareSearchRequest(request.getCabinClass(), departures))
                .stream()
                .collect(Collectors.toMap(
                        result -> new FlightDeparture(result.getFlightId(), result.getDepartureDate()),
                        FareSearchResult::getFare));

        boolean hasPriceFilter = request.getMinPrice() != null || request.getMaxPrice() != null;
        List<FlightInstance> filtered = instances.stream()
                .filter(fi -> passesFareAndPriceFilter(fareMap.get(departureOf(fi)), request, hasPriceFilter))
                .toList();

        return new CabinFareResult(filtered, fareMap);
    }

    private static FlightDeparture departureOf(FlightInstance fi) {
        return new FlightDeparture(fi.getFlight().getId(), fi.getDepartureDateTime().toLocalDate());
    }

    private boolean passesFareAndPriceFilter(FareResponse fare, FlightSearchRequest request, boolean hasPriceFilter) {
        if (fare == null) return false;
        if (!hasPriceFilter) return true;
        Double price = fare.getTotalPrice();
        if (price == null) return false;
        if (request.getMinPrice() != null && price < request.getMinPrice()) return false;
        return request.getMaxPrice() == null || price <= request.getMaxPrice();
    }

    private List<FlightInstanceResponse> enrichWithExternalData(
            List<FlightInstance> instances,
            Map<FlightDeparture, FareResponse> fareMap) {

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
                response.setFare(fareMap.get(departureOf(fi)));
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

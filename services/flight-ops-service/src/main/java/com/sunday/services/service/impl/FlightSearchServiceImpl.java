package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.AirlineStatus;
import java.util.UUID;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.CabinAvailabilityRequest;
import com.sunday.common_lib.payload.request.FareSearchRequest;
import com.sunday.common_lib.payload.request.FareSearchRequest.FlightDeparture;
import com.sunday.common_lib.payload.request.FlightSearchRequest;
import com.sunday.common_lib.payload.response.*;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.client.LocationClient;
import com.sunday.services.client.PricingClient;
import com.sunday.services.client.SeatClient;
import com.sunday.common_lib.util.ErrorMessageUtil;
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

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
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

    static final int MAX_SEATED_TRAVELLERS = 9;

    @Override
    @Transactional(readOnly = true)
    public Page<FlightInstanceResponse> searchFlights(FlightSearchRequest request, Pageable pageable) {
        validateParty(request);
        Pageable sortedPageable = applySort(pageable, request.getSortBy(), request.getSortOrder());
        Specification<FlightInstance> spec = FlightInstanceSpecification.buildSearchSpec(request);
        Page<FlightInstance> dbPage = flightInstanceRepository.findAll(spec, sortedPageable);

        if (dbPage.isEmpty()) {
            return Page.empty(sortedPageable);
        }

        List<FlightInstance> instances = new ArrayList<>(dbPage.getContent());
        Map<FlightDeparture, FareSearchResult> fareMap = Collections.emptyMap();
        Map<UUID, Long> seatsInCabin = request.getCabinClass() != null
                ? countSeatsInCabin(instances, request) : Collections.emptyMap();

        if (request.getCabinClass() != null) {
            instances = instances.stream()
                    .filter(fi -> seatsInCabin.getOrDefault(fi.getId(), 0L) >= request.seatedPassengers())
                    .toList();
            if (instances.isEmpty()) {
                return emptyPageOf(dbPage, sortedPageable);
            }
            CabinFareResult filterResult = filterByCabinAndPrice(instances, request);
            instances = filterResult.instances();
            fareMap = filterResult.fareMap();
            if (instances.isEmpty()) {
                return emptyPageOf(dbPage, sortedPageable);
            }
        }

        List<FlightInstanceResponse> responses = enrichWithExternalData(instances, fareMap, seatsInCabin);
        return new PageImpl<>(responses, sortedPageable, dbPage.getTotalElements());
    }

    /** Filters run after paging, so a page can empty out while later pages still match: keep the totals. */
    private static Page<FlightInstanceResponse> emptyPageOf(Page<FlightInstance> dbPage, Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, dbPage.getTotalElements());
    }

    private record CabinFareResult(List<FlightInstance> instances, Map<FlightDeparture, FareSearchResult> fareMap) {}

    private static void validateParty(FlightSearchRequest request) {
        int adults = request.getAdults();
        int children = request.getChildren();
        int infants = request.getInfants();
        if (adults < 1) {
            throw new BadRequestException(ErrorMessageUtil.SEARCH_ADULTS_MIN);
        }
        if (children < 0 || infants < 0) {
            throw new BadRequestException(ErrorMessageUtil.SEARCH_PASSENGERS_NEGATIVE);
        }
        if (adults + children > MAX_SEATED_TRAVELLERS) {
            throw new BadRequestException(String.format(ErrorMessageUtil.SEARCH_TOO_MANY_SEATED, MAX_SEATED_TRAVELLERS));
        }
        if (infants > adults) {
            throw new BadRequestException(String.format(ErrorMessageUtil.SEARCH_TOO_MANY_INFANTS, infants, adults));
        }
    }

    /**
     * One seat-service call for the whole page: free seats in the requested cabin per departure. A departure needs one
     * for every adult and child; lap infants need none. Fails closed, since showing unsellable flights misleads.
     */
    private Map<UUID, Long> countSeatsInCabin(List<FlightInstance> instances, FlightSearchRequest request) {
        List<UUID> ids = instances.stream().map(FlightInstance::getId).toList();
        Map<UUID, Long> available = seatClient.countAvailableSeats(new CabinAvailabilityRequest(request.getCabinClass(), ids));
        if (available == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE);
        }
        return available;
    }

    /** One pricing call for the whole page: the fare giving the party's lowest total per flight departure in the requested cabin. */
    private CabinFareResult filterByCabinAndPrice(List<FlightInstance> instances, FlightSearchRequest request) {
        List<FlightDeparture> departures = instances.stream()
                .map(FlightSearchServiceImpl::departureOf)
                .distinct()
                .toList();

        FareSearchRequest fareRequest = new FareSearchRequest(request.getCabinClass(), departures,
                request.getAdults(), request.getChildren(), request.getInfants());
        Map<FlightDeparture, FareSearchResult> fareMap = pricingClient
                .searchFares(fareRequest)
                .stream()
                .collect(Collectors.toMap(
                        result -> new FlightDeparture(result.getFlightId(), result.getDepartureDate()),
                        Function.identity()));

        boolean hasPriceFilter = request.getMinPrice() != null || request.getMaxPrice() != null;
        List<FlightInstance> filtered = instances.stream()
                .filter(fi -> passesFareAndPriceFilter(fareMap.get(departureOf(fi)), request, hasPriceFilter))
                .toList();

        return new CabinFareResult(filtered, fareMap);
    }

    private static FlightDeparture departureOf(FlightInstance fi) {
        return new FlightDeparture(fi.getFlight().getId(), fi.getDepartureDateTime().toLocalDate());
    }

    private boolean passesFareAndPriceFilter(FareSearchResult result, FlightSearchRequest request, boolean hasPriceFilter) {
        if (result == null || result.getFare() == null) return false;
        FareResponse fare = result.getFare();
        if (!hasPriceFilter) return true;
        BigDecimal price = fare.getTotalPrice();
        if (price == null) return false;
        if (request.getMinPrice() != null && price.compareTo(request.getMinPrice()) < 0) return false;
        return request.getMaxPrice() == null || price.compareTo(request.getMaxPrice()) <= 0;
    }

    private List<FlightInstanceResponse> enrichWithExternalData(
            List<FlightInstance> instances,
            Map<FlightDeparture, FareSearchResult> fareMap,
            Map<UUID, Long> seatsInCabin) {

        List<UUID> distinctAirlineIds = instances.stream().map(FlightInstance::getAirlineId).distinct().toList();
        List<UUID> distinctAircraftIds = instances.stream().map(FlightInstance::getAircraftId).distinct().toList();
        List<UUID> distinctAirportIds = instances.stream()
                .flatMap(fi -> Stream.of(fi.getDepartureAirportId(), fi.getArrivalAirportId()))
                .distinct().toList();
        Map<UUID, AirlineResponse> airlines = airlineClient.getAirlinesByIdsInternal(distinctAirlineIds);
        Map<UUID, AircraftResponse> aircrafts = airlineIntegrationService.getAircraftsByIdsInternal(distinctAircraftIds);
        Map<UUID, AirportResponse> airports = locationClient.getAirportsByIds(distinctAirportIds);

        List<FlightInstanceResponse> results = new ArrayList<>(instances.size());
        for (FlightInstance fi : instances) {
            AirlineResponse airline = airlines.get(fi.getAirlineId());
            if (isKnownNotSelling(airline)) {
                continue;
            }
            try {
                FlightInstanceResponse response = FlightInstanceMapper.toResponse(
                        fi,
                        aircrafts.get(fi.getAircraftId()),
                        airline,
                        airports.get(fi.getDepartureAirportId()),
                        airports.get(fi.getArrivalAirportId()));
                response.setSeatsLeftInCabin(seatsInCabin.get(fi.getId()));
                FareSearchResult fare = fareMap.get(departureOf(fi));
                if (fare != null) {
                    response.setFare(fare.getFare());
                    response.setPartyPrice(fare.getPartyPrice());
                }
                results.add(response);
            } catch (Exception e) {
                log.error("searchFlights: enrichment failed for FlightInstance id={} – skipping", fi.getId(), e);
            }
        }
        return results;
    }

    /**
     * A suspended, banned or closed airline's departures are not offered. An airline the lookup could not resolve is
     * still shown: enrichment is display-only, and booking-service refuses the booking itself.
     */
    private static boolean isKnownNotSelling(AirlineResponse airline) {
        return airline != null && airline.getStatus() != null && airline.getStatus() != AirlineStatus.ACTIVE;
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

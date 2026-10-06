package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.payload.request.FareSearchRequest;
import com.sunday.common_lib.payload.request.FareSearchRequest.FlightDeparture;
import com.sunday.common_lib.payload.response.FareSearchResult;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.mapper.FareMapper;
import com.sunday.services.model.Fare;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.service.FareSearchService;
import com.sunday.services.validation.FareValidityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareSearchServiceImpl implements FareSearchService {

    static final int MAX_DEPARTURES = 500;

    private final FareRepository fareRepository;
    private final Clock clock;

    @Override
    public List<FareSearchResult> search(FareSearchRequest request) {
        List<FlightDeparture> departures = request.getFlights().stream().distinct().toList();
        if (departures.size() > MAX_DEPARTURES) {
            throw new BadRequestException(String.format(ErrorMessageUtil.FARE_SEARCH_TOO_LARGE, MAX_DEPARTURES));
        }

        Set<Long> flightIds = departures.stream().map(FlightDeparture::getFlightId).collect(Collectors.toSet());
        LocalDate firstDate = departures.stream().map(FlightDeparture::getDepartureDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate lastDate = departures.stream().map(FlightDeparture::getDepartureDate).max(LocalDate::compareTo).orElseThrow();
        Instant now = Instant.now(clock);

        Map<Long, List<Fare>> candidatesByFlight = fareRepository
                .findSearchCandidates(flightIds, request.getCabinClass(), FareStatus.PUBLISHED, now, firstDate, lastDate)
                .stream()
                .collect(Collectors.groupingBy(Fare::getFlightId));

        List<FareSearchResult> results = new ArrayList<>();
        for (FlightDeparture departure : departures) {
            candidatesByFlight.getOrDefault(departure.getFlightId(), List.of()).stream()
                    .filter(fare -> FareValidityPolicy.isSellable(fare, departure.getDepartureDate(), now))
                    .min(Comparator.comparing(Fare::getTotalAmount))
                    .ifPresent(fare -> results.add(new FareSearchResult(
                            departure.getFlightId(), departure.getDepartureDate(), FareMapper.toResponse(fare))));
        }
        return results;
    }
}

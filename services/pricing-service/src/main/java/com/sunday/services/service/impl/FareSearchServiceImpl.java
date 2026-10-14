package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.payload.request.FareSearchRequest;
import com.sunday.common_lib.payload.request.FareSearchRequest.FlightDeparture;
import com.sunday.common_lib.payload.response.FareSearchResult;
import com.sunday.common_lib.payload.response.PartyPriceResponse;
import com.sunday.common_lib.payload.response.PartyPriceResponse.PassengerPrice;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.mapper.FareMapper;
import com.sunday.services.model.Fare;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.service.FareSearchService;
import com.sunday.services.validation.FareValidityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prices a page of departures for a travelling party. Each adult fare is priced the way booking-service will quote
 * it: children and lap infants by the fare filed alongside it for their type when that one is sellable, otherwise by
 * the adult fare, and never in another currency. The adult fare giving the lowest party total wins.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareSearchServiceImpl implements FareSearchService {

    static final int MAX_DEPARTURES = 500;

    private final FareRepository fareRepository;
    private final Clock clock;

    private record SiblingKey(UUID airlineId, UUID flightId, Character rbdCode, String name, PassengerType type) {
        static SiblingKey of(Fare fare, PassengerType type) {
            return new SiblingKey(fare.getAirlineId(), fare.getFlightId(), fare.getRbdCode(), fare.getName(), type);
        }
    }

    private record Priced(Fare adultFare, PartyPriceResponse partyPrice) {}

    @Override
    public List<FareSearchResult> search(FareSearchRequest request) {
        List<FlightDeparture> departures = request.getFlights().stream().distinct().toList();
        if (departures.size() > MAX_DEPARTURES) {
            throw new BadRequestException(String.format(ErrorMessageUtil.FARE_SEARCH_TOO_LARGE, MAX_DEPARTURES));
        }
        if (request.getInfants() > request.getAdults()) {
            throw new BadRequestException(String.format(
                    ErrorMessageUtil.BOOKING_TOO_MANY_INFANTS, request.getInfants(), request.getAdults()));
        }

        Set<PassengerType> types = EnumSet.of(PassengerType.ADT);
        if (request.getChildren() > 0) types.add(PassengerType.CHD);
        if (request.getInfants() > 0) types.add(PassengerType.INF);

        Set<UUID> flightIds = departures.stream().map(FlightDeparture::getFlightId).collect(Collectors.toSet());
        LocalDate firstDate = departures.stream().map(FlightDeparture::getDepartureDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate lastDate = departures.stream().map(FlightDeparture::getDepartureDate).max(LocalDate::compareTo).orElseThrow();
        Instant now = Instant.now(clock);

        List<Fare> candidates = fareRepository
                .findSearchCandidates(flightIds, request.getCabinClass(), types, FareStatus.PUBLISHED, now, firstDate, lastDate);
        Map<UUID, List<Fare>> adultFaresByFlight = candidates.stream()
                .filter(fare -> fare.getPassengerType() == PassengerType.ADT)
                .collect(Collectors.groupingBy(Fare::getFlightId));
        Map<SiblingKey, Fare> siblings = candidates.stream()
                .filter(fare -> fare.getPassengerType() != PassengerType.ADT)
                .collect(Collectors.toMap(fare -> SiblingKey.of(fare, fare.getPassengerType()), Function.identity(), (a, b) -> a));

        List<FareSearchResult> results = new ArrayList<>();
        for (FlightDeparture departure : departures) {
            LocalDate date = departure.getDepartureDate();
            adultFaresByFlight.getOrDefault(departure.getFlightId(), List.of()).stream()
                    .filter(fare -> FareValidityPolicy.isSellable(fare, date, now))
                    .map(fare -> priceParty(fare, request, siblings, date, now))
                    .flatMap(Optional::stream)
                    .min(Comparator.comparing((Priced priced) -> priced.partyPrice().getTotalPrice())
                            .thenComparing(priced -> priced.adultFare().getTotalAmount()))
                    .ifPresent(priced -> results.add(new FareSearchResult(
                            departure.getFlightId(), date, FareMapper.toResponse(priced.adultFare()), priced.partyPrice())));
        }
        return results;
    }

    /** Empty when a child or infant fare is in another currency, which booking would refuse. */
    private Optional<Priced> priceParty(Fare adultFare, FareSearchRequest request, Map<SiblingKey, Fare> siblings,
                                        LocalDate date, Instant now) {
        Map<PassengerType, Integer> party = new EnumMap<>(PassengerType.class);
        party.put(PassengerType.ADT, request.getAdults());
        if (request.getChildren() > 0) party.put(PassengerType.CHD, request.getChildren());
        if (request.getInfants() > 0) party.put(PassengerType.INF, request.getInfants());

        List<PassengerPrice> lines = new ArrayList<>();
        for (Map.Entry<PassengerType, Integer> entry : party.entrySet()) {
            Fare fare = fareFor(adultFare, entry.getKey(), siblings, date, now);
            if (!adultFare.getCurrency().equals(fare.getCurrency())) {
                return Optional.empty();
            }
            lines.add(line(entry.getKey(), entry.getValue(), fare));
        }

        BigDecimal total = lines.stream().map(PassengerPrice::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(new Priced(adultFare, PartyPriceResponse.builder()
                .currency(adultFare.getCurrency())
                .totalPrice(total)
                .passengers(lines)
                .build()));
    }

    private static Fare fareFor(Fare adultFare, PassengerType type, Map<SiblingKey, Fare> siblings, LocalDate date, Instant now) {
        if (type == PassengerType.ADT) {
            return adultFare;
        }
        Fare sibling = siblings.get(SiblingKey.of(adultFare, type));
        return sibling != null && FareValidityPolicy.isSellable(sibling, date, now) ? sibling : adultFare;
    }

    private static PassengerPrice line(PassengerType type, int count, Fare fare) {
        return PassengerPrice.builder()
                .passengerType(type)
                .count(count)
                .fareId(fare.getId())
                .unitPrice(fare.getTotalAmount())
                .subtotal(fare.getTotalAmount().multiply(BigDecimal.valueOf(count)))
                .build();
    }
}

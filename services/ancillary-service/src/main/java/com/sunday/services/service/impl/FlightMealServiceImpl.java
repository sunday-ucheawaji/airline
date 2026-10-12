package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.AncillaryPermissions;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.FlightMealRequest;
import com.sunday.common_lib.payload.response.FlightMealBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.mapper.FlightMealMapper;
import com.sunday.services.model.FlightMeal;
import com.sunday.services.model.Meal;
import com.sunday.services.repository.FlightMealRepository;
import com.sunday.services.repository.MealRepository;
import com.sunday.services.service.FlightMealService;
import com.sunday.services.specification.FlightMealSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightMealServiceImpl implements FlightMealService {

    public static final int MAX_BULK_ROWS = 500;

    private final FlightMealRepository flightMealRepository;
    private final MealRepository mealRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    @Transactional
    public FlightMealResponse create(UUID userId, FlightMealRequest request) {
        log.debug("Creating flight meal for flight ID: {} and meal ID: {}",
                request.getFlightId(), request.getMealId());

        Meal meal = mealRepository.findById(request.getMealId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.MEAL_NOT_FOUND_BY_ID, request.getMealId())));
        airlineIntegrationService.requirePermission(userId, meal.getAirlineId(), AncillaryPermissions.MANAGE);

        Specification<FlightMeal> spec = FlightMealSpecification.hasFlightIdAndMealId(
                request.getFlightId(), request.getMealId());
        if (flightMealRepository.exists(spec)) {
            throw new BadRequestException(String.format(
                    ErrorMessageUtil.FLIGHT_MEAL_ALREADY_ASSIGNED_TO_FLIGHT, request.getMealId(), request.getFlightId()));
        }

        FlightMeal flightMeal = FlightMeal.builder()
                .flightId(request.getFlightId())
                .meal(meal)
                .available(request.getAvailable())
                .price(request.getPrice())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        FlightMeal saved = flightMealRepository.save(flightMeal);
        log.info("Flight meal created successfully with id: {}", saved.getId());
        return FlightMealMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FlightMealBulkCreateResponse bulkCreate(UUID userId, List<FlightMealRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return FlightMealBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }
        if (requests.size() > MAX_BULK_ROWS) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BULK_TOO_LARGE, MAX_BULK_ROWS));
        }
        log.debug("Bulk creating {} flight meals", requests.size());

        List<UUID> mealIds = requests.stream().map(FlightMealRequest::getMealId).distinct().toList();
        Map<UUID, Meal> mealById = mealRepository.findAllById(mealIds).stream()
                .collect(Collectors.toMap(Meal::getId, m -> m));

        List<FlightMealBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();

        Set<UUID> airlineIds = mealById.values().stream().map(Meal::getAirlineId).collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, AncillaryPermissions.MANAGE);

        List<UUID> flightIds = requests.stream().map(FlightMealRequest::getFlightId).distinct().toList();
        Set<String> existingKeys = flightMealRepository.findByFlightIdInAndMeal_IdIn(flightIds, mealIds).stream()
                .map(fm -> fm.getFlightId() + ":" + fm.getMeal().getId())
                .collect(Collectors.toSet());

        List<FlightMeal> toInsert = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();
        for (FlightMealRequest request : requests) {
            Meal meal = mealById.get(request.getMealId());
            if (meal == null) {
                skipped.add(skip(request, String.format(ErrorMessageUtil.MEAL_NOT_FOUND_BY_ID, request.getMealId())));
                continue;
            }
            String key = request.getFlightId() + ":" + request.getMealId();
            if (existingKeys.contains(key)) {
                skipped.add(skip(request, String.format(
                        ErrorMessageUtil.FLIGHT_MEAL_ALREADY_ASSIGNED_TO_FLIGHT, request.getMealId(), request.getFlightId())));
                continue;
            }
            if (!seenInBatch.add(key)) {
                skipped.add(skip(request, String.format(
                        ErrorMessageUtil.FLIGHT_MEAL_DUPLICATE_IN_REQUEST, request.getMealId(), request.getFlightId())));
                continue;
            }
            toInsert.add(FlightMeal.builder()
                    .flightId(request.getFlightId())
                    .meal(meal)
                    .available(request.getAvailable())
                    .price(request.getPrice())
                    .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                    .build());
        }

        List<FlightMealResponse> created = flightMealRepository.saveAll(toInsert).stream()
                .map(FlightMealMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Bulk create: {} created, {} skipped", created.size(), skipped.size());
        return FlightMealBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    private FlightMealBulkCreateResponse.SkippedRequest skip(FlightMealRequest request, String reason) {
        return FlightMealBulkCreateResponse.SkippedRequest.builder().request(request).reason(reason).build();
    }

    @Override
    @Transactional(readOnly = true)
    public FlightMealResponse getById(UUID id) {
        FlightMeal flightMeal = flightMealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_MEAL_NOT_FOUND_BY_ID, id)));
        return FlightMealMapper.toResponse(flightMeal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightMealResponse> getByFlightId(UUID flightId) {
        Specification<FlightMeal> spec = FlightMealSpecification.hasFlightId(flightId);
        return flightMealRepository.findAll(spec).stream()
                .map(FlightMealMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightMealResponse> getAllByIds(List<UUID> Ids) {
        List<FlightMeal> meals = flightMealRepository.findAllById(Ids);
        return meals.stream().map(FlightMealMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FlightMealResponse update(UUID userId, UUID id, FlightMealRequest request) {
        log.debug("Updating flight meal with id: {}", id);

        FlightMeal flightMeal = flightMealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_MEAL_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, flightMeal.getMeal().getAirlineId(), AncillaryPermissions.MANAGE);

        if (!flightMeal.getFlightId().equals(request.getFlightId())) {
            flightMeal.setFlightId(request.getFlightId());
        }

        if (!flightMeal.getMeal().getId().equals(request.getMealId())) {
            Meal meal = mealRepository.findById(request.getMealId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessageUtil.MEAL_NOT_FOUND_BY_ID, request.getMealId())));
            airlineIntegrationService.requirePermission(userId, meal.getAirlineId(), AncillaryPermissions.MANAGE);
            flightMeal.setMeal(meal);
        }

        flightMeal.setAvailable(request.getAvailable());
        flightMeal.setPrice(request.getPrice());
        flightMeal.setDisplayOrder(request.getDisplayOrder());

        FlightMeal updated = flightMealRepository.save(flightMeal);
        log.info("Flight meal updated successfully with id: {}", updated.getId());
        return FlightMealMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        FlightMeal flightMeal = flightMealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_MEAL_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, flightMeal.getMeal().getAirlineId(), AncillaryPermissions.MANAGE);
        flightMealRepository.delete(flightMeal);
        log.info("Flight meal deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public FlightMealResponse updateAvailability(UUID userId, UUID id, Boolean available) {
        FlightMeal flightMeal = flightMealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_MEAL_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, flightMeal.getMeal().getAirlineId(), AncillaryPermissions.MANAGE);
        flightMeal.setAvailable(available);
        FlightMeal updated = flightMealRepository.save(flightMeal);
        log.info("Flight meal availability updated successfully for id: {}", updated.getId());
        return FlightMealMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateMealPrice(UUID flightId, List<UUID> mealIds) {
        if (mealIds == null || mealIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<FlightMeal> meals = flightMealRepository.findAllById(mealIds);

        // findAllById silently skips unknown IDs — reject them instead of undercharging
        Set<UUID> foundIds = meals.stream()
                .map(FlightMeal::getId)
                .collect(Collectors.toSet());
        List<UUID> missingIds = mealIds.stream()
                .distinct()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.FLIGHT_MEAL_NOT_FOUND_FOR_IDS, missingIds));
        }

        List<String> wrongFlight = meals.stream()
                .filter(meal -> !meal.getFlightId().equals(flightId))
                .map(meal -> String.format(ErrorMessageUtil.FLIGHT_MEAL_WRONG_FLIGHT, meal.getId(), flightId))
                .toList();
        if (!wrongFlight.isEmpty()) {
            throw new ConflictException(String.join("; ", wrongFlight));
        }

        // A repeated meal ID means "buy N", same semantics as FlightCabinAncillaryServiceImpl.calculateAncillaryPrice.
        Map<UUID, Long> quantities = mealIds.stream().collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        BigDecimal total = BigDecimal.ZERO;
        for (FlightMeal flightMeal : meals) {
            String label = flightMeal.getMeal().getName() + " (id " + flightMeal.getId() + ")";
            long quantity = quantities.get(flightMeal.getId());
            if (!Boolean.TRUE.equals(flightMeal.getAvailable())
                    || !Boolean.TRUE.equals(flightMeal.getMeal().getAvailable())) {
                throw new BadRequestException(String.format(ErrorMessageUtil.MEAL_NOT_AVAILABLE, label));
            }
            if (flightMeal.getPrice() == null) {
                throw new BadRequestException(String.format(ErrorMessageUtil.MEAL_PRICE_NOT_SET, label));
            }
            total = total.add(flightMeal.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        return total;
    }
}

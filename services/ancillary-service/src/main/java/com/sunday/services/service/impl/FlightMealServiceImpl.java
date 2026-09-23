package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.AncillaryPermissions;
import com.sunday.common_lib.exception.BadRequestException;
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

    private final FlightMealRepository flightMealRepository;
    private final MealRepository mealRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    @Transactional
    public FlightMealResponse create(Long userId, FlightMealRequest request) {
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
    public FlightMealBulkCreateResponse bulkCreate(Long userId, List<FlightMealRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return FlightMealBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }
        log.debug("Bulk creating {} flight meals", requests.size());

        List<Long> mealIds = requests.stream().map(FlightMealRequest::getMealId).distinct().toList();
        Map<Long, Meal> mealById = mealRepository.findAllById(mealIds).stream()
                .collect(Collectors.toMap(Meal::getId, m -> m));

        List<FlightMealBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();

        Set<Long> airlineIds = mealById.values().stream().map(Meal::getAirlineId).collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, AncillaryPermissions.MANAGE);

        List<Long> flightIds = requests.stream().map(FlightMealRequest::getFlightId).distinct().toList();
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
                skipped.add(skip(request, "Duplicate meal " + request.getMealId()
                        + " for flight " + request.getFlightId() + " within this request"));
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
    public FlightMealResponse getById(Long id) {
        FlightMeal flightMeal = flightMealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_MEAL_NOT_FOUND_BY_ID, id)));
        return FlightMealMapper.toResponse(flightMeal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightMealResponse> getByFlightId(Long flightId) {
        Specification<FlightMeal> spec = FlightMealSpecification.hasFlightId(flightId);
        return flightMealRepository.findAll(spec).stream()
                .map(FlightMealMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightMealResponse> getAllByIds(List<Long> Ids) {
        List<FlightMeal> meals = flightMealRepository.findAllById(Ids);
        return meals.stream().map(FlightMealMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FlightMealResponse update(Long userId, Long id, FlightMealRequest request) {
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
    public void delete(Long userId, Long id) {
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
    public FlightMealResponse updateAvailability(Long userId, Long id, Boolean available) {
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
    public Double calculateMealPrice(List<Long> mealIds) {
        if (mealIds == null || mealIds.isEmpty()) {
            return 0.0;
        }

        List<Long> duplicateIds = mealIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!duplicateIds.isEmpty()) {
            throw new BadRequestException(String.format(ErrorMessageUtil.FLIGHT_MEAL_DUPLICATE_IN_REQUEST, duplicateIds));
        }

        List<FlightMeal> meals = flightMealRepository.findAllById(mealIds);

        Set<Long> foundIds = meals.stream()
                .map(FlightMeal::getId)
                .collect(Collectors.toSet());
        List<Long> missingIds = mealIds.stream()
                .distinct()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.FLIGHT_MEAL_NOT_FOUND_FOR_IDS, missingIds));
        }

        double total = 0.0;
        for (FlightMeal flightMeal : meals) {
            String label = flightMeal.getMeal().getName() + " (id " + flightMeal.getId() + ")";
            if (!Boolean.TRUE.equals(flightMeal.getAvailable())
                    || !Boolean.TRUE.equals(flightMeal.getMeal().getAvailable())) {
                throw new BadRequestException(String.format(ErrorMessageUtil.MEAL_NOT_AVAILABLE, label));
            }
            if (flightMeal.getPrice() == null) {
                throw new BadRequestException(String.format(ErrorMessageUtil.MEAL_PRICE_NOT_SET, label));
            }
            total += flightMeal.getPrice();
        }
        return total;
    }
}

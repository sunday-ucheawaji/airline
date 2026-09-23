package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.AncillaryPermissions;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.MealRequest;
import com.sunday.common_lib.payload.response.MealBulkCreateResponse;
import com.sunday.common_lib.payload.response.MealResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.mapper.MealMapper;
import com.sunday.services.model.Meal;
import com.sunday.services.repository.MealRepository;
import com.sunday.services.service.MealService;
import com.sunday.services.specification.MealSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    @Transactional
    public MealResponse create(Long userId, MealRequest request) {
        log.debug("Creating meal with code: {}", request.getCode());

        Long airlineId = request.getAirlineId();
        if (airlineId == null) {
            throw new BadRequestException(ErrorMessageUtil.AIRLINE_ID_REQUIRED);
        }
        airlineIntegrationService.requirePermission(userId, airlineId, AncillaryPermissions.MANAGE);

        Specification<Meal> spec = MealSpecification.hasCodeAndAirlineId(request.getCode(), airlineId);
        if (mealRepository.exists(spec)) {
            throw new BadRequestException(
                    String.format(ErrorMessageUtil.MEAL_CODE_ALREADY_EXISTS_FOR_AIRLINE, request.getCode(), airlineId));
        }

        Meal meal = Meal.builder()
                .code(request.getCode())
                .name(request.getName())
                .mealType(request.getMealType())
                .dietaryRestriction(request.getDietaryRestriction())
                .ingredients(request.getIngredients())
                .imageUrl(request.getImageUrl())
                .available(request.getAvailable())
                .requiresAdvanceBooking(request.getRequiresAdvanceBooking() != null
                        ? request.getRequiresAdvanceBooking() : false)
                .advanceBookingHours(request.getAdvanceBookingHours())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .airlineId(airlineId)
                .build();

        Meal savedMeal = mealRepository.save(meal);
        log.info("Meal created successfully with id: {}", savedMeal.getId());
        return MealMapper.toResponse(savedMeal);
    }

    @Override
    @Transactional
    public MealBulkCreateResponse bulkCreate(Long userId, List<MealRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return MealBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }
        log.debug("Bulk creating {} meals", requests.size());

        List<MealBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        List<MealRequest> candidates = new ArrayList<>();
        for (MealRequest req : requests) {
            if (req.getAirlineId() == null) {
                skipped.add(skip(req, ErrorMessageUtil.AIRLINE_ID_REQUIRED));
            } else {
                candidates.add(req);
            }
        }

        Set<Long> airlineIds = candidates.stream().map(MealRequest::getAirlineId).collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, AncillaryPermissions.MANAGE);

        List<String> codes = candidates.stream().map(MealRequest::getCode).distinct().toList();
        Set<String> existingKeys = mealRepository.findByAirlineIdInAndCodeIn(new ArrayList<>(airlineIds), codes)
                .stream()
                .map(m -> m.getAirlineId() + ":" + m.getCode())
                .collect(Collectors.toSet());

        List<Meal> toInsert = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();
        for (MealRequest req : candidates) {
            String key = req.getAirlineId() + ":" + req.getCode();
            if (existingKeys.contains(key)) {
                skipped.add(skip(req, String.format(
                        ErrorMessageUtil.MEAL_CODE_ALREADY_EXISTS_FOR_AIRLINE, req.getCode(), req.getAirlineId())));
                continue;
            }
            if (!seenInBatch.add(key)) {
                skipped.add(skip(req, "Duplicate code " + req.getCode() + " for airline "
                        + req.getAirlineId() + " within this request"));
                continue;
            }
            toInsert.add(Meal.builder()
                    .code(req.getCode())
                    .name(req.getName())
                    .mealType(req.getMealType())
                    .dietaryRestriction(req.getDietaryRestriction())
                    .ingredients(req.getIngredients())
                    .imageUrl(req.getImageUrl())
                    .available(req.getAvailable())
                    .requiresAdvanceBooking(req.getRequiresAdvanceBooking() != null
                            ? req.getRequiresAdvanceBooking() : false)
                    .advanceBookingHours(req.getAdvanceBookingHours())
                    .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                    .airlineId(req.getAirlineId())
                    .build());
        }

        List<MealResponse> created = mealRepository.saveAll(toInsert).stream()
                .map(MealMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Bulk create: {} created, {} skipped", created.size(), skipped.size());
        return MealBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    private MealBulkCreateResponse.SkippedRequest skip(MealRequest request, String reason) {
        return MealBulkCreateResponse.SkippedRequest.builder().request(request).reason(reason).build();
    }

    @Override
    @Transactional(readOnly = true)
    public MealResponse getById(Long id) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.MEAL_NOT_FOUND_BY_ID, id)));
        return MealMapper.toResponse(meal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealResponse> getByAirlineId(Long userId, Long airlineId) {
        airlineIntegrationService.requireMembership(userId, airlineId);
        Specification<Meal> spec = MealSpecification.hasAirlineId(airlineId);
        return mealRepository.findAll(spec).stream()
                .map(MealMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MealResponse update(Long userId, Long id, MealRequest request) {
        log.debug("Updating meal with id: {}", id);

        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.MEAL_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, meal.getAirlineId(), AncillaryPermissions.MANAGE);

        if (!meal.getCode().equals(request.getCode())) {
            Specification<Meal> spec = MealSpecification.hasCodeAndAirlineId(request.getCode(), meal.getAirlineId());
            if (mealRepository.exists(spec)) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.MEAL_CODE_ALREADY_EXISTS_FOR_AIRLINE, request.getCode(), meal.getAirlineId()));
            }
        }

        meal.setCode(request.getCode());
        meal.setName(request.getName());
        meal.setMealType(request.getMealType());
        meal.setDietaryRestriction(request.getDietaryRestriction());
        meal.setIngredients(request.getIngredients());
        meal.setImageUrl(request.getImageUrl());
        meal.setAvailable(request.getAvailable());
        meal.setRequiresAdvanceBooking(request.getRequiresAdvanceBooking());
        meal.setAdvanceBookingHours(request.getAdvanceBookingHours());
        meal.setDisplayOrder(request.getDisplayOrder());

        Meal updatedMeal = mealRepository.save(meal);
        log.info("Meal updated successfully with id: {}", updatedMeal.getId());
        return MealMapper.toResponse(updatedMeal);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.MEAL_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, meal.getAirlineId(), AncillaryPermissions.MANAGE);
        mealRepository.delete(meal);
        log.info("Meal deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public MealResponse updateAvailability(Long userId, Long id, Boolean available) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.MEAL_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, meal.getAirlineId(), AncillaryPermissions.MANAGE);
        meal.setAvailable(available);
        Meal updatedMeal = mealRepository.save(meal);
        log.info("Meal availability updated successfully for id: {}", updatedMeal.getId());
        return MealMapper.toResponse(updatedMeal);
    }
}

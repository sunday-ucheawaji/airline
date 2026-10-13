package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.AirlinePermissions;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AirlineIdentityChangeRequestCreate;
import com.sunday.common_lib.payload.response.AirlineIdentityChangeResponse;
import com.sunday.common_lib.util.CacheUtil;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.IdentityChangeStatus;
import com.sunday.services.mapper.AirlineIdentityChangeMapper;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineIdentityChangeRequest;
import com.sunday.services.repository.AirlineIdentityChangeRequestRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.service.AirlineIdentityChangeService;
import com.sunday.services.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AirlineIdentityChangeServiceImpl implements AirlineIdentityChangeService {

    private final AirlineIdentityChangeRequestRepository changeRepository;
    private final AirlineRepository airlineRepository;
    private final AirlineService airlineService;

    @Override
    @Transactional
    public AirlineIdentityChangeResponse request(UUID airlineId, AirlineIdentityChangeRequestCreate request, UUID userId) {
        Airline airline = getAirlineOrThrow(airlineId);
        requireActiveAirline(airline);
        airlineService.requirePermission(userId, List.of(airlineId), AirlinePermissions.IDENTITY_CHANGE_REQUEST);

        AirlineIdentityChangeRequest change = AirlineIdentityChangeRequest.builder()
                .airline(airline)
                .requestedByUserId(userId)
                .proposedLegalName(changed(request.getLegalName(), airline.getLegalName()))
                .proposedRegistrationNumber(changed(request.getRegistrationNumber(), airline.getRegistrationNumber()))
                .proposedIataCode(changed(request.getIataCode(), airline.getIataCode()))
                .proposedIcaoCode(changed(request.getIcaoCode(), airline.getIcaoCode()))
                .reason(request.getReason())
                .status(IdentityChangeStatus.PENDING)
                .build();
        if (change.getProposedLegalName() == null && change.getProposedRegistrationNumber() == null
                && change.getProposedIataCode() == null && change.getProposedIcaoCode() == null) {
            throw new BadRequestException(ErrorMessageUtil.AIRLINE_IDENTITY_CHANGE_NOTHING_CHANGED);
        }
        if (changeRepository.existsByAirlineIdAndStatus(airlineId, IdentityChangeStatus.PENDING)) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRLINE_IDENTITY_CHANGE_ALREADY_PENDING, airlineId));
        }
        requireCodesAvailable(change);
        return AirlineIdentityChangeMapper.toResponse(changeRepository.save(change));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AirlineIdentityChangeResponse> listForAirline(UUID airlineId, UUID userId) {
        airlineService.requirePermission(userId, List.of(airlineId), AirlinePermissions.PROFILE_UPDATE);
        return changeRepository.findByAirlineIdOrderByCreatedAtDesc(airlineId).stream()
                .map(AirlineIdentityChangeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AirlineIdentityChangeResponse cancel(UUID airlineId, UUID requestId, UUID userId) {
        AirlineIdentityChangeRequest change = changeRepository.findByIdAndAirlineId(requestId, airlineId)
                .orElseThrow(() -> notFound(requestId));
        if (!change.getRequestedByUserId().equals(userId)) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.AIRLINE_IDENTITY_CHANGE_CANCEL_NOT_REQUESTER, requestId));
        }
        requirePending(change);
        change.setStatus(IdentityChangeStatus.CANCELLED);
        return AirlineIdentityChangeMapper.toResponse(changeRepository.save(change));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AirlineIdentityChangeResponse> listForReview(IdentityChangeStatus status, Pageable pageable) {
        IdentityChangeStatus queue = status == null ? IdentityChangeStatus.PENDING : status;
        return changeRepository.findByStatus(queue, pageable).map(AirlineIdentityChangeMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AirlineIdentityChangeResponse getForReview(UUID requestId) {
        return AirlineIdentityChangeMapper.toResponse(getOrThrow(requestId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_BY_USER, allEntries = true),
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_DROPDOWN, allEntries = true)
    })
    public AirlineIdentityChangeResponse approve(UUID requestId, UUID reviewerUserId, String comment) {
        AirlineIdentityChangeRequest change = getOrThrow(requestId);
        requirePending(change);
        requireNotRequester(change, reviewerUserId);
        Airline airline = change.getAirline();
        requireActiveAirline(airline);
        requireCodesAvailable(change);

        if (change.getProposedLegalName() != null) airline.setLegalName(change.getProposedLegalName());
        if (change.getProposedRegistrationNumber() != null) airline.setRegistrationNumber(change.getProposedRegistrationNumber());
        if (change.getProposedIataCode() != null) airline.setIataCode(change.getProposedIataCode());
        if (change.getProposedIcaoCode() != null) airline.setIcaoCode(change.getProposedIcaoCode());
        airlineRepository.save(airline);

        return AirlineIdentityChangeMapper.toResponse(decide(change, IdentityChangeStatus.APPROVED, reviewerUserId, comment));
    }

    @Override
    @Transactional
    public AirlineIdentityChangeResponse reject(UUID requestId, UUID reviewerUserId, String comment) {
        if (!StringUtils.hasText(comment)) {
            throw new BadRequestException(ErrorMessageUtil.AIRLINE_IDENTITY_CHANGE_REJECTION_REASON_MANDATORY);
        }
        AirlineIdentityChangeRequest change = getOrThrow(requestId);
        requirePending(change);
        requireNotRequester(change, reviewerUserId);
        return AirlineIdentityChangeMapper.toResponse(decide(change, IdentityChangeStatus.REJECTED, reviewerUserId, comment));
    }

    // ---------- Helpers ----------

    private AirlineIdentityChangeRequest decide(AirlineIdentityChangeRequest change, IdentityChangeStatus outcome,
                                                UUID reviewerUserId, String comment) {
        change.setStatus(outcome);
        change.setReviewedByUserId(reviewerUserId);
        change.setReviewComment(comment);
        change.setReviewedAt(Instant.now());
        return changeRepository.save(change);
    }

    /** The proposed value, or null when it is blank or the same as the current one. */
    private static String changed(String proposed, String current) {
        if (!StringUtils.hasText(proposed)) {
            return null;
        }
        String value = proposed.strip();
        return Objects.equals(value, current) ? null : value;
    }

    private void requireCodesAvailable(AirlineIdentityChangeRequest change) {
        UUID airlineId = change.getAirline().getId();
        String iata = change.getProposedIataCode();
        if (iata != null && airlineRepository.findByIataCode(iata).filter(other -> !other.getId().equals(airlineId)).isPresent()) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRLINE_IATA_CODE_ALREADY_EXISTS, iata));
        }
        String icao = change.getProposedIcaoCode();
        if (icao != null && airlineRepository.findByIcaoCode(icao).filter(other -> !other.getId().equals(airlineId)).isPresent()) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRLINE_ICAO_CODE_ALREADY_EXISTS, icao));
        }
    }

    private static void requirePending(AirlineIdentityChangeRequest change) {
        if (change.getStatus() != IdentityChangeStatus.PENDING) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.AIRLINE_IDENTITY_CHANGE_NOT_PENDING, change.getId(), change.getStatus()));
        }
    }

    private static void requireNotRequester(AirlineIdentityChangeRequest change, UUID reviewerUserId) {
        if (change.getRequestedByUserId().equals(reviewerUserId)) {
            throw new ConflictException(ErrorMessageUtil.AIRLINE_IDENTITY_CHANGE_REVIEWER_IS_REQUESTER);
        }
    }

    private static void requireActiveAirline(Airline airline) {
        if (airline.getStatus() != AirlineStatus.ACTIVE) {
            throw new OperationNotPermittedException(String.format(
                    ErrorMessageUtil.AIRLINE_NOT_ACTIVE, airline.getId(), airline.getStatus()));
        }
    }

    private Airline getAirlineOrThrow(UUID airlineId) {
        return airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRLINE_NOT_FOUND_BY_ID, airlineId)));
    }

    private AirlineIdentityChangeRequest getOrThrow(UUID requestId) {
        return changeRepository.findById(requestId).orElseThrow(() -> notFound(requestId));
    }

    private static ResourceNotFoundException notFound(UUID requestId) {
        return new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRLINE_IDENTITY_CHANGE_NOT_FOUND, requestId));
    }
}

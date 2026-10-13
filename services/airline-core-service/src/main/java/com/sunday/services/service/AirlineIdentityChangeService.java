package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AirlineIdentityChangeRequestCreate;
import com.sunday.common_lib.payload.response.AirlineIdentityChangeResponse;
import com.sunday.services.enums.IdentityChangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Changes to an airline's verified identity fields: requested by the airline, decided by platform staff. */
public interface AirlineIdentityChangeService {

    AirlineIdentityChangeResponse request(UUID airlineId, AirlineIdentityChangeRequestCreate request, UUID userId);

    List<AirlineIdentityChangeResponse> listForAirline(UUID airlineId, UUID userId);

    AirlineIdentityChangeResponse cancel(UUID airlineId, UUID requestId, UUID userId);

    Page<AirlineIdentityChangeResponse> listForReview(IdentityChangeStatus status, Pageable pageable);

    AirlineIdentityChangeResponse getForReview(UUID requestId);

    AirlineIdentityChangeResponse approve(UUID requestId, UUID reviewerUserId, String comment);

    AirlineIdentityChangeResponse reject(UUID requestId, UUID reviewerUserId, String comment);
}

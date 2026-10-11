package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.response.FareResponse;

public interface FareLifecycleService {

    /** Permission-checked against the fare's own airline. Refuses any move other than DRAFT or WITHDRAWN to PUBLISHED. */
    FareResponse publishFare(UUID userId, UUID id);

    /** Permission-checked against the fare's own airline. Only a PUBLISHED fare can be withdrawn. */
    FareResponse withdrawFare(UUID userId, UUID id);
}

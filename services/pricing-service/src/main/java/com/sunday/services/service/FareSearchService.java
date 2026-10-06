package com.sunday.services.service;

import com.sunday.common_lib.payload.request.FareSearchRequest;
import com.sunday.common_lib.payload.response.FareSearchResult;

import java.util.List;

public interface FareSearchService {

    /** Cheapest sellable fare for each requested flight departure, in one database query per call. */
    List<FareSearchResult> search(FareSearchRequest request);
}

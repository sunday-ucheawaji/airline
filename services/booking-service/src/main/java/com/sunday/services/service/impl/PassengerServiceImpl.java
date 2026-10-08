package com.sunday.services.service.impl;

import com.sunday.common_lib.payload.request.PassengerRequest;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.services.mapper.PassengerMapper;
import com.sunday.services.model.Passenger;
import com.sunday.services.repository.PassengerRepository;
import com.sunday.services.service.PassengerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;

    @Override
    @Transactional
    public PassengerResponse createPassenger(PassengerRequest request, Long userId) {
        Passenger saved = passengerRepository.save(createPassengerEntity(request, userId));
        return PassengerMapper.toResponse(saved);
    }

    @Override
    public Passenger createPassengerEntity(PassengerRequest request, Long userId) {
        Passenger passenger = PassengerMapper.toEntity(request);
        passenger.setPrimaryUserId(userId);
        return passenger;
    }

    @Override
    public boolean existsById(Long id) {
        return passengerRepository.existsById(id);
    }

    @Override
    public long count() {
        return passengerRepository.count();
    }
}

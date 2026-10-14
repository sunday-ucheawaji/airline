package com.sunday.services.mapper;

import com.sunday.common_lib.payload.request.PassengerRequest;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.services.model.Passenger;

import java.util.UUID;

public class PassengerMapper {

    public static Passenger toEntity(PassengerRequest request) {
        return Passenger.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .passportNumber(request.getPassportNumber())
                .nationality(request.getNationality())
                .frequentFlyerNumber(request.getFrequentFlyerNumber())
                .seatInstanceId(request.getSeatInstanceId())
                .requiresWheelchairAssistance(request.getRequiresWheelchairAssistance())
                .dietaryPreferences(request.getDietaryPreferences())
                .medicalConditions(request.getMedicalConditions())
                .build();
    }

    public static void updateEntityFromRequest(PassengerRequest request, Passenger passenger) {
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setEmail(request.getEmail());
        passenger.setPhone(request.getPhone());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setGender(request.getGender());
        passenger.setPassportNumber(request.getPassportNumber());
        passenger.setNationality(request.getNationality());
        passenger.setFrequentFlyerNumber(request.getFrequentFlyerNumber());
        passenger.setRequiresWheelchairAssistance(request.getRequiresWheelchairAssistance());
        passenger.setDietaryPreferences(request.getDietaryPreferences());
        passenger.setMedicalConditions(request.getMedicalConditions());
    }

    /** The passenger's details as a request for a new booking, on the given seat (none for a lap infant). */
    public static PassengerRequest toRequest(Passenger passenger, UUID seatInstanceId) {
        return PassengerRequest.builder()
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .email(passenger.getEmail())
                .phone(passenger.getPhone())
                .dateOfBirth(passenger.getDateOfBirth())
                .gender(passenger.getGender())
                .passportNumber(passenger.getPassportNumber())
                .nationality(passenger.getNationality())
                .frequentFlyerNumber(passenger.getFrequentFlyerNumber())
                .seatInstanceId(seatInstanceId)
                .requiresWheelchairAssistance(passenger.getRequiresWheelchairAssistance())
                .dietaryPreferences(passenger.getDietaryPreferences())
                .medicalConditions(passenger.getMedicalConditions())
                .build();
    }

    /** The same passenger details on another seat (null for a lap infant). */
    public static PassengerRequest withSeat(PassengerRequest passenger, UUID seatInstanceId) {
        return PassengerRequest.builder()
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .email(passenger.getEmail())
                .phone(passenger.getPhone())
                .dateOfBirth(passenger.getDateOfBirth())
                .gender(passenger.getGender())
                .passportNumber(passenger.getPassportNumber())
                .nationality(passenger.getNationality())
                .frequentFlyerNumber(passenger.getFrequentFlyerNumber())
                .seatInstanceId(seatInstanceId)
                .requiresWheelchairAssistance(passenger.getRequiresWheelchairAssistance())
                .dietaryPreferences(passenger.getDietaryPreferences())
                .medicalConditions(passenger.getMedicalConditions())
                .build();
    }

    public static PassengerResponse toResponse(Passenger passenger) {
        return PassengerResponse.builder()
                .id(passenger.getId())
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .email(passenger.getEmail())
                .phone(passenger.getPhone())
                .dateOfBirth(passenger.getDateOfBirth())
                .gender(passenger.getGender())
                .passportNumber(passenger.getPassportNumber())
                .nationality(passenger.getNationality())
                .frequentFlyerNumber(passenger.getFrequentFlyerNumber())
                .seatInstanceId(passenger.getSeatInstanceId())
                .passengerType(passenger.getPassengerType())
                .lapInfant(passenger.isLapInfant())
                .fareId(passenger.getFareId())
                .fareAmount(passenger.getFareAmount())
                .primaryUserId(passenger.getPrimaryUserId())
                .requiresWheelchairAssistance(passenger.getRequiresWheelchairAssistance())
                .dietaryPreferences(passenger.getDietaryPreferences())
                .medicalConditions(passenger.getMedicalConditions())
                .isActive(passenger.getIsActive())
                .age(passenger.getAge())
                .isAdult(passenger.isAdult())
                .fullName(passenger.getFullName())
                .createdAt(passenger.getCreatedAt())
                .updatedAt(passenger.getUpdatedAt())
                .build();
    }
}

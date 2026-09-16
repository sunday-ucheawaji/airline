package com.sunday.services.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String firstName, String rawToken);
}

package com.sunday.services.service.impl;

import com.sunday.services.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * No real email provider is wired into this repo yet, so verification emails
 * are just logged. Swap this out for a real provider-backed implementation later.
 */
@Slf4j
@Service
public class LogEmailService implements EmailService {

    @Override
    public void sendVerificationEmail(String toEmail, String firstName, String rawToken) {
        log.info("Verification email for {} <{}> - verification token: {}", firstName, toEmail, rawToken);
    }
}

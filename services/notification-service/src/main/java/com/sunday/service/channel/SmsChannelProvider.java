package com.sunday.service.channel;

import com.sunday.service.model.NotificationChannel;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsChannelProvider extends ResilientChannelSupport implements NotificationChannelProvider {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @Value("${twilio.enabled:false}")
    private boolean enabled;

    public SmsChannelProvider(CircuitBreakerRegistry circuitBreakerRegistry, RateLimiterRegistry rateLimiterRegistry) {
        super("sms", circuitBreakerRegistry, rateLimiterRegistry);
    }

    @PostConstruct
    void init() {
        if (enabled && accountSid != null && !accountSid.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS channel initialised");
        } else {
            log.info("Twilio SMS disabled — set twilio.enabled=true and credentials to activate");
        }
    }

    @Override
    public NotificationChannel channelType() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public String send(String recipient, String subject, String body) throws Exception {
        return guarded(() -> Message.creator(new PhoneNumber(recipient), new PhoneNumber(fromNumber), body)
                .create()
                .getSid());
    }
}

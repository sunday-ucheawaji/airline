package com.sunday.service.listener;

import com.sunday.common_lib.event.EmailVerificationRequestedEvent;
import com.sunday.common_lib.event.ExistingAccountSignupAttemptedEvent;
import com.sunday.common_lib.event.PasswordResetRequestedEvent;
import com.sunday.service.content.AccountEmailContentBuilder;
import com.sunday.service.model.NotificationChannel;
import com.sunday.service.model.NotificationEventType;
import com.sunday.service.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/** Account emails from user-service. Email only; the token request id is the idempotency reference. */
@Component
@RequiredArgsConstructor
public class AccountEmailListener {

    private final NotificationDispatchService dispatchService;
    private final AccountEmailContentBuilder content;

    @KafkaListener(topics = EmailVerificationRequestedEvent.TOPIC, groupId = "notification-service-group")
    public void handleEmailVerificationRequested(@Payload EmailVerificationRequestedEvent event) {
        dispatchService.dispatch(NotificationEventType.EMAIL_VERIFICATION, event.getRequestId().toString(),
                NotificationChannel.EMAIL, event.getEmail(), content.subjectForEmailVerification(), content.bodyForEmailVerification(event));
    }

    @KafkaListener(topics = PasswordResetRequestedEvent.TOPIC, groupId = "notification-service-group")
    public void handlePasswordResetRequested(@Payload PasswordResetRequestedEvent event) {
        dispatchService.dispatch(NotificationEventType.PASSWORD_RESET, event.getRequestId().toString(),
                NotificationChannel.EMAIL, event.getEmail(), content.subjectForPasswordReset(), content.bodyForPasswordReset(event));
    }

    @KafkaListener(topics = ExistingAccountSignupAttemptedEvent.TOPIC, groupId = "notification-service-group")
    public void handleExistingAccountSignupAttempted(@Payload ExistingAccountSignupAttemptedEvent event) {
        dispatchService.dispatch(NotificationEventType.EXISTING_ACCOUNT_SIGNUP, event.getRequestId().toString(),
                NotificationChannel.EMAIL, event.getEmail(), content.subjectForExistingAccountSignup(), content.bodyForExistingAccountSignup(event));
    }
}

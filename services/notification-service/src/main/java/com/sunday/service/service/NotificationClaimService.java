package com.sunday.service.service;

import com.sunday.service.model.Notification;
import com.sunday.service.model.NotificationChannel;
import com.sunday.service.model.NotificationEventType;
import com.sunday.service.model.NotificationStatus;
import com.sunday.service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The claim-first idiom: isolated in its own REQUIRES_NEW transaction so a caught
 * DataIntegrityViolationException from a failed insert (a concurrent claim winning the race) never
 * risks a poisoned Hibernate session for whatever the caller does next — same rationale as
 * BookingSagaServiceImpl.tryStart (booking-service).
 */
@Service
@RequiredArgsConstructor
class NotificationClaimService {

    private final NotificationRepository notificationRepository;

    /** Returns null when a prior attempt already succeeded (true duplicate — skip); otherwise the
     *  claimed row (new, or a retryable PENDING/FAILED one) ready for a send attempt. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Notification claim(NotificationEventType eventType, String reference, NotificationChannel channel,
                        String recipient, String subject, String body) {
        Notification existing = notificationRepository.findByEventTypeAndReferenceAndChannel(eventType, reference, channel)
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == NotificationStatus.SENT) {
                return null;
            }
            existing.setAttemptCount(existing.getAttemptCount() + 1);
            existing.setSubject(subject);
            existing.setBody(body);
            return notificationRepository.save(existing);
        }

        Notification fresh = Notification.builder()
                .eventType(eventType).reference(reference).channel(channel)
                .recipient(recipient).subject(subject).body(body)
                .status(NotificationStatus.PENDING).attemptCount(1)
                .build();
        try {
            return notificationRepository.saveAndFlush(fresh);
        } catch (DataIntegrityViolationException e) {
            return notificationRepository.findByEventTypeAndReferenceAndChannel(eventType, reference, channel)
                    .filter(winner -> winner.getStatus() != NotificationStatus.SENT)
                    .orElse(null);
        }
    }
}

package com.sunday.service.controller;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.service.dto.NotificationDTO;
import com.sunday.service.dto.NotificationMapper;
import com.sunday.service.model.Notification;
import com.sunday.service.repository.NotificationRepository;
import com.sunday.service.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Both endpoints are platform-only (NOTIFICATION_SUPPORT_READ / NOTIFICATION_RESEND, enforced at the
 * gateway — see api-gateway SecurityConfig) — a notification has no airline owner of its own to check
 * membership against, same reasoning as BookingPermissions.BOOKING_SUPPORT_READ.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService dispatchService;

    @GetMapping("/support/booking/{reference}")
    public ResponseEntity<List<NotificationDTO>> getDeliveryHistory(@PathVariable String reference) {
        List<NotificationDTO> history = notificationRepository.findByReferenceOrderByCreatedAtDesc(reference)
                .stream().map(NotificationMapper::toDTO).toList();
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<NotificationDTO> resend(@PathVariable Long id) {
        Notification record = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.NOTIFICATION_NOT_FOUND_BY_ID, id)));
        return ResponseEntity.ok(NotificationMapper.toDTO(dispatchService.resend(record)));
    }
}

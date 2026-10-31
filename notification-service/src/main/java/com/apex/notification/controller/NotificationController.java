package com.apex.notification.controller;

import com.apex.common.dto.ApiResponse;
import com.apex.notification.entity.NotificationRecord;
import com.apex.notification.repository.NotificationRepository;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/recipient/{recipient}")
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getByRecipient(@PathVariable String recipient) {
        String corrId = MDC.get("correlationId");
        List<NotificationRecord> list = repository.findByRecipientIdentifierOrderByCreatedAtDesc(recipient);
        return ResponseEntity.ok(ApiResponse.ok("Customer notifications retrieved", list, corrId));
    }

    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getByReference(@PathVariable String referenceId) {
        String corrId = MDC.get("correlationId");
        List<NotificationRecord> list = repository.findByReferenceId(referenceId);
        return ResponseEntity.ok(ApiResponse.ok("Reference notifications retrieved", list, corrId));
    }
}

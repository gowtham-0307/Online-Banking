package com.apex.notification.entity;

import com.apex.common.enums.NotificationChannel;
import com.apex.common.enums.NotificationStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient", columnList = "recipient_identifier"),
        @Index(name = "idx_reference_id", columnList = "reference_id")
})
@EntityListeners(AuditingEntityListener.class)
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false, unique = true, length = 64)
    private String notificationId;

    @Column(name = "recipient_identifier", nullable = false, length = 128)
    private String recipientIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "message_body", nullable = false, length = 2048)
    private String messageBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public NotificationRecord() {
    }

    public NotificationRecord(Long id, String notificationId, String recipientIdentifier,
                              NotificationChannel channel, String subject, String messageBody,
                              NotificationStatus status, String referenceId, String correlationId,
                              Instant createdAt, Instant sentAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.recipientIdentifier = recipientIdentifier;
        this.channel = channel;
        this.subject = subject;
        this.messageBody = messageBody;
        this.status = status;
        this.referenceId = referenceId;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String notificationId;
        private String recipientIdentifier;
        private NotificationChannel channel;
        private String subject;
        private String messageBody;
        private NotificationStatus status;
        private String referenceId;
        private String correlationId;
        private Instant createdAt;
        private Instant sentAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder notificationId(String notificationId) {
            this.notificationId = notificationId;
            return this;
        }

        public Builder recipientIdentifier(String recipientIdentifier) {
            this.recipientIdentifier = recipientIdentifier;
            return this;
        }

        public Builder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder messageBody(String messageBody) {
            this.messageBody = messageBody;
            return this;
        }

        public Builder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder sentAt(Instant sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public NotificationRecord build() {
            return new NotificationRecord(id, notificationId, recipientIdentifier, channel, subject,
                    messageBody, status, referenceId, correlationId, createdAt, sentAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}

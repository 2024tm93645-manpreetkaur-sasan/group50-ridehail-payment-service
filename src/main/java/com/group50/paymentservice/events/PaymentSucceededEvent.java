package com.group50.paymentservice.events;

import java.time.Instant;

public record PaymentSucceededEvent(
        String paymentId,
        String tripId,
        double amount,
        String method,
        String status,
        String reference,
        Instant createdAt
) {}

package com.group50.paymentservice.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payments")
public record Payment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id,

        @Column(nullable = false, unique = true)
        String paymentId,

        @Column(nullable = false)
        String tripId,

        @Column(nullable = false)
        double amount,

        @Column(nullable = false)
        String method,

        @Column(nullable = false)
        String status,

        @Column(nullable = false, unique = true)
        String reference,

        Instant createdAt
) {}

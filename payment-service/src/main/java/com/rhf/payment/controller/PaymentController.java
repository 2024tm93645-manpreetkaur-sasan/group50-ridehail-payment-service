package com.rhf.payment.controller;

import com.rhf.payment.entity.Payment;
import com.rhf.payment.service.PaymentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/charge")
    @RateLimiter(name = "paymentChargeLimiter", fallbackMethod = "chargeRateLimited")
    public ResponseEntity<?> charge(@RequestBody Map<String, Object> body) {
        try {
            Long tripId = Long.valueOf(String.valueOf(body.get("trip_id")));
            BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
            String method = String.valueOf(body.getOrDefault("method", "CARD"));
            String reference = (String) body.getOrDefault("reference", null);
            String tripStatus = String.valueOf(body.get("status"));

            if (tripStatus == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Trip status is required for payment"));
            }

            // Payment allowed only for COMPLETED trips
            if (!"COMPLETED".equalsIgnoreCase(tripStatus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "FAILED", "message", "Payment allowed only for COMPLETED trips"));
            }

            Payment p = service.charge(tripId, amount, method, reference);

            if ("FAILED".equalsIgnoreCase(p.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "FAILED", "payment_id", p.getPaymentId(), "message", "Trip not completed or invalid"));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "SUCCESS", "payment_id", p.getPaymentId(), "trip_id", p.getTripId(), "amount", p.getAmount(), "method", p.getMethod()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    public ResponseEntity<?> chargeRateLimited(Map<String, Object> body, Throwable t) {
        return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded"));
    }

    @PatchMapping("/{id}/refund")
    @RateLimiter(name = "paymentRefundLimiter", fallbackMethod = "refundRateLimited")
    public ResponseEntity<?> refund(@PathVariable("id") Long id) {
        Payment p = service.refund(id);

        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Payment not found"));
        }

        if (!"REFUNDED".equalsIgnoreCase(p.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", p.getStatus(), "message", "Only successful payments can be refunded"));
        }

        return ResponseEntity.ok(Map.of("status", "REFUNDED", "payment_id", p.getPaymentId()));
    }

    public ResponseEntity<?> refundRateLimited(Long id, Throwable t) {
        return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded"));
    }


    @GetMapping("/trip/{tripId}")
    public ResponseEntity<?> getPaymentByTripId(@PathVariable("tripId") Long tripId) {
        try {
            Payment p = service.getPaymentByTripId(tripId);

            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No payment found for this trip"));
            }

            return ResponseEntity.ok(Map.of(
                    "payment_id", p.getPaymentId(),
                    "trip_id", p.getTripId(),
                    "amount", p.getAmount(),
                    "method", p.getMethod(),
                    "status", p.getStatus()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

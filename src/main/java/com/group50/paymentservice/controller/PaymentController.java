package com.group50.paymentservice.controller;

import com.group50.paymentservice.model.Payment;
import com.group50.paymentservice.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Idempotent endpoint — if same reference, same response
    @PutMapping("/charge")
    public ResponseEntity<Payment> charge(@RequestBody Payment p) {
        return ResponseEntity.ok(paymentService.create(p));
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<Payment> refund(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.refund(id));
    }

    @GetMapping
    public ResponseEntity<List<Payment>> list() {
        return ResponseEntity.ok(paymentService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> get(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.get(id));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<Payment>> byTrip(@PathVariable String tripId) {
        return ResponseEntity.ok(paymentService.byTrip(tripId));
    }
}

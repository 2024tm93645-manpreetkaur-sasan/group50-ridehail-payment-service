package com.rhf.payment.service;

import com.rhf.payment.entity.Payment;
import com.rhf.payment.repository.PaymentRepository;
import com.rhf.payment.util.PaymentHelper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repo;
    private final Counter successCounter;
    private final Counter failureCounter;

    public PaymentService(PaymentRepository repo, MeterRegistry meterRegistry) {
        this.repo = repo;
        this.successCounter = meterRegistry.counter("payment_success_total");
        this.failureCounter = meterRegistry.counter("payment_failed_total");
    }

    @Transactional
    public Payment charge(Long tripId, BigDecimal amount, String method, String reference) {

        // Idempotent reuse
        Optional<Payment> existing = PaymentHelper.checkIdempotentCharge(log, repo, reference);
        if (existing.isPresent()) return existing.get();
        successCounter.increment();
        return PaymentHelper.successCharge(log, repo, tripId, amount, method, reference);
    }

    @Transactional
    public Payment refund(Long paymentId) {

        return repo.findById(paymentId)
                .map(p -> PaymentHelper.processRefund(log, repo, p))
                .orElse(null);
    }

    public Payment getPaymentByTripId(Long tripId) {
        return repo.findTopByTripIdOrderByCreatedAtDesc(tripId);
    }
}

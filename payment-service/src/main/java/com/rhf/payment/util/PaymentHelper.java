package com.rhf.payment.util;

import com.rhf.payment.entity.Payment;
import com.rhf.payment.repository.PaymentRepository;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Optional;

import static com.rhf.payment.util.PaymentConstants.*;

public class PaymentHelper {

    private PaymentHelper() {
    }

    public static Optional<Payment> checkIdempotentCharge(Logger log, PaymentRepository repo, String reference) {
        if (reference == null || reference.isBlank()) return Optional.empty();

        return repo.findByReference(reference).map(existing -> {
            log.info("Idempotent CHARGE reference={} paymentId={}", reference, existing.getPaymentId());
            return existing;
        });
    }

    public static Payment successCharge(Logger log, PaymentRepository repo, Long tripId, BigDecimal amount, String method, String reference) {
        Payment p = new Payment();
        p.setTripId(tripId);
        p.setAmount(amount);
        p.setMethod(method);
        p.setReference(reference);
        p.setStatus(STATUS_SUCCESS);

        Payment saved = repo.save(p);
        log.info("Payment SUCCESS paymentId={} tripId={} amount={}", saved.getPaymentId(), tripId, amount);

        return saved;
    }


    public static Payment failedCharge(Logger log, PaymentRepository repo, Long tripId, BigDecimal amount, String method, String reference, String tripStatus) {
        log.warn("Charge FAILED tripId={} reason=Trip not completed (status: {})", tripId, tripStatus);

        Payment p = new Payment();
        p.setTripId(tripId);
        p.setAmount(amount);
        p.setMethod(method);
        p.setReference(reference);
        p.setStatus(STATUS_FAILED);

        return repo.save(p);
    }

    public static Payment processRefund(Logger log, PaymentRepository repo, Payment p) {

        if (STATUS_REFUNDED.equalsIgnoreCase(p.getStatus())) {
            log.info("Idempotent REFUND paymentId={}", p.getPaymentId());
            return p;
        }

        if (!STATUS_SUCCESS.equalsIgnoreCase(p.getStatus())) {
            log.warn("Refund rejected paymentId={} status={}", p.getPaymentId(), p.getStatus());
            return p;
        }

        p.setStatus(STATUS_REFUNDED);
        log.info("Payment REFUNDED paymentId={}", p.getPaymentId());
        return repo.save(p);
    }
}

package com.rhf.payment.repository;

import com.rhf.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReference(String reference);
    Payment findTopByTripIdOrderByCreatedAtDesc(Long tripId);

}

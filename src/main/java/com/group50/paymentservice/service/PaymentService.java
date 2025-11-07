package com.group50.paymentservice.service;

import com.group50.paymentservice.model.Payment;
import com.group50.paymentservice.repository.PaymentRepository;
import com.group50.paymentservice.events.PaymentSucceededEvent;
import com.group50.paymentservice.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository repo;
    private final RabbitTemplate rabbitTemplate;

    public PaymentService(PaymentRepository repo, RabbitTemplate rabbitTemplate) {
        this.repo = repo;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Idempotent by reference
    public Payment create(Payment input) {
        return repo.findByReference(input.reference())
                .orElseGet(() -> processNewPayment(input));
    }

    private Payment processNewPayment(Payment input) {
        Payment payment = new Payment(
                null,
                input.paymentId(),
                input.tripId(),
                input.amount(),
                input.method(),
                "SUCCESS",
                input.reference(),
                Instant.now()
        );

        Payment saved = repo.save(payment);

        // Publish PaymentSucceededEvent
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                saved.paymentId(),
                saved.tripId(),
                saved.amount(),
                saved.method(),
                saved.status(),
                saved.reference(),
                saved.createdAt()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );

        return saved;
    }

    public Payment refund(String id) {
        Optional<Payment> p = repo.findById(Long.parseLong(id));
        if (p.isEmpty()) throw new RuntimeException("Payment not found");

        Payment original = p.get();
        Payment refunded = new Payment(
                original.id(),
                original.paymentId(),
                original.tripId(),
                original.amount(),
                original.method(),
                "REFUNDED",
                original.reference(),
                Instant.now()
        );

        return repo.save(refunded);
    }

    public List<Payment> list() {
        return repo.findAll();
    }

    public Payment get(String id) {
        return repo.findById(Long.parseLong(id))
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public List<Payment> byTrip(String tripId) {
        return repo.findByTripId(tripId);
    }
}

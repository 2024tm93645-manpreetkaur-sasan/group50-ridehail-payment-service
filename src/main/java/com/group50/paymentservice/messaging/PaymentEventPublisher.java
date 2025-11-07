package com.group50.paymentservice.messaging;

import com.group50.paymentservice.events.PaymentSucceededEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentSucceeded(PaymentSucceededEvent event) {
        rabbitTemplate.convertAndSend("payment.exchange", "payment.success", event);
        System.out.println("Published event: " + event);
    }
}

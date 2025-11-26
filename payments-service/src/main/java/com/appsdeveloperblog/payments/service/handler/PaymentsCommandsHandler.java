package com.appsdeveloperblog.payments.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.appsdeveloperblog.core.dto.Payment;
import com.appsdeveloperblog.core.dto.commands.ProcessPaymentCommand;
import com.appsdeveloperblog.core.dto.events.PaymentFailedEvent;
import com.appsdeveloperblog.core.dto.events.PaymentProcessedEvent;
import com.appsdeveloperblog.core.exceptions.CreditCardProcessorUnavailableException;
import com.appsdeveloperblog.payments.service.PaymentService;

@Component
@KafkaListener(topics = {"${payments.commands.topic.name}"})
public class PaymentsCommandsHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @NonNull private final String paymentsEventsTopicName;
    
    public PaymentsCommandsHandler(
        PaymentService paymentService,
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${payments.events.topic.name}") @NonNull String paymentsEventsTopicName
    ) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentsEventsTopicName = paymentsEventsTopicName;
    }

    @KafkaHandler
    public void handleCommand(@Payload ProcessPaymentCommand command) {
        try {
            Payment payment = new Payment(
                command.getOrderid(),
                command.getProductId(),
                command.getProductPrice(),
                command.getProductQuantity()
            );
    
            Payment processedPayment = paymentService.process(payment);

            PaymentProcessedEvent paymentProcessedEvent = new PaymentProcessedEvent(
                processedPayment.getOrderId(),
                processedPayment.getProductId(),
                processedPayment.getProductPrice(),
                processedPayment.getProductQuantity()
            );

            kafkaTemplate.send(this.paymentsEventsTopicName, paymentProcessedEvent);
        } catch (CreditCardProcessorUnavailableException ccpue) {
            this.logger.error(ccpue.getLocalizedMessage(), ccpue);
            PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(
                command.getOrderid(),
                command.getProductId(),
                command.getProductQuantity()
            );
            kafkaTemplate.send(this.paymentsEventsTopicName, paymentFailedEvent);
            logger.info("Payment failed event sent to Kafka topic: " + this.paymentsEventsTopicName);
        }
    }
}

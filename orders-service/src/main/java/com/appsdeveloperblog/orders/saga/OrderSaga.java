package com.appsdeveloperblog.orders.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.appsdeveloperblog.core.dto.commands.ApprovedOrderCommand;
import com.appsdeveloperblog.core.dto.commands.CancelProductReservationCommand;
import com.appsdeveloperblog.core.dto.commands.ProcessPaymentCommand;
import com.appsdeveloperblog.core.dto.commands.RejectOrderCommand;
import com.appsdeveloperblog.core.dto.commons.ReserveProductCommand;
import com.appsdeveloperblog.core.dto.events.OrderApprovedEvent;
import com.appsdeveloperblog.core.dto.events.OrderCreatedEvent;
import com.appsdeveloperblog.core.dto.events.PaymentFailedEvent;
import com.appsdeveloperblog.core.dto.events.PaymentProcessedEvent;
import com.appsdeveloperblog.core.dto.events.ProductReservationCancelledEvent;
import com.appsdeveloperblog.core.dto.events.ProductReservedEvent;
import com.appsdeveloperblog.core.types.OrderStatus;
import com.appsdeveloperblog.orders.service.OrderHistoryService;


@Component
@KafkaListener(topics = {
    "${orders.events.topic.name}",
    "${products.events.topic.name}",
    "${payments.events.topic.name}"
})
public class OrderSaga {

    private final Logger logger = LoggerFactory.getLogger(OrderSaga.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderHistoryService orderHistoryService;
    @NonNull private final String productsCommandsTopicName;
    @NonNull private final String paymentsCommandsTopicName;
    @NonNull private final String ordersCommandsTopicName;

    public OrderSaga(
        KafkaTemplate<String, Object> kafkaTemplate,
        OrderHistoryService orderHistoryService,
        @Value("${products.commands.topic.name}") @NonNull String productsCommandsTopicName,
        @Value("${payments.commands.topic.name}") @NonNull String paymentsCommandsTopicName,
        @Value("${orders.commands.topic.name}") @NonNull String ordersCommandsTopicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.productsCommandsTopicName = productsCommandsTopicName;
        this.orderHistoryService = orderHistoryService;
        this.paymentsCommandsTopicName = paymentsCommandsTopicName;
        this.ordersCommandsTopicName = ordersCommandsTopicName;
    }

    @KafkaHandler
    public void handleEvent(@Payload OrderCreatedEvent event) {
        logger.info("Consuming order created event: {}", event);
        ReserveProductCommand command = new ReserveProductCommand(
            event.getProductId(),
            event.getProductQuantity(),
            event.getOrderId()
        );


        kafkaTemplate.send(productsCommandsTopicName, command);

        orderHistoryService.add(event.getOrderId(), OrderStatus.CREATED);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservedEvent event) {
        ProcessPaymentCommand processPaymentCommand = new ProcessPaymentCommand(
            event.getOrderId(),
            event.getProductId(),
            event.getProductPrice(),
            event.getProductQuantity()
        );

        kafkaTemplate.send(this.paymentsCommandsTopicName, processPaymentCommand);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentProcessedEvent event) {
        ApprovedOrderCommand approvedOrderCommand = new ApprovedOrderCommand(
            event.getOrderId()
        );

        kafkaTemplate.send(this.ordersCommandsTopicName, approvedOrderCommand);
    }

    @KafkaHandler
    public void handleEvent(@Payload OrderApprovedEvent event) {
        orderHistoryService.add(event.getOrderId(), OrderStatus.APPROVED);
        logger.info("Order approved event consumed: {}", event);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentFailedEvent event) {
        CancelProductReservationCommand cancelProductReservationCommand = new CancelProductReservationCommand(
            event.getProductId(),
            event.getOrderId(),
            event.getProductQuantity()
        );

        kafkaTemplate.send(this.productsCommandsTopicName, cancelProductReservationCommand);
        logger.info("Payment failed event consumed: {} and sending cancel product reservation command to Kafka topic: {}", event, this.productsCommandsTopicName);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservationCancelledEvent event) {
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(event.getOrderId());
        
        orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
        
        this.kafkaTemplate.send(this.ordersCommandsTopicName, rejectOrderCommand);
        logger.info("Reject order command sent to Kafka topic: " + this.ordersCommandsTopicName);
    }
}

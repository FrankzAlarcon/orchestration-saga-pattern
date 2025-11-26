package com.appsdeveloperblog.products.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.appsdeveloperblog.core.dto.Product;
import com.appsdeveloperblog.core.dto.commands.CancelProductReservationCommand;
import com.appsdeveloperblog.core.dto.commons.ReserveProductCommand;
import com.appsdeveloperblog.core.dto.events.ProductReservationCancelledEvent;
import com.appsdeveloperblog.core.dto.events.ProductReservationFailedEvent;
import com.appsdeveloperblog.core.dto.events.ProductReservedEvent;
import com.appsdeveloperblog.products.service.ProductService;

@Component
@KafkaListener(topics = {"${products.commands.topic.name}"})
public class ProductsCommandsHandler {

    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String productsEventsTopicName;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ProductsCommandsHandler(
        ProductService productService,
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${products.events.topic.name}") String productsEventsTopicName
    ) {
        this.productService = productService;
        this.kafkaTemplate = kafkaTemplate;
        this.productsEventsTopicName = productsEventsTopicName;
    }

    @KafkaHandler
    public void handleCommand(@Payload ReserveProductCommand command) {
        try {
            Product desiredProduct = new Product(command.getProductId(), command.getProductQuantity());
            Product reservedProduct = productService.reserve(desiredProduct, command.getOrderId());

            ProductReservedEvent productReservedEvent = new ProductReservedEvent(
                command.getOrderId(),
                command.getProductId(),
                reservedProduct.getPrice(),
                reservedProduct.getQuantity()
            );
            kafkaTemplate.send(this.productsEventsTopicName, productReservedEvent);
            logger.info("Product reserved event sent to Kafka topic: " + this.productsEventsTopicName);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            ProductReservationFailedEvent productReservationFailedEvent = new ProductReservationFailedEvent(
                command.getProductId(),
                command.getOrderId(),
                command.getProductQuantity()
            );
            kafkaTemplate.send(this.productsEventsTopicName, productReservationFailedEvent);
            logger.info("Product reservation failed event sent to Kafka topic: " + this.productsEventsTopicName);
        }
    }

    @KafkaHandler
    public void handleCommand(@Payload CancelProductReservationCommand command) {
        Product productToCancel = new Product(command.getProductId(), command.getProductQuantity());

        productService.cancelReservation(productToCancel, command.getOrderId());

        ProductReservationCancelledEvent productReservationCancelledEvent = new ProductReservationCancelledEvent(
            command.getProductId(),
            command.getOrderId()
        );

        this.kafkaTemplate.send(this.productsEventsTopicName, productReservationCancelledEvent);
    }
}

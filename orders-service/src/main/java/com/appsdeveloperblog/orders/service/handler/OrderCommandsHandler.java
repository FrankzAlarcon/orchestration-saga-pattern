package com.appsdeveloperblog.orders.service.handler;

import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.appsdeveloperblog.core.dto.commands.ApprovedOrderCommand;
import com.appsdeveloperblog.core.dto.commands.RejectOrderCommand;
import com.appsdeveloperblog.orders.service.OrderService;

@Component
@KafkaListener(topics = {"${orders.commands.topic.name}"})
public class OrderCommandsHandler {

    private final OrderService orderService;

    public OrderCommandsHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaHandler
    public void handleCommand(@Payload ApprovedOrderCommand command) {
        orderService.approveOrder(command.getOrderId());
    }

    @KafkaHandler
    public void handleCommand(@Payload RejectOrderCommand command) {
        orderService.rejectOrder(command.getOrderId());
    }
}

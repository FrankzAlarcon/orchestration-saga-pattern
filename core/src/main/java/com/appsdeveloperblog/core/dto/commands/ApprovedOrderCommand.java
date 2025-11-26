package com.appsdeveloperblog.core.dto.commands;

import java.util.UUID;

public class ApprovedOrderCommand {
    private UUID orderId;

    public ApprovedOrderCommand() {}

    public ApprovedOrderCommand(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return "ApprovedOrderCommand [orderId=" + orderId + "]";
    }
}

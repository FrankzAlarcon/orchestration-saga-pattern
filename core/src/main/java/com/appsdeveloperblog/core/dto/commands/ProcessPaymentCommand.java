package com.appsdeveloperblog.core.dto.commands;

import java.math.BigDecimal;
import java.util.UUID;

public class ProcessPaymentCommand {
    private UUID orderid;
    private UUID productId;
    private BigDecimal productPrice;
    private Integer productQuantity;

    public ProcessPaymentCommand() {}

    public ProcessPaymentCommand(UUID orderid, UUID productId, BigDecimal productPrice, Integer productQuantity) {
        this.orderid = orderid;
        this.productId = productId;
        this.productPrice = productPrice;
        this.productQuantity = productQuantity;
    }

    public UUID getOrderid() {
        return orderid;
    }

    public void setOrderid(UUID orderid) {
        this.orderid = orderid;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getProductQuantity() {
        return productQuantity;
    }

    public void setProductQuantity(Integer productQuantity) {
        this.productQuantity = productQuantity;
    }

    @Override
    public String toString() {
        return "ProcessPaymentCommand [orderid=" + orderid + ", productId=" + productId + ", productPrice=" + productPrice
                + ", productQuantity=" + productQuantity + "]";
    }
}

package com.verasotelomercedes.ordermanagement.order.domain;

public class ShippingRateException extends RuntimeException {

    public ShippingRateException(String message) {
        super(message);
    }

    public ShippingRateException(String message, Throwable cause) {
        super(message, cause);
    }
}

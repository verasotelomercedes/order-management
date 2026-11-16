package com.verasotelomercedes.ordermanagement.order.domain;

import java.math.BigDecimal;

public record ShippingRate(
        BigDecimal cost,
        String currency,
        Integer deliveryDays) {
}

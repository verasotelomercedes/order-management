package com.verasotelomercedes.ordermanagement.order.domain;

import java.math.BigDecimal;

public record ShippingRateRequest(
        String destinationPostalCode,
        BigDecimal weightKg,
        int numberOfPackages) {
}

package com.verasotelomercedes.ordermanagement.logistics.application;

import java.math.BigDecimal;

public record LogisticsRate(
        BigDecimal cost,
        String currency,
        Integer deliveryDays) {
}

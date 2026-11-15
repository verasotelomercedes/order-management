package com.verasotelomercedes.ordermanagement.logistics.application;

import java.math.BigDecimal;

public record LogisticsRateRequest(
        String destinationPostalCode,
        BigDecimal weightKg,
        int numberOfPackages) {
}

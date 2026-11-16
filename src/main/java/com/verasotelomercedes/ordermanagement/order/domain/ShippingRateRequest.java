package com.verasotelomercedes.ordermanagement.order.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ShippingRateRequest {
    private final String destinationPostalCode;
    private final BigDecimal weightKg;
    private final int numberOfPackages;
}

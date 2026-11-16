package com.verasotelomercedes.ordermanagement.order.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ShippingRate {
    private final BigDecimal cost;
    private final String currency;
    private final Integer deliveryDays;
}

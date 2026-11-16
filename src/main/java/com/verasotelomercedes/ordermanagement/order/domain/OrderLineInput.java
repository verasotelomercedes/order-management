package com.verasotelomercedes.ordermanagement.order.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderLineInput {
    private final String product;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal unitWeightKg;
}

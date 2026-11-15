package com.verasotelomercedes.ordermanagement.order.domain;

import java.math.BigDecimal;

public record OrderLineInput(
        String product,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal unitWeightKg) {
}

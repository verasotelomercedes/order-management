package com.verasotelomercedes.ordermanagement.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class OrderCalculator {
    public BigDecimal calculateProductAmount(List<OrderLine> lines) {
        return lines.stream()
                .map(OrderLine::calculateLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalAmount(BigDecimal productAmount, BigDecimal shippingCost) {
        BigDecimal shipping = shippingCost == null ? BigDecimal.ZERO : shippingCost;
        return productAmount.add(shipping).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalWeightKg(List<OrderLine> lines) {
        return lines.stream()
                .map(OrderLine::calculateTotalWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);
    }
}

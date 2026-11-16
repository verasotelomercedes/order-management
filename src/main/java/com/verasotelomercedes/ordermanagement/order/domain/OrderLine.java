package com.verasotelomercedes.ordermanagement.order.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class OrderLine {
    public static final int UNIT_PRICE_SCALE = 4;
    public static final int WEIGHT_SCALE = 3;

    private final Long id;
    private final String product;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal unitWeightKg;

    @Builder
    public OrderLine(Long id, String product, Integer quantity, BigDecimal unitPrice, BigDecimal unitWeightKg) {
        if (product == null || product.isBlank()) {
            throw new IllegalArgumentException("product must not be blank");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null || unitPrice.setScale(UNIT_PRICE_SCALE, RoundingMode.HALF_UP).signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be greater than zero");
        }
        if (unitWeightKg == null || unitWeightKg.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP).signum() <= 0) {
            throw new IllegalArgumentException("unitWeightKg must be greater than zero");
        }
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice.setScale(UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
        this.unitWeightKg = unitWeightKg.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateLineAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalWeightKg() {
        return unitWeightKg.multiply(BigDecimal.valueOf(quantity)).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }
}

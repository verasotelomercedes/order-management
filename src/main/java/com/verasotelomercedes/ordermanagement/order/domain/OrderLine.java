package com.verasotelomercedes.ordermanagement.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderLine {
    private final Long id;
    private final String product;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal unitWeightKg;

    public OrderLine(String product, Integer quantity, BigDecimal unitPrice, BigDecimal unitWeightKg) {
        this(null, product, quantity, unitPrice, unitWeightKg);
    }

    public OrderLine(Long id, String product, Integer quantity, BigDecimal unitPrice, BigDecimal unitWeightKg) {
        if (product == null || product.isBlank()) {
            throw new IllegalArgumentException("product must not be blank");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be greater than zero");
        }
        if (unitWeightKg == null || unitWeightKg.signum() <= 0) {
            throw new IllegalArgumentException("unitWeightKg must be greater than zero");
        }
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitWeightKg = unitWeightKg;
    }

    public BigDecimal calculateLineAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalWeightKg() {
        return unitWeightKg.multiply(BigDecimal.valueOf(quantity)).setScale(3, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getUnitWeightKg() {
        return unitWeightKg;
    }
}

package com.verasotelomercedes.ordermanagement.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCalculatorTest {

    private final OrderCalculator calculator = new OrderCalculator();

    @Test
    void productAmountSumsLineAmountsWithTwoDecimals() {
        List<OrderLine> lines = List.of(
                OrderLine.builder().product("A").quantity(3).unitPrice(new BigDecimal("0.10")).unitWeightKg(new BigDecimal("1")).build(),
                OrderLine.builder().product("B").quantity(2).unitPrice(new BigDecimal("19.99")).unitWeightKg(new BigDecimal("1")).build());

        BigDecimal amount = calculator.calculateProductAmount(lines);

        assertThat(amount).isEqualTo(new BigDecimal("40.28"));
    }

    @Test
    void lineAmountIsRoundedHalfUpBeforeSummingLines() {
        List<OrderLine> lines = List.of(
                OrderLine.builder().product("A").quantity(1).unitPrice(new BigDecimal("0.005")).unitWeightKg(new BigDecimal("1")).build(),
                OrderLine.builder().product("B").quantity(1).unitPrice(new BigDecimal("0.005")).unitWeightKg(new BigDecimal("1")).build());

        BigDecimal amount = calculator.calculateProductAmount(lines);

        assertThat(amount).isEqualTo(new BigDecimal("0.02"));
    }

    @Test
    void totalAmountAddsShippingCost() {
        BigDecimal total = calculator.calculateTotalAmount(new BigDecimal("32.52"), new BigDecimal("7.95"));

        assertThat(total).isEqualTo(new BigDecimal("40.47"));
    }

    @Test
    void totalAmountEqualsProductAmountWhenShippingIsUnknown() {
        BigDecimal total = calculator.calculateTotalAmount(new BigDecimal("32.52"), null);

        assertThat(total).isEqualTo(new BigDecimal("32.52"));
    }

    @Test
    void totalWeightSumsLinesWithThreeDecimals() {
        List<OrderLine> lines = List.of(
                OrderLine.builder().product("A").quantity(3).unitPrice(new BigDecimal("10.00")).unitWeightKg(new BigDecimal("0.500")).build(),
                OrderLine.builder().product("B").quantity(1).unitPrice(new BigDecimal("2.50")).unitWeightKg(new BigDecimal("1.0005")).build());

        BigDecimal weight = calculator.calculateTotalWeightKg(lines);

        assertThat(weight).isEqualTo(new BigDecimal("2.501"));
    }
}

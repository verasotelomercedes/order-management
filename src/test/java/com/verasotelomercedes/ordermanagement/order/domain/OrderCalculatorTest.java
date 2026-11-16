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
                new OrderLine("A", 3, new BigDecimal("0.10"), new BigDecimal("1")),
                new OrderLine("B", 2, new BigDecimal("19.99"), new BigDecimal("1")));

        BigDecimal amount = calculator.calculateProductAmount(lines);

        assertThat(amount).isEqualTo(new BigDecimal("40.28"));
    }

    @Test
    void lineAmountIsRoundedHalfUpBeforeSummingLines() {
        List<OrderLine> lines = List.of(
                new OrderLine("A", 1, new BigDecimal("0.005"), new BigDecimal("1")),
                new OrderLine("B", 1, new BigDecimal("0.005"), new BigDecimal("1")));

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
                new OrderLine("A", 3, new BigDecimal("10.00"), new BigDecimal("0.500")),
                new OrderLine("B", 1, new BigDecimal("2.50"), new BigDecimal("1.0005")));

        BigDecimal weight = calculator.calculateTotalWeightKg(lines);

        assertThat(weight).isEqualTo(new BigDecimal("2.501"));
    }
}

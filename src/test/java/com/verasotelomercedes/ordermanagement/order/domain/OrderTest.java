package com.verasotelomercedes.ordermanagement.order.domain;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Customer customer;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Jane Doe").email("jane@example.com").build();
        order = new Order(customer, new OrderInput(1L, "28001", List.of(
                new OrderLineInput("A", 3, new BigDecimal("10.005"), new BigDecimal("0.500")),
                new OrderLineInput("B", 1, new BigDecimal("2.50"), new BigDecimal("1.000")))));
    }

    @Test
    void newOrderIsPendingWithProductAmountAndTotalWithoutShipping() {
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(order.getProductAmount()).isEqualTo(new BigDecimal("32.52"));
        assertThat(order.getShippingCost()).isNull();
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("32.52"));
        assertThat(order.calculateTotalWeightKg()).isEqualTo(new BigDecimal("2.500"));
    }

    @Test
    void completingConfirmationAddsShippingCostToTotal() {
        order.startConfirmation();
        order.completeConfirmation(new BigDecimal("7.95"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getShippingCost()).isEqualTo(new BigDecimal("7.95"));
        assertThat(order.getProductAmount()).isEqualTo(new BigDecimal("32.52"));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("40.47"));
    }

    @Test
    void shippingCostIsRoundedHalfUpToTwoDecimals() {
        order.startConfirmation();
        order.completeConfirmation(new BigDecimal("7.955"));

        assertThat(order.getShippingCost()).isEqualTo(new BigDecimal("7.96"));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("40.48"));
    }

    @Test
    void addingALineRecalculatesAmounts() {
        order.addLine(OrderLine.builder().product("C").quantity(2).unitPrice(new BigDecimal("1.25")).unitWeightKg(new BigDecimal("0.100")).build());

        assertThat(order.getProductAmount()).isEqualTo(new BigDecimal("35.02"));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("35.02"));
        assertThat(order.calculateTotalWeightKg()).isEqualTo(new BigDecimal("2.700"));
    }

    @Test
    void cancellingConfirmationReturnsToPendingWithoutShippingCost() {
        order.startConfirmation();
        order.cancelConfirmation();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(order.getShippingCost()).isNull();
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("32.52"));
    }

    @Test
    void cannotStartConfirmationTwice() {
        order.startConfirmation();

        assertThatThrownBy(() -> order.startConfirmation())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotCompleteConfirmationWithoutStartingIt() {
        assertThatThrownBy(() -> order.completeConfirmation(new BigDecimal("7.95")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidPostalCodeAndEmptyLines() {
        assertThatThrownBy(() -> new Order(customer, new OrderInput(1L, "2800", List.of(
                new OrderLineInput("A", 1, BigDecimal.ONE, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Order(customer, new OrderInput(1L, "28001", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

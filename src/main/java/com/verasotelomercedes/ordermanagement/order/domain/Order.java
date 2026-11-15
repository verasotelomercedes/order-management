package com.verasotelomercedes.ordermanagement.order.domain;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Order {
    private static final OrderCalculator CALCULATOR = new OrderCalculator();

    private final Long id;
    private final Customer customer;
    private OrderStatus status;
    private final String destinationPostalCode;
    private BigDecimal productAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private final List<OrderLine> lines = new ArrayList<>();

    public Order(Customer customer, OrderInput input) {
        this.id = null;
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(input, "input must not be null");
        if (input.destinationPostalCode() == null || !input.destinationPostalCode().matches("\\d{5}")) {
            throw new IllegalArgumentException("destinationPostalCode must be exactly 5 digits");
        }
        if (input.lines() == null || input.lines().isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        this.destinationPostalCode = input.destinationPostalCode();
        this.status = OrderStatus.PENDING_CONFIRMATION;
        this.productAmount = new BigDecimal("0.00");
        this.totalAmount = new BigDecimal("0.00");
        this.shippingCost = null;
        input.lines().forEach(line -> addLine(
                new OrderLine(line.product(), line.quantity(), line.unitPrice(), line.unitWeightKg())));
    }

    private Order(Long id, Customer customer, OrderStatus status, String destinationPostalCode,
                  BigDecimal productAmount, BigDecimal shippingCost, BigDecimal totalAmount,
                  List<OrderLine> lines) {
        this.id = id;
        this.customer = customer;
        this.status = status;
        this.destinationPostalCode = destinationPostalCode;
        this.productAmount = productAmount;
        this.shippingCost = shippingCost;
        this.totalAmount = totalAmount;
        this.lines.addAll(lines);
    }

    public static Order restore(Long id, Customer customer, OrderStatus status, String destinationPostalCode,
                                BigDecimal productAmount, BigDecimal shippingCost, BigDecimal totalAmount,
                                List<OrderLine> lines) {
        return new Order(id, customer, status, destinationPostalCode,
                productAmount, shippingCost, totalAmount, lines);
    }

    public void addLine(OrderLine line) {
        Objects.requireNonNull(line, "line must not be null");
        lines.add(line);
        recalculateAmounts();
    }

    public BigDecimal calculateTotalWeightKg() {
        return CALCULATOR.calculateTotalWeightKg(lines);
    }

    public void startConfirmation() {
        requireStatus(OrderStatus.PENDING_CONFIRMATION);
        this.status = OrderStatus.CONFIRMING;
    }

    public void completeConfirmation(BigDecimal shippingCost) {
        requireStatus(OrderStatus.CONFIRMING);
        this.shippingCost = Objects.requireNonNull(shippingCost, "shippingCost must not be null")
                .setScale(2, RoundingMode.HALF_UP);
        recalculateAmounts();
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancelConfirmation() {
        requireStatus(OrderStatus.CONFIRMING);
        this.status = OrderStatus.PENDING_CONFIRMATION;
    }

    private void recalculateAmounts() {
        this.productAmount = CALCULATOR.calculateProductAmount(lines);
        this.totalAmount = CALCULATOR.calculateTotalAmount(productAmount, shippingCost);
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Order " + id + " must be " + expected + " but is " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getDestinationPostalCode() {
        return destinationPostalCode;
    }

    public BigDecimal getProductAmount() {
        return productAmount;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }
}

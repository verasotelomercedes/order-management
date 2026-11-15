package com.verasotelomercedes.ordermanagement.order.application;

import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRateRequest;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.domain.ports.outbound.OrderRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class OrderStateManager {

    private final OrderRepositoryPort orderRepository;

    public OrderStateManager(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogisticsRateRequest startConfirmation(Long orderId) {
        Order order = findOrder(orderId);
        order.startConfirmation();
        orderRepository.save(order);
        return new LogisticsRateRequest(
                order.getDestinationPostalCode(),
                order.calculateTotalWeightKg(),
                1);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelConfirmation(Long orderId) {
        Order order = findOrder(orderId);
        order.cancelConfirmation();
        orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order completeConfirmation(Long orderId, BigDecimal shippingCost) {
        Order order = findOrder(orderId);
        order.completeConfirmation(shippingCost);
        return orderRepository.save(order);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }
}

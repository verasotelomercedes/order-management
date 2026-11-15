package com.verasotelomercedes.ordermanagement.order.domain.ports.outbound;

import com.verasotelomercedes.ordermanagement.order.domain.Order;

import java.util.Optional;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(Long id);
}

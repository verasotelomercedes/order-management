package com.verasotelomercedes.ordermanagement.order.infrastructure.persistence;

import com.verasotelomercedes.ordermanagement.customer.infrastructure.persistence.CustomerJpaRepository;
import com.verasotelomercedes.ordermanagement.order.domain.ports.outbound.OrderRepositoryPort;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.Optional;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository orderJpaRepository;
    private final CustomerJpaRepository customerJpaRepository;
    private final OrderEntityMapper mapper;

    public OrderRepositoryAdapter(OrderJpaRepository orderJpaRepository,
                                  CustomerJpaRepository customerJpaRepository,
                                  OrderEntityMapper mapper) {
        this.orderJpaRepository = orderJpaRepository;
        this.customerJpaRepository = customerJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity;
        if (order.getId() == null) {
            entity = mapper.toEntity(order, customerJpaRepository.getReferenceById(order.getCustomer().getId()));
        } else {
            entity = orderJpaRepository.findById(order.getId())
                    .orElseThrow(() -> new NoSuchElementException("Order not found: " + order.getId()));
            mapper.updateState(entity, order);
        }
        return mapper.toDomain(orderJpaRepository.save(entity));
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id).map(mapper::toDomain);
    }
}

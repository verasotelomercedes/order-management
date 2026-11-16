package com.verasotelomercedes.ordermanagement.order.infrastructure.persistence;

import com.verasotelomercedes.ordermanagement.customer.infrastructure.persistence.CustomerEntity;
import com.verasotelomercedes.ordermanagement.customer.infrastructure.persistence.CustomerEntityMapper;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.domain.OrderLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEntityMapper {
    private final CustomerEntityMapper customerMapper;

    public OrderEntityMapper(CustomerEntityMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public OrderEntity toEntity(Order order, CustomerEntity customer) {
        OrderEntity entity = new OrderEntity();
        entity.setCustomer(customer);
        entity.setDestinationPostalCode(order.getDestinationPostalCode());
        updateState(entity, order);
        for (OrderLine line : order.getLines()) {
            OrderLineEntity lineEntity = new OrderLineEntity();
            lineEntity.setOrder(entity);
            lineEntity.setProduct(line.getProduct());
            lineEntity.setQuantity(line.getQuantity());
            lineEntity.setUnitPrice(line.getUnitPrice());
            lineEntity.setUnitWeightKg(line.getUnitWeightKg());
            entity.getLines().add(lineEntity);
        }
        return entity;
    }

    public void updateState(OrderEntity entity, Order order) {
        entity.setStatus(order.getStatus());
        entity.setProductAmount(order.getProductAmount());
        entity.setShippingCost(order.getShippingCost());
        entity.setTotalAmount(order.getTotalAmount());
    }

    public Order toDomain(OrderEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
                .map(line -> OrderLine.builder().id(line.getId()).product(line.getProduct()).quantity(line.getQuantity()).unitPrice(line.getUnitPrice()).unitWeightKg(line.getUnitWeightKg()).build())
                .toList();
        return Order.builder()
                .id(entity.getId())
                .customer(customerMapper.toDomain(entity.getCustomer()))
                .status(entity.getStatus())
                .destinationPostalCode(entity.getDestinationPostalCode())
                .productAmount(entity.getProductAmount())
                .shippingCost(entity.getShippingCost())
                .totalAmount(entity.getTotalAmount())
                .lines(lines)
                .build();
    }
}

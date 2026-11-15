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
                .map(line -> new OrderLine(line.getId(), line.getProduct(), line.getQuantity(),
                        line.getUnitPrice(), line.getUnitWeightKg()))
                .toList();
        return Order.restore(
                entity.getId(),
                customerMapper.toDomain(entity.getCustomer()),
                entity.getStatus(),
                entity.getDestinationPostalCode(),
                entity.getProductAmount(),
                entity.getShippingCost(),
                entity.getTotalAmount(),
                lines);
    }
}

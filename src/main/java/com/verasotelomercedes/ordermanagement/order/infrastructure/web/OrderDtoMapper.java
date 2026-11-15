package com.verasotelomercedes.ordermanagement.order.infrastructure.web;

import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.domain.OrderInput;
import com.verasotelomercedes.ordermanagement.order.domain.OrderLineInput;
import com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto.CreateOrderLineRequestDto;
import com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto.CreateOrderRequestDto;
import com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto.OrderResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderDtoMapper {
    public OrderInput toInput(CreateOrderRequestDto request) {
        validateFormat(request);
        List<OrderLineInput> lines = request.lines.stream()
                .map(line -> new OrderLineInput(line.product, line.quantity, line.unitPrice, line.unitWeightKg))
                .toList();
        return new OrderInput(request.customerId, request.destinationPostalCode, lines);
    }

    public OrderResponseDto toResponse(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getDestinationPostalCode(),
                order.getProductAmount(),
                order.getShippingCost(),
                order.getTotalAmount());
    }

    private static void validateFormat(CreateOrderRequestDto request) {
        require(request != null, "request body is required");
        require(request.customerId != null, "customerId is required");
        require(request.destinationPostalCode != null && request.destinationPostalCode.matches("\\d{5}"),
                "destinationPostalCode must be exactly 5 digits");
        require(request.lines != null && !request.lines.isEmpty(), "lines must not be empty");
        for (CreateOrderLineRequestDto line : request.lines) {
            require(line != null, "lines must not contain null elements");
            require(line.product != null && !line.product.isBlank(), "product is required");
            require(line.quantity != null, "quantity is required");
            require(line.unitPrice != null, "unitPrice is required");
            require(line.unitWeightKg != null, "unitWeightKg is required");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}

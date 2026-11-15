package com.verasotelomercedes.ordermanagement.order.infrastructure.web;

import com.verasotelomercedes.ordermanagement.order.application.OrderService;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto.CreateOrderRequestDto;
import com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto.OrderResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/pedidos")
public class OrderController {

    private final OrderService orderService;
    private final OrderDtoMapper mapper;

    public OrderController(OrderService orderService, OrderDtoMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@RequestBody CreateOrderRequestDto request) {
        Order order = orderService.create(mapper.toInput(request));
        return ResponseEntity.created(URI.create("/api/pedidos/" + order.getId())).body(mapper.toResponse(order));
    }

    @GetMapping("/{id}")
    public OrderResponseDto get(@PathVariable Long id) {
        return mapper.toResponse(orderService.get(id));
    }

    @PostMapping("/{id}/confirmacion")
    public OrderResponseDto confirm(@PathVariable Long id) {
        return mapper.toResponse(orderService.confirm(id));
    }
}

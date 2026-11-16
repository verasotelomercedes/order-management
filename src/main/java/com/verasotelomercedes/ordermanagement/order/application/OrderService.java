package com.verasotelomercedes.ordermanagement.order.application;

import com.verasotelomercedes.ordermanagement.customer.domain.ports.outbound.CustomerRepositoryPort;
import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.order.domain.ports.outbound.OrderRepositoryPort;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateException;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRate;
import com.verasotelomercedes.ordermanagement.order.domain.ports.outbound.ShippingRatePort;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateRequest;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.domain.OrderInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Slf4j
@Service
public class OrderService {
    private final OrderRepositoryPort orderRepository;
    private final CustomerRepositoryPort customerRepository;
    private final ShippingRatePort shippingRatePort;
    private final OrderStateManager orderStateManager;

    public OrderService(OrderRepositoryPort orderRepository,
                        CustomerRepositoryPort customerRepository,
                        ShippingRatePort shippingRatePort,
                        OrderStateManager orderStateManager) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.shippingRatePort = shippingRatePort;
        this.orderStateManager = orderStateManager;
    }

    @Transactional
    public Order create(OrderInput input) {
        Customer customer = customerRepository.findById(input.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + input.getCustomerId()));
        return orderRepository.save(new Order(customer, input));
    }

    @Transactional(readOnly = true)
    public Order get(Long orderId) {
        return findOrder(orderId);
    }

    public Order confirm(Long orderId) {
        ShippingRateRequest rateRequest = orderStateManager.startConfirmation(orderId);

        try {
            ShippingRate rate = shippingRatePort.requestRate(rateRequest);
            return orderStateManager.completeConfirmation(orderId, rate.getCost());
        } catch (RuntimeException e) {
            log.error("Confirmation of order {} failed, reverting it to PENDING_CONFIRMATION", orderId, e);
            orderStateManager.cancelConfirmation(orderId);
            throw e;
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }
}

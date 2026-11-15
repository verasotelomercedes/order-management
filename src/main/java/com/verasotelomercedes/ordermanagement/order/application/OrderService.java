package com.verasotelomercedes.ordermanagement.order.application;

import com.verasotelomercedes.ordermanagement.customer.domain.ports.outbound.CustomerRepositoryPort;
import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.order.domain.ports.outbound.OrderRepositoryPort;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsException;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRate;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRatePort;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRateRequest;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.domain.OrderInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class OrderService {
    private final OrderRepositoryPort orderRepository;
    private final CustomerRepositoryPort customerRepository;
    private final LogisticsRatePort logisticsRatePort;
    private final OrderStateManager orderStateManager;

    public OrderService(OrderRepositoryPort orderRepository,
                        CustomerRepositoryPort customerRepository,
                        LogisticsRatePort logisticsRatePort,
                        OrderStateManager orderStateManager) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.logisticsRatePort = logisticsRatePort;
        this.orderStateManager = orderStateManager;
    }

    @Transactional
    public Order create(OrderInput input) {
        Customer customer = customerRepository.findById(input.customerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + input.customerId()));
        return orderRepository.save(new Order(customer, input));
    }

    @Transactional(readOnly = true)
    public Order get(Long orderId) {
        return findOrder(orderId);
    }

    public Order confirm(Long orderId) {
        LogisticsRateRequest rateRequest = orderStateManager.startConfirmation(orderId);

        try {
            LogisticsRate rate = logisticsRatePort.requestRate(rateRequest);
            if (rate == null || rate.cost() == null) {
                throw new LogisticsException("Logistics provider returned no shipping cost");
            }
            return orderStateManager.completeConfirmation(orderId, rate.cost());
        } catch (RuntimeException e) {
            try {
                orderStateManager.cancelConfirmation(orderId);
            } catch (RuntimeException cancelError) {
                e.addSuppressed(cancelError);
            }
            throw e;
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }
}

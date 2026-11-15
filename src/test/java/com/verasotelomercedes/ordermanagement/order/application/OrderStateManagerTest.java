package com.verasotelomercedes.ordermanagement.order.application;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRateRequest;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.domain.OrderInput;
import com.verasotelomercedes.ordermanagement.order.domain.OrderLineInput;
import com.verasotelomercedes.ordermanagement.order.domain.OrderStatus;
import com.verasotelomercedes.ordermanagement.order.domain.ports.outbound.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStateManagerTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @InjectMocks
    private OrderStateManager orderStateManager;

    private Order order;

    @BeforeEach
    void setUp() {
        OrderInput input = new OrderInput(1L, "28001", List.of(
                new OrderLineInput("A", 2, new BigDecimal("10.00"), new BigDecimal("0.500")),
                new OrderLineInput("B", 1, new BigDecimal("5.00"), new BigDecimal("1.250"))));
        order = new Order(new Customer(1L, "Jane Doe", "jane@example.com"), input);
    }

    @Test
    void shouldMoveOrderToConfirmingAndBuildRateRequest() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        LogisticsRateRequest request = orderStateManager.startConfirmation(10L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMING);
        assertThat(request.destinationPostalCode()).isEqualTo("28001");
        assertThat(request.weightKg()).isEqualByComparingTo("2.250");
        assertThat(request.numberOfPackages()).isEqualTo(1);
        verify(orderRepository).save(order);
    }

    @Test
    void shouldNotStartConfirmationWhenOrderIsNotPending() {
        order.startConfirmation();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderStateManager.startConfirmation(10L))
                .isInstanceOf(IllegalStateException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldRevertOrderToPendingWhenCancelling() {
        order.startConfirmation();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderStateManager.cancelConfirmation(10L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        verify(orderRepository).save(order);
    }

    @Test
    void shouldStoreShippingCostAndConfirmOrder() {
        order.startConfirmation();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderStateManager.completeConfirmation(10L, new BigDecimal("4.995"));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getShippingCost()).isEqualByComparingTo("5.00");
        assertThat(result.getProductAmount()).isEqualByComparingTo("25.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldFailWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderStateManager.startConfirmation(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}

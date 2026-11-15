package com.verasotelomercedes.ordermanagement.order.application;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.customer.domain.ports.outbound.CustomerRepositoryPort;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsException;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRate;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRatePort;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private CustomerRepositoryPort customerRepository;

    @Mock
    private LogisticsRatePort logisticsRatePort;

    @Mock
    private OrderStateManager orderStateManager;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private OrderInput input;

    @BeforeEach
    void setUp() {
        customer = new Customer(1L, "Jane Doe", "jane@example.com");
        input = new OrderInput(1L, "28001", List.of(
                new OrderLineInput("A", 3, new BigDecimal("10.005"), new BigDecimal("0.500")),
                new OrderLineInput("B", 1, new BigDecimal("2.50"), new BigDecimal("1.000"))));
    }

    @Test
    void shouldCreatePendingOrderWithCalculatedAmounts() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.create(input);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getProductAmount()).isEqualByComparingTo("32.52");
        assertThat(order.getShippingCost()).isNull();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("32.52");
    }

    @Test
    void shouldNotCreateOrderWhenCustomerDoesNotExist() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(input))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("1");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldReturnExistingOrder() {
        Order order = new Order(customer, input);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThat(orderService.get(10L)).isSameAs(order);
    }

    @Test
    void shouldFailWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldConfirmOrderWithShippingCostFromProvider() {
        LogisticsRateRequest rateRequest = new LogisticsRateRequest("28001", new BigDecimal("2.500"), 1);
        LogisticsRate rate = new LogisticsRate(new BigDecimal("5.00"), "EUR", 2);
        Order confirmed = new Order(customer, input);
        when(orderStateManager.startConfirmation(10L)).thenReturn(rateRequest);
        when(logisticsRatePort.requestRate(rateRequest)).thenReturn(rate);
        when(orderStateManager.completeConfirmation(10L, new BigDecimal("5.00"))).thenReturn(confirmed);

        Order result = orderService.confirm(10L);

        assertThat(result).isSameAs(confirmed);
        verify(orderStateManager, never()).cancelConfirmation(any());
    }

    @Test
    void shouldCancelConfirmationWhenProviderFails() {
        LogisticsRateRequest rateRequest = new LogisticsRateRequest("28001", new BigDecimal("2.500"), 1);
        LogisticsException failure = new LogisticsException("provider down");
        when(orderStateManager.startConfirmation(10L)).thenReturn(rateRequest);
        when(logisticsRatePort.requestRate(rateRequest)).thenThrow(failure);

        assertThatThrownBy(() -> orderService.confirm(10L)).isSameAs(failure);

        verify(orderStateManager).cancelConfirmation(10L);
        verify(orderStateManager, never()).completeConfirmation(any(), any());
    }

    @Test
    void shouldCancelConfirmationWhenProviderReturnsNoRate() {
        LogisticsRateRequest rateRequest = new LogisticsRateRequest("28001", new BigDecimal("2.500"), 1);
        when(orderStateManager.startConfirmation(10L)).thenReturn(rateRequest);
        when(logisticsRatePort.requestRate(rateRequest)).thenReturn(null);

        assertThatThrownBy(() -> orderService.confirm(10L))
                .isInstanceOf(LogisticsException.class)
                .hasMessageContaining("no shipping cost");

        verify(orderStateManager).cancelConfirmation(10L);
    }

    @Test
    void shouldCancelConfirmationWhenProviderReturnsRateWithoutCost() {
        LogisticsRateRequest rateRequest = new LogisticsRateRequest("28001", new BigDecimal("2.500"), 1);
        when(orderStateManager.startConfirmation(10L)).thenReturn(rateRequest);
        when(logisticsRatePort.requestRate(rateRequest)).thenReturn(new LogisticsRate(null, "EUR", 2));

        assertThatThrownBy(() -> orderService.confirm(10L)).isInstanceOf(LogisticsException.class);

        verify(orderStateManager).cancelConfirmation(10L);
    }

    @Test
    void shouldCancelConfirmationWhenStoringTheResultFails() {
        LogisticsRateRequest rateRequest = new LogisticsRateRequest("28001", new BigDecimal("2.500"), 1);
        LogisticsRate rate = new LogisticsRate(new BigDecimal("5.00"), "EUR", 2);
        IllegalStateException failure = new IllegalStateException("database error");
        when(orderStateManager.startConfirmation(10L)).thenReturn(rateRequest);
        when(logisticsRatePort.requestRate(rateRequest)).thenReturn(rate);
        when(orderStateManager.completeConfirmation(10L, new BigDecimal("5.00"))).thenThrow(failure);

        assertThatThrownBy(() -> orderService.confirm(10L)).isSameAs(failure);

        verify(orderStateManager).cancelConfirmation(10L);
    }

    @Test
    void shouldKeepOriginalErrorWhenCancellingAlsoFails() {
        LogisticsRateRequest rateRequest = new LogisticsRateRequest("28001", new BigDecimal("2.500"), 1);
        LogisticsException failure = new LogisticsException("provider down");
        IllegalStateException cancelFailure = new IllegalStateException("cannot cancel");
        when(orderStateManager.startConfirmation(10L)).thenReturn(rateRequest);
        when(logisticsRatePort.requestRate(rateRequest)).thenThrow(failure);
        doThrow(cancelFailure).when(orderStateManager).cancelConfirmation(10L);

        assertThatThrownBy(() -> orderService.confirm(10L))
                .isSameAs(failure)
                .hasSuppressedException(cancelFailure);
    }

    @Test
    void shouldNotCallProviderWhenConfirmationCannotStart() {
        IllegalStateException failure = new IllegalStateException("already confirmed");
        when(orderStateManager.startConfirmation(10L)).thenThrow(failure);

        assertThatThrownBy(() -> orderService.confirm(10L)).isSameAs(failure);

        verify(logisticsRatePort, never()).requestRate(any());
        verify(orderStateManager, never()).cancelConfirmation(any());
    }
}

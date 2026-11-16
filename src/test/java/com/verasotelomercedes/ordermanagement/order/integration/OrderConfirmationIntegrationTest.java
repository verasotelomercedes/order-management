package com.verasotelomercedes.ordermanagement.order.integration;

import com.sun.net.httpserver.HttpServer;
import com.verasotelomercedes.ordermanagement.customer.application.CustomerService;
import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.order.application.OrderService;
import com.verasotelomercedes.ordermanagement.order.domain.Order;
import com.verasotelomercedes.ordermanagement.order.domain.OrderInput;
import com.verasotelomercedes.ordermanagement.order.domain.OrderLineInput;
import com.verasotelomercedes.ordermanagement.order.domain.OrderStatus;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "logistics.token=testToken",
        "logistics.connect-timeout=1s",
        "logistics.read-timeout=1s",
        "logistics.max-retries=2",
        "logistics.retry-backoff=10ms"
})
class OrderConfirmationIntegrationTest {

    private static final HttpServer PROVIDER = startProvider();
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static volatile int status = 200;
    private static volatile long delayMillis = 0;
    private static volatile int failFirstCalls = 0;

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("logistics.base-url", () -> "http://localhost:" + PROVIDER.getAddress().getPort());
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @BeforeEach
    void resetProvider() {
        CALLS.set(0);
        status = 200;
        delayMillis = 0;
        failFirstCalls = 0;
    }

    @AfterAll
    static void stopProvider() {
        PROVIDER.stop(0);
    }

    @Test
    void confirmsOrderAndStoresShippingCostWhenProviderResponds() {
        Order order = createOrder();

        Order confirmed = orderService.confirm(order.getId());

        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.getShippingCost()).isEqualTo(new BigDecimal("7.95"));
        assertThat(confirmed.getTotalAmount()).isEqualTo(new BigDecimal("40.47"));
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void recoversFromIntermittent503WithRetries() {
        failFirstCalls = 2;
        Order order = createOrder();

        Order confirmed = orderService.confirm(order.getId());

        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(CALLS.get()).isEqualTo(3);
    }

    @Test
    void leavesOrderPendingWhenProviderKeepsFailingWith503() {
        status = 503;
        Order order = createOrder();

        assertThatThrownBy(() -> orderService.confirm(order.getId()))
                .isInstanceOf(ShippingRateException.class);

        Order reloaded = orderService.get(order.getId());
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(reloaded.getShippingCost()).isNull();
        assertThat(reloaded.getTotalAmount()).isEqualTo(new BigDecimal("32.52"));
        assertThat(CALLS.get()).isEqualTo(3);
    }

    @Test
    void leavesOrderPendingWhenProviderTimesOut() {
        delayMillis = 1500;
        Order order = createOrder();

        assertThatThrownBy(() -> orderService.confirm(order.getId()))
                .isInstanceOf(ShippingRateException.class)
                .hasMessageContaining("timed out");

        assertThat(orderService.get(order.getId()).getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
    }

    @Test
    void orderCanBeConfirmedAgainAfterAProviderFailure() {
        status = 503;
        Order order = createOrder();
        assertThatThrownBy(() -> orderService.confirm(order.getId()))
                .isInstanceOf(ShippingRateException.class);

        status = 200;
        Order confirmed = orderService.confirm(order.getId());

        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void simultaneousConfirmationsCallTheProviderOnlyOnce() throws Exception {
        delayMillis = 500;
        Order order = createOrder();
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<Order>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Order> task = () -> {
                ready.countDown();
                go.await();
                return orderService.confirm(order.getId());
            };
            futures.add(executor.submit(task));
        }
        ready.await();
        go.countDown();

        int successes = 0;
        List<Throwable> failures = new ArrayList<>();
        for (Future<Order> future : futures) {
            try {
                future.get();
                successes++;
            } catch (java.util.concurrent.ExecutionException e) {
                failures.add(e.getCause());
            }
        }
        executor.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(failures).hasSize(threads - 1)
                .allSatisfy(failure -> assertThat(failure)
                        .isInstanceOfAny(IllegalStateException.class, ObjectOptimisticLockingFailureException.class));
        assertThat(CALLS.get()).isEqualTo(1);

        Order reloaded = orderService.get(order.getId());
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(reloaded.getShippingCost()).isEqualTo(new BigDecimal("7.95"));
        assertThat(reloaded.getTotalAmount()).isEqualTo(new BigDecimal("40.47"));
    }

    private Order createOrder() {
        Customer customer = customerService.create(
                Customer.builder().name("Jane Doe").email(UUID.randomUUID() + "@example.com").build());
        return orderService.create(new OrderInput(customer.getId(), "43840", List.of(
                new OrderLineInput("A", 3, new BigDecimal("10.005"), new BigDecimal("0.500")),
                new OrderLineInput("B", 1, new BigDecimal("2.50"), new BigDecimal("1.000")))));
    }

    private static HttpServer startProvider() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/api/tarifas", exchange -> {
                int call = CALLS.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                int responseStatus = call <= failFirstCalls ? 503 : status;
                byte[] payload = responseStatus == 200
                        ? "{\"coste\":7.95,\"moneda\":\"EUR\",\"plazoDias\":2}".getBytes(StandardCharsets.UTF_8)
                        : new byte[0];
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, payload.length == 0 ? -1 : payload.length);
                if (payload.length > 0) {
                    exchange.getResponseBody().write(payload);
                }
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

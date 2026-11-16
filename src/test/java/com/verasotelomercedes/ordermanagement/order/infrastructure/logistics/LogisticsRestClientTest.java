package com.verasotelomercedes.ordermanagement.order.infrastructure.logistics;

import com.sun.net.httpserver.HttpServer;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateException;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRate;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogisticsRestClientTest {

    private static final ShippingRateRequest REQUEST =
            new ShippingRateRequest("43840", new BigDecimal("3.5"), 2);

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<Integer> statuses = new ArrayList<>();
    private volatile long delayMillis = 0;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/tarifas", exchange -> {
            int call = calls.getAndIncrement();
            authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int status = statuses.get(Math.min(call, statuses.size() - 1));
            byte[] payload = status == 200
                    ? "{\"coste\":7.95,\"moneda\":\"EUR\",\"plazoDias\":2}".getBytes(StandardCharsets.UTF_8)
                    : new byte[0];
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                exchange.getResponseBody().write(payload);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    private LogisticsRestClient client(String token, int maxRetries, Duration readTimeout) {
        RestClient restClient = new LogisticsConfiguration().logisticsHttpClient(
                "http://localhost:" + server.getAddress().getPort(),
                token, Duration.ofSeconds(1), readTimeout);
        return new LogisticsRestClient(restClient, maxRetries, Duration.ofMillis(10));
    }

    @Test
    void returnsRateAndSendsContractPayloadWithBearerToken() {
        statuses.add(200);

        ShippingRate rate = client("secret", 2, Duration.ofSeconds(2)).requestRate(REQUEST);

        assertThat(rate.cost()).isEqualByComparingTo("7.95");
        assertThat(rate.currency()).isEqualTo("EUR");
        assertThat(rate.deliveryDays()).isEqualTo(2);
        assertThat(authHeaders).containsExactly("Bearer secret");
        assertThat(bodies.get(0))
                .contains("\"codigoPostalDestino\":\"43840\"")
                .contains("\"pesoKg\":3.5")
                .contains("\"numBultos\":2");
    }

    @Test
    void retriesOnIntermittent503ThenSucceeds() {
        statuses.add(503);
        statuses.add(503);
        statuses.add(200);

        ShippingRate rate = client("secret", 2, Duration.ofSeconds(2)).requestRate(REQUEST);

        assertThat(rate.cost()).isEqualByComparingTo("7.95");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void failsAfterExhaustingRetriesOn503() {
        statuses.add(503);

        assertThatThrownBy(() -> client("secret", 2, Duration.ofSeconds(2)).requestRate(REQUEST))
                .isInstanceOf(ShippingRateException.class);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryOtherErrorStatuses() {
        statuses.add(401);

        assertThatThrownBy(() -> client("secret", 2, Duration.ofSeconds(2)).requestRate(REQUEST))
                .isInstanceOf(ShippingRateException.class)
                .hasMessageContaining("401");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void failsWithShippingRateExceptionOnTimeout() {
        statuses.add(200);
        delayMillis = 600;

        assertThatThrownBy(() -> client("secret", 0, Duration.ofMillis(150)).requestRate(REQUEST))
                .isInstanceOf(ShippingRateException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void failsWithoutRetryingWhenProviderReturnsNoShippingCost() {
        statuses.add(204);

        assertThatThrownBy(() -> client("secret", 2, Duration.ofSeconds(2)).requestRate(REQUEST))
                .isInstanceOf(ShippingRateException.class)
                .hasMessageContaining("no shipping cost");
        assertThat(calls.get()).isEqualTo(1);
    }
}

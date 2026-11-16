package com.verasotelomercedes.ordermanagement.order.infrastructure.logistics;

import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateException;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRate;
import com.verasotelomercedes.ordermanagement.order.domain.ports.outbound.ShippingRatePort;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateRequest;
import com.verasotelomercedes.ordermanagement.order.infrastructure.logistics.dto.RateRequestDto;
import com.verasotelomercedes.ordermanagement.order.infrastructure.logistics.dto.RateResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class LogisticsRestClient implements ShippingRatePort {

    private final RestClient restClient;
    private final int maxRetries;
    private final Duration retryBackoff;

    public LogisticsRestClient(
            @Qualifier("logisticsHttpClient") RestClient restClient,
            @Value("${logistics.max-retries}") int maxRetries,
            @Value("${logistics.retry-backoff}") Duration retryBackoff) {
        this.restClient = restClient;
        this.maxRetries = maxRetries;
        this.retryBackoff = retryBackoff;
    }

    @Override
    public ShippingRate requestRate(ShippingRateRequest request) {
        RateResponseDto response = callWithRetries(new RateRequestDto(
                request.getDestinationPostalCode(), request.getWeightKg(), request.getNumberOfPackages()));
        if (response == null || response.getCost() == null) {
            throw new ShippingRateException("Logistics provider returned no shipping cost");
        }
        return new ShippingRate(response.getCost(), response.getCurrency(), response.getDeliveryDays());
    }

    private RateResponseDto callWithRetries(RateRequestDto body) {
        int attempt = 1;
        while (true) {
            boolean retriesExhausted = attempt > maxRetries;
            try {
                return call(body);
            } catch (ResourceAccessException e) {
                if (retriesExhausted) {
                    throw new ShippingRateException(
                            "Logistics provider unreachable or timed out after " + attempt + " attempts", e);
                }
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean serviceUnavailable = status == HttpStatus.SERVICE_UNAVAILABLE.value();
                if (!serviceUnavailable || retriesExhausted) {
                    throw new ShippingRateException(
                            "Logistics provider responded with status " + status + " after " + attempt + " attempt(s)", e);
                }
            }
            pause();
            attempt++;
        }
    }

    private RateResponseDto call(RateRequestDto body) {
        return restClient.post()
                .uri("/api/tarifas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RateResponseDto.class);
    }

    private void pause() {
        try {
            Thread.sleep(retryBackoff.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShippingRateException("Interrupted while waiting to retry the logistics provider", e);
        }
    }
}

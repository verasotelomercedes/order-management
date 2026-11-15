package com.verasotelomercedes.ordermanagement.order.domain;

import java.util.List;

public record OrderInput(
        Long customerId,
        String destinationPostalCode,
        List<OrderLineInput> lines) {
}

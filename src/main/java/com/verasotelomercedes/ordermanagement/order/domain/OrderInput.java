package com.verasotelomercedes.ordermanagement.order.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderInput {
    private final Long customerId;
    private final String destinationPostalCode;
    private final List<OrderLineInput> lines;
}

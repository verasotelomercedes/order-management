package com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequestDto {

    private static final long serialVersionUID = 1L;

    public Long customerId;
    public String destinationPostalCode;
    public List<CreateOrderLineRequestDto> lines;
}

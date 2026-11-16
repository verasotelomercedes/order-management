package com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderLineRequestDto {

    private static final long serialVersionUID = 1L;

    public String product;
    public Integer quantity;
    public BigDecimal unitPrice;
    public BigDecimal unitWeightKg;
}

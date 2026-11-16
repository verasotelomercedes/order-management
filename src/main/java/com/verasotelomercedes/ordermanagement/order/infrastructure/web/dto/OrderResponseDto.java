package com.verasotelomercedes.ordermanagement.order.infrastructure.web.dto;

import com.verasotelomercedes.ordermanagement.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private static final long serialVersionUID = 1L;

    public Long id;
    public Long customerId;
    public OrderStatus status;
    public String destinationPostalCode;
    public BigDecimal productAmount;
    public BigDecimal shippingCost;
    public BigDecimal totalAmount;
}

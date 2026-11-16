package com.verasotelomercedes.ordermanagement.order.infrastructure.logistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateResponseDto {

    @JsonProperty("coste")
    public BigDecimal cost;

    @JsonProperty("moneda")
    public String currency;

    @JsonProperty("plazoDias")
    public Integer deliveryDays;
}

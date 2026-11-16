package com.verasotelomercedes.ordermanagement.order.infrastructure.logistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateRequestDto {

    @JsonProperty("codigoPostalDestino")
    public String destinationPostalCode;

    @JsonProperty("pesoKg")
    public BigDecimal weightKg;

    @JsonProperty("numBultos")
    public int numberOfPackages;
}

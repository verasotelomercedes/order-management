package com.verasotelomercedes.ordermanagement.customer.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequestDto {

    private static final long serialVersionUID = 1L;

    public String name;
    public String email;
}

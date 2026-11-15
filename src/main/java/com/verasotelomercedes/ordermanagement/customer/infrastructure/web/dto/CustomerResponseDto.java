package com.verasotelomercedes.ordermanagement.customer.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public Long id;
    public String name;
    public String email;
}

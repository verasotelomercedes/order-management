package com.verasotelomercedes.ordermanagement.customer.infrastructure.web;

import com.verasotelomercedes.ordermanagement.customer.application.CustomerService;
import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.customer.infrastructure.web.dto.CreateCustomerRequestDto;
import com.verasotelomercedes.ordermanagement.customer.infrastructure.web.dto.CustomerResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/clientes")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponseDto> create(@RequestBody CreateCustomerRequestDto request) {
        Customer customer = customerService.create(Customer.builder().name(request.name).email(request.email).build());
        return ResponseEntity.created(URI.create("/api/clientes/" + customer.getId())).body(toResponse(customer));
    }

    @GetMapping("/{id}")
    public CustomerResponseDto get(@PathVariable Long id) {
        return toResponse(customerService.get(id));
    }

    private static CustomerResponseDto toResponse(Customer customer) {
        return new CustomerResponseDto(customer.getId(), customer.getName(), customer.getEmail());
    }
}

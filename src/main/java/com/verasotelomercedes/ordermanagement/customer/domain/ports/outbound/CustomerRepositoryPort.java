package com.verasotelomercedes.ordermanagement.customer.domain.ports.outbound;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;

import java.util.Optional;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    boolean existsByEmail(String email);
}

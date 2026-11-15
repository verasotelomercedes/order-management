package com.verasotelomercedes.ordermanagement.customer.application;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.customer.domain.ports.outbound.CustomerRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CustomerService {

    private final CustomerRepositoryPort customerRepository;

    public CustomerService(CustomerRepositoryPort customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer create(Customer customer) {
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new IllegalStateException("A customer with email " + customer.getEmail() + " already exists");
        }
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer get(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
    }
}

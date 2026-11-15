package com.verasotelomercedes.ordermanagement.customer.infrastructure.persistence;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerEntityMapper {

    public CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.getId());
        entity.setName(customer.getName());
        entity.setEmail(customer.getEmail());
        return entity;
    }

    public Customer toDomain(CustomerEntity entity) {
        return new Customer(entity.getId(), entity.getName(), entity.getEmail());
    }
}

package com.verasotelomercedes.ordermanagement.customer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    boolean existsByEmail(String email);
}

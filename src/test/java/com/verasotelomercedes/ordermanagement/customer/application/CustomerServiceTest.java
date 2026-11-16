package com.verasotelomercedes.ordermanagement.customer.application;

import com.verasotelomercedes.ordermanagement.customer.domain.Customer;
import com.verasotelomercedes.ordermanagement.customer.domain.ports.outbound.CustomerRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldSaveNewCustomer() {
        Customer customer = Customer.builder().name("Jane Doe").email("jane@example.com").build();
        Customer saved = Customer.builder().id(1L).name("Jane Doe").email("jane@example.com").build();
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(customerRepository.save(customer)).thenReturn(saved);

        Customer result = customerService.create(customer);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void shouldRejectCustomerWithExistingEmail() {
        Customer customer = Customer.builder().name("Jane Doe").email("jane@example.com").build();
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(customer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jane@example.com");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldReturnExistingCustomer() {
        Customer customer = Customer.builder().id(1L).name("Jane Doe").email("jane@example.com").build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThat(customerService.get(1L)).isSameAs(customer);
    }

    @Test
    void shouldFailWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.get(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}

package com.smartmall.tlai.business.customer.application;

import com.smartmall.tlai.business.customer.domain.Customer;
import com.smartmall.tlai.business.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock CustomerRepository repository;

    @Test
    void updateCanClearOptionalContactFields() {
        Customer existing = new Customer(7L, "旧名称", "123456789", "old@example.com",
                OffsetDateTime.parse("2030-01-01T00:00:00Z"), OffsetDateTime.parse("2030-01-01T00:00:00Z"));
        when(repository.findById(7L)).thenReturn(Optional.of(existing));

        new CustomerService(repository).update(7L, new CustomerCommand(" 客户 A ", "", null));

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).update(saved.capture());
        assertThat(saved.getValue().name()).isEqualTo("客户 A");
        assertThat(saved.getValue().phone()).isNull();
        assertThat(saved.getValue().email()).isNull();
    }
}

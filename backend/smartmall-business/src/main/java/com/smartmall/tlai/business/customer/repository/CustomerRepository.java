package com.smartmall.tlai.business.customer.repository;

import com.smartmall.tlai.business.customer.domain.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    long count(String keyword);
    List<Customer> findPage(int page, int size, String keyword);
    Optional<Customer> findById(long id);
    Customer insert(Customer customer);
    Customer update(Customer customer);
}

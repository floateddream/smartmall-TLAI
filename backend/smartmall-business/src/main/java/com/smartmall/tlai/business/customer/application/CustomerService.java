package com.smartmall.tlai.business.customer.application;

import com.smartmall.tlai.business.customer.domain.Customer;
import com.smartmall.tlai.business.customer.repository.CustomerRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class CustomerService {
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<Customer> list(int page, int size, String keyword) {
        String search = keyword == null ? "" : keyword.trim();
        return new PageResponse<>(repository.findPage(page, size, search),
                repository.count(search), page, size);
    }

    @Transactional(readOnly = true)
    public Customer get(long id) {
        return requireCustomer(id);
    }

    @Transactional
    public Customer create(CustomerCommand command) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.insert(new Customer(null, command.name().trim(), normalizeOptional(command.phone()),
                normalizeEmail(command.email()), now, now));
    }

    @Transactional
    public Customer update(long id, CustomerCommand command) {
        Customer existing = requireCustomer(id);
        return repository.update(new Customer(id, command.name().trim(), normalizeOptional(command.phone()),
                normalizeEmail(command.email()), existing.createdAt(), OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private Customer requireCustomer(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeOptional(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}

package com.smartmall.tlai.infrastructure.persistence.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.business.customer.domain.Customer;
import com.smartmall.tlai.business.customer.repository.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class MybatisCustomerRepository implements CustomerRepository {
    private final CustomerMapper mapper;

    public MybatisCustomerRepository(CustomerMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long count(String keyword) {
        return mapper.selectCount(filter(keyword));
    }

    @Override
    public List<Customer> findPage(int page, int size, String keyword) {
        long offset = ((long) page - 1) * size;
        LambdaQueryWrapper<CustomerPersistenceEntity> query = filter(keyword)
                .orderByDesc(CustomerPersistenceEntity::getId)
                .last("LIMIT " + size + " OFFSET " + offset);
        return mapper.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Customer> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Customer insert(Customer customer) {
        CustomerPersistenceEntity entity = toEntity(customer);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Customer update(Customer customer) {
        CustomerPersistenceEntity entity = toEntity(customer);
        mapper.updateById(entity);
        return toDomain(entity);
    }

    private LambdaQueryWrapper<CustomerPersistenceEntity> filter(String keyword) {
        LambdaQueryWrapper<CustomerPersistenceEntity> query = Wrappers.lambdaQuery(CustomerPersistenceEntity.class);
        if (!keyword.isEmpty()) {
            query.and(part -> part.like(CustomerPersistenceEntity::getName, keyword)
                    .or().like(CustomerPersistenceEntity::getPhone, keyword)
                    .or().like(CustomerPersistenceEntity::getEmail, keyword.toLowerCase(Locale.ROOT)));
        }
        return query;
    }

    private Customer toDomain(CustomerPersistenceEntity entity) {
        return new Customer(entity.getId(), entity.getName(), entity.getPhone(), entity.getEmail(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private CustomerPersistenceEntity toEntity(Customer customer) {
        CustomerPersistenceEntity entity = new CustomerPersistenceEntity();
        entity.setId(customer.id());
        entity.setName(customer.name());
        entity.setPhone(customer.phone());
        entity.setEmail(customer.email());
        entity.setCreatedAt(customer.createdAt());
        entity.setUpdatedAt(customer.updatedAt());
        return entity;
    }
}

package com.smartmall.tlai.infrastructure.persistence.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartmall.tlai.agent.model.AgentActionConfirmation;
import com.smartmall.tlai.agent.model.AgentActionStatus;
import com.smartmall.tlai.agent.repository.AgentActionConfirmationRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class MybatisAgentActionConfirmationRepository implements AgentActionConfirmationRepository {
    private final AgentActionConfirmationMapper mapper;

    public MybatisAgentActionConfirmationRepository(AgentActionConfirmationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentActionConfirmation insert(AgentActionConfirmation confirmation) {
        AgentActionConfirmationPersistenceEntity entity = toEntity(confirmation);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<AgentActionConfirmation> findByTokenHash(String tokenHash) {
        var query = new LambdaQueryWrapper<AgentActionConfirmationPersistenceEntity>()
                .eq(AgentActionConfirmationPersistenceEntity::getTokenHash, tokenHash)
                .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query)).map(this::toDomain);
    }

    @Override
    public boolean markExecuting(long id, long userId, OffsetDateTime confirmedAt) {
        var update = new LambdaUpdateWrapper<AgentActionConfirmationPersistenceEntity>()
                .eq(AgentActionConfirmationPersistenceEntity::getId, id)
                .eq(AgentActionConfirmationPersistenceEntity::getUserId, userId)
                .eq(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.PENDING.name())
                .gt(AgentActionConfirmationPersistenceEntity::getExpiresAt, confirmedAt)
                .set(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.EXECUTING.name())
                .set(AgentActionConfirmationPersistenceEntity::getConfirmedAt, confirmedAt);
        return mapper.update(null, update) == 1;
    }

    @Override
    public void markSucceeded(long id, String resultSummary) {
        var update = new LambdaUpdateWrapper<AgentActionConfirmationPersistenceEntity>()
                .eq(AgentActionConfirmationPersistenceEntity::getId, id)
                .eq(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.EXECUTING.name())
                .set(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.SUCCEEDED.name())
                .set(AgentActionConfirmationPersistenceEntity::getResultSummary, resultSummary);
        mapper.update(null, update);
    }

    @Override
    public boolean cancel(long id, long userId) {
        var update = new LambdaUpdateWrapper<AgentActionConfirmationPersistenceEntity>()
                .eq(AgentActionConfirmationPersistenceEntity::getId, id)
                .eq(AgentActionConfirmationPersistenceEntity::getUserId, userId)
                .eq(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.PENDING.name())
                .set(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.CANCELLED.name());
        return mapper.update(null, update) == 1;
    }

    @Override
    public boolean markExpired(long id) {
        var update = new LambdaUpdateWrapper<AgentActionConfirmationPersistenceEntity>()
                .eq(AgentActionConfirmationPersistenceEntity::getId, id)
                .eq(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.PENDING.name())
                .set(AgentActionConfirmationPersistenceEntity::getStatus, AgentActionStatus.EXPIRED.name());
        return mapper.update(null, update) == 1;
    }

    private AgentActionConfirmationPersistenceEntity toEntity(AgentActionConfirmation value) {
        var entity = new AgentActionConfirmationPersistenceEntity();
        entity.setId(value.id());
        entity.setTokenHash(value.tokenHash());
        entity.setUserId(value.userId());
        entity.setUsername(value.username());
        entity.setActionName(value.actionName());
        entity.setArgumentsJson(value.argumentsJson());
        entity.setStatus(value.status().name());
        entity.setExpiresAt(value.expiresAt());
        entity.setResultSummary(value.resultSummary());
        entity.setCreatedAt(value.createdAt());
        entity.setConfirmedAt(value.confirmedAt());
        return entity;
    }

    private AgentActionConfirmation toDomain(AgentActionConfirmationPersistenceEntity entity) {
        return new AgentActionConfirmation(entity.getId(), entity.getTokenHash(), entity.getUserId(),
                entity.getUsername(), entity.getActionName(), entity.getArgumentsJson(),
                AgentActionStatus.valueOf(entity.getStatus()), entity.getExpiresAt(), entity.getResultSummary(),
                entity.getCreatedAt(), entity.getConfirmedAt());
    }
}

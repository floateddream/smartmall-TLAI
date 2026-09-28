package com.smartmall.tlai.infrastructure.persistence.access;

import com.smartmall.tlai.business.access.domain.PermissionAudit;
import com.smartmall.tlai.business.access.repository.PermissionAuditRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisPermissionAuditRepository implements PermissionAuditRepository {
    private final PermissionAuditMapper mapper;

    public MybatisPermissionAuditRepository(PermissionAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(PermissionAudit audit) {
        mapper.insert(toEntity(audit));
    }

    @Override
    public long count(String targetType, Long targetId) {
        return mapper.countAudits(targetType, targetId);
    }

    @Override
    public List<PermissionAudit> findPage(int page, int size, String targetType, Long targetId) {
        long offset = ((long) page - 1) * size;
        return mapper.selectPage(offset, size, targetType, targetId).stream().map(this::toDomain).toList();
    }

    private PermissionAudit toDomain(PermissionAuditPersistenceEntity entity) {
        return new PermissionAudit(entity.getId(), entity.getOperatorId(), entity.getOperatorName(),
                entity.getTargetType(), entity.getTargetId(), entity.getTargetName(), entity.getAction(),
                entity.getBeforeValue(), entity.getAfterValue(), entity.getCreatedAt());
    }

    private PermissionAuditPersistenceEntity toEntity(PermissionAudit audit) {
        PermissionAuditPersistenceEntity entity = new PermissionAuditPersistenceEntity();
        entity.setId(audit.id());
        entity.setOperatorId(audit.operatorId());
        entity.setOperatorName(audit.operatorName());
        entity.setTargetType(audit.targetType());
        entity.setTargetId(audit.targetId());
        entity.setTargetName(audit.targetName());
        entity.setAction(audit.action());
        entity.setBeforeValue(audit.beforeValue());
        entity.setAfterValue(audit.afterValue());
        entity.setCreatedAt(audit.createdAt());
        return entity;
    }
}

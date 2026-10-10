package com.smartmall.tlai.agent.application;

import com.smartmall.tlai.agent.repository.AgentToolAuditRepository;
import com.smartmall.tlai.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentToolAuditServiceTest {
    @Mock AgentToolAuditRepository repository;

    @Test
    void normalizesValidAuditFilters() {
        when(repository.findPage(2, 20, "queryOrder", "SUCCESS", null)).thenReturn(List.of());
        when(repository.count("queryOrder", "SUCCESS", null)).thenReturn(0L);

        var result = new AgentToolAuditService(repository)
                .list(2, 20, " queryOrder ", " success ");

        assertThat(result.page()).isEqualTo(2);
        verify(repository).findPage(2, 20, "queryOrder", "SUCCESS", null);
        verify(repository).count("queryOrder", "SUCCESS", null);
    }

    @Test
    void rejectsUnknownAuditStatus() {
        assertThatThrownBy(() -> new AgentToolAuditService(repository)
                .list(1, 20, null, "RUNNING"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getMessage()).contains("状态不合法"));

        verifyNoInteractions(repository);
    }

    @Test
    void filtersByExplicitTraceId() {
        String traceId = "123e4567-e89b-42d3-a456-426614174000";
        when(repository.findPage(1, 20, null, null, traceId)).thenReturn(List.of());
        when(repository.count(null, null, traceId)).thenReturn(0L);

        new AgentToolAuditService(repository).list(1, 20, null, null, traceId.toUpperCase());

        verify(repository).findPage(1, 20, null, null, traceId);
    }

    @Test
    void rejectsMalformedTraceIdWithoutQueryingRepository() {
        assertThatThrownBy(() -> new AgentToolAuditService(repository)
                .list(1, 20, null, null, "../conversation/1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getMessage()).contains("追踪标识不合法"));

        verifyNoInteractions(repository);
    }
}

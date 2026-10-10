package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.model.AiCallLog;
import com.smartmall.tlai.ai.model.AiCallScene;
import com.smartmall.tlai.ai.model.AiCallStatus;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.repository.AiCallLogRepository;
import com.smartmall.tlai.common.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCallLogServiceTest {
    private final AiCallLogRepository repository = mock(AiCallLogRepository.class);
    private final AiCallLogService service = new AiCallLogService(repository);

    @Test
    void shouldNormalizeFiltersAndReturnPage() {
        when(repository.findPage(2, 20, AiModelType.CHAT, AiProvider.OPENAI,
                AiCallScene.ASSISTANT_CHAT, AiCallStatus.SUCCESS, null)).thenReturn(List.of());
        when(repository.count(AiModelType.CHAT, AiProvider.OPENAI,
                AiCallScene.ASSISTANT_CHAT, AiCallStatus.SUCCESS, null)).thenReturn(21L);

        var result = service.list(2, 20, "chat", "openai", "assistant_chat", "success");

        assertThat(result.total()).isEqualTo(21);
        assertThat(result.page()).isEqualTo(2);
        verify(repository).findPage(2, 20, AiModelType.CHAT, AiProvider.OPENAI,
                AiCallScene.ASSISTANT_CHAT, AiCallStatus.SUCCESS, null);
    }

    @Test
    void shouldRejectUnknownFilter() {
        assertThatThrownBy(() -> service.list(1, 20, "AUDIO", null, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldFilterByExplicitTraceId() {
        String traceId = "123e4567-e89b-42d3-a456-426614174000";
        when(repository.findPage(1, 20, null, null, null, null, traceId)).thenReturn(List.of());
        when(repository.count(null, null, null, null, traceId)).thenReturn(0L);

        service.list(1, 20, null, null, null, null, traceId.toUpperCase());

        verify(repository).findPage(1, 20, null, null, null, null, traceId);
    }

    @Test
    void shouldRejectMalformedTraceIdWithoutQueryingRepository() {
        assertThatThrownBy(() -> service.list(1, 20, null, null, null, null,
                "not-a-trace-id"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getMessage()).contains("追踪标识不合法"));
    }
}

package com.smartmall.tlai.rag.application;

import com.smartmall.tlai.ai.application.AiChatService;
import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiChatResult;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.knowledge.application.KnowledgeSearchService;
import com.smartmall.tlai.knowledge.model.KnowledgeSearchHit;
import com.smartmall.tlai.rag.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {
    @Mock KnowledgeSearchService searchService;
    @Mock AiChatService chatService;

    @Test
    void addsRetrievedKnowledgeAndReturnsSources() {
        List<AiChatMessage> messages = userQuestion();
        var hit = new KnowledgeSearchHit(11, 7, "退款规范.md", 2,
                "退款金额超过五千元需要财务主管审核。", 0.86);
        when(searchService.search("退款超过多少钱需要审核？", 5, 0.35))
                .thenReturn(List.of(hit));
        when(chatService.chatWithContext(anyList(), contains("[资料1]")))
                .thenReturn(new AiChatResult("超过五千元。[资料1]", "deepseek-chat", 250));

        var result = service().chat(messages);

        assertThat(result.content()).contains("[资料1]");
        assertThat(result.sources()).hasSize(1);
        assertThat(result.sources().getFirst().documentName()).isEqualTo("退款规范.md");
        assertThat(result.sources().getFirst().score()).isEqualTo(0.86);
    }

    @Test
    void doesNotCallChatModelWhenKnowledgeDoesNotMatch() {
        List<AiChatMessage> messages = userQuestion();
        when(searchService.search("退款超过多少钱需要审核？", 5, 0.35)).thenReturn(List.of());

        var result = service().chat(messages);

        assertThat(result.sources()).isEmpty();
        assertThat(result.content()).contains("没有找到");
        verify(chatService, never()).chatWithContext(anyList(), contains("知识库"));
    }

    private RagChatService service() {
        return new RagChatService(searchService, chatService,
                new RagProperties(5, 0.35, 10_000));
    }

    private List<AiChatMessage> userQuestion() {
        return List.of(new AiChatMessage(AiMessageRole.USER, "退款超过多少钱需要审核？"));
    }
}

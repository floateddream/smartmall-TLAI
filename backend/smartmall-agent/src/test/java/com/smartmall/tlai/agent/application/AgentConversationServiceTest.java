package com.smartmall.tlai.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.tlai.agent.model.AgentChatResult;
import com.smartmall.tlai.agent.model.AgentConversation;
import com.smartmall.tlai.agent.model.AgentConversationMessage;
import com.smartmall.tlai.agent.model.AgentConversationState;
import com.smartmall.tlai.agent.model.AgentConversationStatus;
import com.smartmall.tlai.agent.model.AgentResponseType;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.agent.repository.AgentConversationRepository;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.ai.model.AiCallContext;
import com.smartmall.tlai.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentConversationServiceTest {
    @Mock AgentConversationRepository repository;
    @Mock ReadOnlyAgentService agentService;

    @Test
    void createsOwnerBoundConversation() {
        AgentUserContext user = user(7L);
        when(repository.insertConversation(any())).thenAnswer(invocation -> {
            AgentConversation value = invocation.getArgument(0);
            return new AgentConversation(15L, value.userId(), value.title(), value.status(),
                    value.stateJson(), value.lastMessageAt(), value.createdAt(), value.updatedAt());
        });

        AgentConversation result = service().create(user);

        assertThat(result.id()).isEqualTo(15L);
        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.title()).isEqualTo("新对话");
        assertThat(result.status()).isEqualTo(AgentConversationStatus.ACTIVE);
    }

    @Test
    void rejectsConversationOwnedByAnotherUser() {
        when(repository.findOwnedConversation(99L, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().messages(99L, user(8L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getMessage()).contains("会话不存在"));

        verify(repository, never()).findMessages(anyLong(), anyLong(), anyInt());
    }

    @Test
    void persistsMessagesAndUpdatesMultiTurnState() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AgentConversation original = new AgentConversation(21L, 7L, "新对话",
                AgentConversationStatus.ACTIVE, "{\"intent\":\"createAfterSalesTicket\",\"argumentsJson\":\"{}\",\"missingFields\":[\"subject\"]}",
                now, now, now);
        AgentConversation updated = new AgentConversation(21L, 7L, "主题是物流停滞",
                AgentConversationStatus.ACTIVE, "{\"intent\":\"createAfterSalesTicket\",\"argumentsJson\":\"{\\\"subject\\\":\\\"物流停滞\\\"}\",\"missingFields\":[\"description\"]}",
                now, now, now);
        when(repository.findOwnedConversation(21L, 7L))
                .thenReturn(Optional.of(original), Optional.of(updated));
        when(repository.insertMessage(any())).thenAnswer(invocation -> {
            AgentConversationMessage value = invocation.getArgument(0);
            long id = value.role() == AiMessageRole.USER ? 31L : 32L;
            return new AgentConversationMessage(id, value.conversationId(), value.userId(), value.role(),
                    value.content(), value.responseType(), value.modelName(), value.latencyMs(), value.tools(),
                    value.suggestions(), value.missingFields(), value.createdAt());
        });
        when(repository.findRecentMessages(21L, 7L, 30)).thenReturn(List.of(
                new AgentConversationMessage(31L, 21L, 7L, AiMessageRole.USER, "主题是物流停滞",
                        null, null, null, List.of(), List.of(), List.of(), now)));
        AgentConversationState nextState = new AgentConversationState("createAfterSalesTicket",
                "{\"subject\":\"物流停滞\"}", List.of("description"));
        when(agentService.chat(any(), eq(user(7L)), any(), any())).thenReturn(new AgentChatResult(
                "还需要具体问题描述", "deepseek-chat", 120, List.of(), null, List.of(),
                AgentResponseType.CLARIFICATION, List.of("description"), nextState));

        var turn = service().reply(21L, "主题是物流停滞", user(7L));

        assertThat(turn.userMessage().id()).isEqualTo(31L);
        assertThat(turn.assistantMessage().id()).isEqualTo(32L);
        assertThat(turn.conversation()).isEqualTo(updated);
        ArgumentCaptor<AgentConversationState> stateCaptor = ArgumentCaptor.forClass(AgentConversationState.class);
        ArgumentCaptor<AiCallContext> contextCaptor = ArgumentCaptor.forClass(AiCallContext.class);
        verify(agentService).chat(any(), eq(user(7L)), stateCaptor.capture(), contextCaptor.capture());
        assertThat(stateCaptor.getValue().missingFields()).containsExactly("subject");
        assertThat(contextCaptor.getValue().conversationId()).isEqualTo(21L);
        assertThat(contextCaptor.getValue().traceId()).matches("[0-9a-f-]{36}");
        verify(repository).updateConversation(eq(21L), eq(7L), eq("主题是物流停滞"),
                org.mockito.ArgumentMatchers.contains("description"), any());
    }

    @Test
    void clearsStateAfterCompletedReply() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AgentConversation conversation = new AgentConversation(22L, 7L, "工单处理",
                AgentConversationStatus.ACTIVE, "{\"intent\":\"createAfterSalesTicket\",\"argumentsJson\":\"{}\",\"missingFields\":[\"subject\"]}",
                now, now, now);
        when(repository.findOwnedConversation(22L, 7L)).thenReturn(Optional.of(conversation), Optional.of(conversation));
        when(repository.insertMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findRecentMessages(22L, 7L, 30)).thenReturn(List.of());
        when(agentService.chat(any(), eq(user(7L)), any(), any())).thenReturn(new AgentChatResult(
                "操作已准备好", "deepseek-chat", 80, List.of(), null, List.of(),
                AgentResponseType.ANSWER, List.of()));

        service().reply(22L, "继续", user(7L));

        verify(repository).updateConversation(eq(22L), eq(7L), eq("工单处理"),
                org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void rejectsReplyToArchivedConversation() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        when(repository.findOwnedConversation(23L, 7L)).thenReturn(Optional.of(new AgentConversation(
                23L, 7L, "历史会话", AgentConversationStatus.ARCHIVED, null, now, now, now)));

        assertThatThrownBy(() -> service().reply(23L, "继续", user(7L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getMessage()).contains("归档会话"));

        verify(repository, never()).insertMessage(any());
        verify(agentService, never()).chat(any(), any(), any(), any());
    }

    private AgentConversationService service() {
        return new AgentConversationService(repository, agentService,
                new ObjectMapper().findAndRegisterModules());
    }

    private static AgentUserContext user(long id) {
        return new AgentUserContext(id, "user-" + id, Set.of("ai:chat"));
    }
}

package com.smartmall.tlai.agent.application;

import com.smartmall.tlai.agent.model.AgentUserContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCapabilityRegistryTest {
    private final AgentCapabilityRegistry registry = new AgentCapabilityRegistry();

    @Test
    void exposesOnlyCapabilitiesAllowedForCurrentUser() {
        var user = new AgentUserContext(7L, "operator",
                Set.of("ai:chat", "order:view", "ticket:create"));

        assertThat(registry.availableTo(user))
                .extracting("code")
                .containsExactly("generalChat", "queryOrder", "createAfterSalesTicket");
        assertThat(registry.plannerInstructions(user))
                .contains("queryOrder", "createAfterSalesTicket")
                .doesNotContain("queryRefund", "addTicketReply");
    }

    @Test
    void quickSuggestionsNeverExecuteControlledWriteAction() {
        var user = new AgentUserContext(7L, "operator",
                Set.of("ai:chat", "order:view", "ticket:create"));

        assertThat(registry.suggestionsFor(user, null, 10))
                .extracting("capabilityCode")
                .containsExactly("generalChat", "queryOrder")
                .doesNotContain("createAfterSalesTicket");
    }
}

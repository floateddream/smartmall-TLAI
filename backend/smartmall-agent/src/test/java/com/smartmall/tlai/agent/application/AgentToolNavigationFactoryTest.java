package com.smartmall.tlai.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.tlai.agent.model.AgentToolExecution;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolNavigationFactoryTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldBuildOnlyWhitelistedOrderNavigation() throws Exception {
        var arguments = objectMapper.readTree("""
                {"id":null,"keyword":"SM2026","status":"PAID","redirect":"https://example.com"}
                """);

        var navigation = AgentToolNavigationFactory.create("queryOrder", arguments);

        assertThat(navigation.path()).isEqualTo("/orders");
        assertThat(navigation.query()).containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of("keyword", "SM2026", "status", "PAID"));
        assertThat(navigation.query()).doesNotContainKey("redirect");
    }

    @Test
    void shouldMapAnalyticsCapabilitiesToControlledReport() throws Exception {
        var arguments = objectMapper.readTree("""
                {"startDate":"2026-09-01","endDate":"2026-09-30","limit":10,"path":"/access-control"}
                """);

        var navigation = AgentToolNavigationFactory.create("topRefundProducts", arguments);

        assertThat(navigation.path()).isEqualTo("/data-analysis");
        assertThat(navigation.query()).containsEntry("report", "REFUND_PRODUCT")
                .containsEntry("startDate", "2026-09-01")
                .containsEntry("endDate", "2026-09-30")
                .containsEntry("granularity", "MONTH")
                .containsEntry("limit", "10")
                .doesNotContainKey("path");
    }

    @Test
    void shouldIgnoreUnknownToolsAndInvalidParameters() throws Exception {
        var arguments = objectMapper.readTree("""
                {"id":-1,"keyword":"safe","status":"ADMIN","category":"SCRIPT"}
                """);

        assertThat(AgentToolNavigationFactory.create("unknownTool", arguments)).isNull();
        assertThat(AgentToolNavigationFactory.create("queryTicket", arguments).query())
                .containsOnlyKeys("keyword");
    }

    @Test
    void shouldUseOnlyTheDocumentedToolRouteMapping() throws Exception {
        var emptyArguments = objectMapper.readTree("{}");
        var expected = java.util.Map.of(
                "queryOrder", "/orders",
                "queryRefund", "/refunds",
                "queryTicket", "/tickets",
                "knowledgeBaseSearch", "/knowledge",
                "analyticsQuery", "/data-analysis");

        expected.forEach((tool, path) ->
                assertThat(AgentToolNavigationFactory.create(tool, emptyArguments).path()).isEqualTo(path));
    }

    @Test
    void shouldReadHistoricalToolJsonWithoutNavigation() throws Exception {
        AgentToolExecution execution = objectMapper.readValue("""
                {"toolName":"queryOrder","displayName":"订单查询","summary":"已查询订单列表","latencyMs":18}
                """, AgentToolExecution.class);

        assertThat(execution.navigation()).isNull();
        assertThat(execution.toolName()).isEqualTo("queryOrder");
    }

    @Test
    void shouldDropUnsafeTextAndInvalidDateRange() throws Exception {
        var unsafeListArguments = objectMapper.readTree("""
                {"keyword":"order\\nredirect","status":"PAID"}
                """);
        var invalidAnalyticsArguments = objectMapper.readTree("""
                {"report":"ORDER_TREND","startDate":"2020-01-01","endDate":"2027-01-02","granularity":"DAY"}
                """);

        assertThat(AgentToolNavigationFactory.create("queryOrder", unsafeListArguments).query())
                .containsExactlyEntriesOf(java.util.Map.of("status", "PAID"));
        assertThat(AgentToolNavigationFactory.create("analyticsQuery", invalidAnalyticsArguments).query())
                .containsEntry("report", "ORDER_TREND")
                .containsEntry("granularity", "DAY")
                .doesNotContainKeys("startDate", "endDate");
    }
}

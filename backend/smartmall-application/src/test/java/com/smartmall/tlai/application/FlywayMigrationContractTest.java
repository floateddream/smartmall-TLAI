package com.smartmall.tlai.application;

import org.junit.jupiter.api.Test;
import com.smartmall.tlai.infrastructure.persistence.knowledge.KnowledgeChunkMapper;
import org.apache.ibatis.annotations.Select;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationContractTest {
    private static final List<String> MIGRATIONS = List.of(
            "V1__create_user_table.sql",
            "V2__create_catalog_tables.sql",
            "V3__create_order_tables.sql",
            "V4__create_refund_and_ticket_tables.sql",
            "V5__create_rbac_tables.sql",
            "V6__create_ai_model_config.sql",
            "V7__add_ai_provider_credentials.sql",
            "V8__add_embedding_model_support.sql",
            "V9__create_knowledge_base.sql",
            "V10__add_analytics_permission.sql",
            "V11__add_analytics_audit.sql",
            "V12__create_agent_tool_audit.sql",
            "V13__create_agent_action_confirmation.sql",
            "V14__create_ai_conversation_and_agent_audit_permission.sql",
            "V15__create_ai_call_log.sql",
            "V16__link_ai_audit_records.sql");

    @Test
    void migrationsOneThroughSixteenArePackagedWithoutGaps() {
        for (String migration : MIGRATIONS) {
            assertThat(getClass().getClassLoader().getResource("db/migration/" + migration))
                    .as("migration %s must be on the runtime classpath", migration)
                    .isNotNull();
        }
    }

    @Test
    void recentAiMigrationsKeepTablesPermissionsAndCorrelationIndexes() throws IOException {
        String v14 = read("V14__create_ai_conversation_and_agent_audit_permission.sql");
        String v15 = read("V15__create_ai_call_log.sql");
        String v16 = read("V16__link_ai_audit_records.sql");

        assertThat(v14).contains("CREATE TABLE ai_conversation", "CREATE TABLE ai_conversation_message",
                "agent:audit_view", "idx_ai_conversation_user_updated", "idx_ai_conversation_message_order");
        assertThat(v15).contains("CREATE TABLE ai_call_log", "ai:call_log_view",
                "idx_ai_call_log_created_at", "idx_ai_call_log_filters");
        assertThat(v16).contains("ADD COLUMN trace_id", "ADD COLUMN conversation_id",
                "idx_agent_tool_audit_trace", "idx_ai_call_log_trace",
                "idx_agent_tool_audit_conversation", "idx_ai_call_log_conversation");
    }

    @Test
    void semanticSearchOnlyReadsReadyKnowledgeDocuments() throws NoSuchMethodException {
        Select select = KnowledgeChunkMapper.class
                .getMethod("searchSimilar", String.class, int.class, double.class)
                .getAnnotation(Select.class);

        assertThat(select).isNotNull();
        assertThat(String.join("\n", select.value())).contains("d.status = 'READY'");
    }

    private String read(String name) throws IOException {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("db/migration/" + name)) {
            assertThat(stream).as("migration %s must exist", name).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

package com.smartmall.tlai.agent.application;

import com.smartmall.tlai.agent.model.AgentCapability;
import com.smartmall.tlai.agent.model.AgentCapabilityCategory;
import com.smartmall.tlai.agent.model.AgentCapabilityType;
import com.smartmall.tlai.agent.model.AgentSuggestion;
import com.smartmall.tlai.agent.model.AgentUserContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentCapabilityRegistry {
    private static final List<AgentCapability> CAPABILITIES = List.of(
            capability("generalChat", "通用问答", "回答日常、技术和系统使用问题",
                    AgentCapabilityCategory.GENERAL, AgentCapabilityType.DIRECT, "ai:chat",
                    List.of("你能做什么？", "这个系统的售后流程是怎样的？"), null),
            capability("knowledgeBaseSearch", "知识库检索", "根据已上传的内部资料回答问题",
                    AgentCapabilityCategory.KNOWLEDGE, AgentCapabilityType.READ_ONLY, "knowledge:view",
                    List.of("搜索知识库里的退换货规则"),
                    "knowledgeBaseSearch：知识库语义检索。arguments={\"query\":\"问题\",\"limit\":5}"),
            capability("queryOrder", "订单查询", "按订单号、客户或状态查询订单",
                    AgentCapabilityCategory.BUSINESS, AgentCapabilityType.READ_ONLY, "order:view",
                    List.of("查询最近的订单"),
                    "queryOrder：查询订单详情或列表。arguments={\"id\":null,\"keyword\":\"订单号或客户名\",\"status\":\"\"}"),
            capability("queryRefund", "退款查询", "查询退款申请与处理状态",
                    AgentCapabilityCategory.BUSINESS, AgentCapabilityType.READ_ONLY, "refund:view",
                    List.of("查询最近待审核的退款"),
                    "queryRefund：查询退款详情或列表。arguments={\"id\":null,\"keyword\":\"退款号或订单号\",\"status\":\"\"}"),
            capability("queryTicket", "工单查询", "查询售后工单与沟通记录",
                    AgentCapabilityCategory.BUSINESS, AgentCapabilityType.READ_ONLY, "ticket:view",
                    List.of("查看处理中的售后工单"),
                    "queryTicket：查询工单详情或列表。arguments={\"id\":null,\"keyword\":\"工单号或订单号\",\"status\":\"\",\"category\":\"\"}"),
            capability("topRefundProducts", "退款商品排行", "统计退款金额较高的商品",
                    AgentCapabilityCategory.ANALYTICS, AgentCapabilityType.READ_ONLY, "analytics:view",
                    List.of("分析最近 30 天退款金额最高的商品"),
                    "topRefundProducts：按已完成退款金额统计商品排行。arguments={\"startDate\":\"YYYY-MM-DD\",\"endDate\":\"YYYY-MM-DD\",\"limit\":10}"),
            capability("refundReasonDistribution", "退款原因分析", "统计退款原因及占比",
                    AgentCapabilityCategory.ANALYTICS, AgentCapabilityType.READ_ONLY, "analytics:view",
                    List.of("分析最近 30 天的退款原因"),
                    "refundReasonDistribution：统计退款原因分布。arguments={\"startDate\":\"YYYY-MM-DD\",\"endDate\":\"YYYY-MM-DD\",\"limit\":10}"),
            capability("createAfterSalesTicket", "创建售后工单", "根据订单创建需要确认的售后工单",
                    AgentCapabilityCategory.BUSINESS, AgentCapabilityType.CONFIRM_REQUIRED, "ticket:create",
                    List.of("为订单号 [请替换] 创建物流问题工单，普通优先级，主题是物流停滞，描述是三天没有更新"),
                    "createAfterSalesTicket：创建售后工单。优先使用业务订单号，orderId 与 orderNo 必须且只能填写一个。arguments={\"orderId\":null,\"orderNo\":\"SM20260924082608B125E3\",\"category\":\"RETURN_EXCHANGE\",\"priority\":\"NORMAL\",\"subject\":\"主题\",\"description\":\"问题描述\"}"),
            capability("addTicketReply", "追加工单回复", "向未关闭的售后工单追加回复",
                    AgentCapabilityCategory.BUSINESS, AgentCapabilityType.CONFIRM_REQUIRED, "ticket:reply",
                    List.of("给工单 ID [请替换] 回复：我们已经联系仓库核实"),
                    "addTicketReply：追加工单回复。arguments={\"ticketId\":123,\"content\":\"回复内容\"}")
    );

    public Optional<AgentCapability> find(String code) {
        return CAPABILITIES.stream().filter(capability -> capability.code().equals(code)).findFirst();
    }

    public List<AgentCapability> availableTo(AgentUserContext user) {
        return CAPABILITIES.stream().filter(capability -> user.can(capability.permission())).toList();
    }

    public String plannerInstructions(AgentUserContext user) {
        return availableTo(user).stream()
                .filter(capability -> capability.type() != AgentCapabilityType.DIRECT)
                .map(AgentCapability::plannerInstruction)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    public String describeAvailable(AgentUserContext user) {
        return availableTo(user).stream()
                .map(capability -> "- %s：%s".formatted(capability.displayName(), capability.description()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- 当前账号没有可用的 AI 能力");
    }

    public List<AgentSuggestion> suggestionsFor(AgentUserContext user, String excludedCode, int limit) {
        return availableTo(user).stream()
                .filter(capability -> capability.type() != AgentCapabilityType.CONFIRM_REQUIRED)
                .filter(capability -> excludedCode == null || !capability.code().equals(excludedCode))
                .filter(capability -> !capability.examples().isEmpty())
                .limit(limit)
                .map(capability -> new AgentSuggestion(capability.displayName(),
                        capability.examples().getFirst(), capability.code()))
                .toList();
    }

    private static AgentCapability capability(String code, String displayName, String description,
                                              AgentCapabilityCategory category, AgentCapabilityType type,
                                              String permission, List<String> examples,
                                              String plannerInstruction) {
        return new AgentCapability(code, displayName, description, category, type,
                permission, examples, plannerInstruction);
    }
}

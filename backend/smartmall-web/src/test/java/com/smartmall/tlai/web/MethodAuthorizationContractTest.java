package com.smartmall.tlai.web;

import com.smartmall.tlai.web.access.AccessControlController;
import com.smartmall.tlai.web.ai.AgentAuditController;
import com.smartmall.tlai.web.ai.AiCallLogController;
import com.smartmall.tlai.web.ai.AiChatController;
import com.smartmall.tlai.web.ai.AiConversationController;
import com.smartmall.tlai.web.ai.AiModelConfigController;
import com.smartmall.tlai.web.ai.PersonalAiModelConfigController;
import com.smartmall.tlai.web.analytics.AnalyticsController;
import com.smartmall.tlai.web.customer.CustomerController;
import com.smartmall.tlai.web.dashboard.DashboardController;
import com.smartmall.tlai.web.knowledge.KnowledgeDocumentController;
import com.smartmall.tlai.web.order.OrderController;
import com.smartmall.tlai.web.product.ProductController;
import com.smartmall.tlai.web.refund.RefundController;
import com.smartmall.tlai.web.ticket.TicketController;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MethodAuthorizationContractTest {
    private static final List<Class<?>> PROTECTED_CONTROLLERS = List.of(
            AccessControlController.class, AgentAuditController.class, AiCallLogController.class,
            AiChatController.class, AiConversationController.class, AiModelConfigController.class,
            PersonalAiModelConfigController.class,
            AnalyticsController.class, CustomerController.class, DashboardController.class,
            KnowledgeDocumentController.class, OrderController.class, ProductController.class,
            RefundController.class, TicketController.class);

    @Test
    void everyProtectedHttpEndpointDeclaresMethodOrControllerAuthorization() {
        for (Class<?> controller : PROTECTED_CONTROLLERS) {
            boolean controllerProtected = AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
            for (Method method : controller.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) continue;
                assertThat(controllerProtected || AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class))
                        .as("%s#%s must declare @PreAuthorize", controller.getSimpleName(), method.getName())
                        .isTrue();
            }
        }
    }

    @Test
    void modelConfigurationUsesOneControllerLevelPermissionForEveryEndpoint() {
        PreAuthorize annotation = AnnotatedElementUtils.findMergedAnnotation(
                AiModelConfigController.class, PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasAuthority('system:model_manage')");

        PreAuthorize personalAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                PersonalAiModelConfigController.class, PreAuthorize.class);
        assertThat(personalAnnotation).isNotNull();
        assertThat(personalAnnotation.value()).isEqualTo("hasAuthority('ai:chat')");
    }
}

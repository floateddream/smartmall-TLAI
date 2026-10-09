package com.smartmall.tlai.analytics.model;

/** 自然语言问题、受控查询结果与模型解读的完整返回。 */
public record NaturalLanguageAnalyticsResult(
        String question,
        AnalyticsResult analytics,
        String explanation,
        String modelName,
        long latencyMs
) {
}

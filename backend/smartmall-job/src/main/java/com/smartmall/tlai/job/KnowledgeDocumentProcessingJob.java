package com.smartmall.tlai.job;

import com.smartmall.tlai.knowledge.application.KnowledgeDocumentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 单线程轮询数据库任务，避免小规格服务器同时运行多个文档向量化任务。 */
@Component
public class KnowledgeDocumentProcessingJob {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentProcessingJob.class);
    private final KnowledgeDocumentProcessor processor;

    public KnowledgeDocumentProcessingJob(KnowledgeDocumentProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(initialDelayString = "${smartmall.knowledge.processing.initial-delay:5000}",
            fixedDelayString = "${smartmall.knowledge.processing.poll-interval:3000}")
    public void processNext() {
        try {
            processor.processNext();
        } catch (RuntimeException exception) {
            // 防止基础设施异常终止后续调度；文档级失败由 Processor 自己落库。
            log.error("知识库文档处理定时任务执行失败", exception);
        }
    }
}

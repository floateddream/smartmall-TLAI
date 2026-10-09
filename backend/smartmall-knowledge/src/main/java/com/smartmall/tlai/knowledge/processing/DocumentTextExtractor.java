package com.smartmall.tlai.knowledge.processing;

import java.io.InputStream;

/** 从不同文件格式提取纯文本，具体解析器由基础设施模块提供。 */
public interface DocumentTextExtractor {
    String extract(InputStream inputStream, String filename);
}

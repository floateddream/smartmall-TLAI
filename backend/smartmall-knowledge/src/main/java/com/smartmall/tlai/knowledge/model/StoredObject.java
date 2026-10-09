package com.smartmall.tlai.knowledge.model;

import java.io.InputStream;

/** 对象存储读取结果，流由 HTTP 响应完成传输后关闭。 */
public record StoredObject(InputStream inputStream, long sizeBytes, String contentType) {
}

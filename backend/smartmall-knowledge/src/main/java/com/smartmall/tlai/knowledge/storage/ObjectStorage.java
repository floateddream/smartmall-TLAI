package com.smartmall.tlai.knowledge.storage;

import com.smartmall.tlai.knowledge.model.StoredObject;

import java.io.InputStream;

/** 原始文件存储端口，使知识库业务不直接依赖 MinIO SDK。 */
public interface ObjectStorage {
    void put(String objectKey, InputStream inputStream, long sizeBytes, String contentType);
    StoredObject get(String objectKey);
    void delete(String objectKey);
}

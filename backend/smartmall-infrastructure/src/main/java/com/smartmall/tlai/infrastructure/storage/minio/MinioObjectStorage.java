package com.smartmall.tlai.infrastructure.storage.minio;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.StoredObject;
import com.smartmall.tlai.knowledge.storage.ObjectStorage;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class MinioObjectStorage implements ObjectStorage {
    private static final Logger log = LoggerFactory.getLogger(MinioObjectStorage.class);

    private final MinioProperties properties;
    private final MinioClient client;

    public MinioObjectStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = properties.configured()
                ? MinioClient.builder().endpoint(properties.endpoint())
                    .credentials(properties.accessKey(), properties.secretKey()).build()
                : null;
    }

    @Override
    public void put(String objectKey, InputStream inputStream, long sizeBytes, String contentType) {
        MinioClient configuredClient = requireClient();
        try {
            configuredClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(inputStream, sizeBytes, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            log.error("MinIO upload failed: endpoint={}, bucket={}, objectKey={}",
                    properties.endpoint(), properties.bucket(), objectKey, exception);
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_ERROR, "文件上传到对象存储失败");
        }
    }

    @Override
    public StoredObject get(String objectKey) {
        MinioClient configuredClient = requireClient();
        try {
            StatObjectResponse stat = configuredClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket()).object(objectKey).build());
            GetObjectResponse response = configuredClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket()).object(objectKey).build());
            return new StoredObject(response, stat.size(), stat.contentType());
        } catch (Exception exception) {
            log.error("MinIO download failed: endpoint={}, bucket={}, objectKey={}",
                    properties.endpoint(), properties.bucket(), objectKey, exception);
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_ERROR, "从对象存储读取文件失败");
        }
    }

    @Override
    public void delete(String objectKey) {
        MinioClient configuredClient = requireClient();
        try {
            configuredClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket()).object(objectKey).build());
        } catch (Exception exception) {
            log.error("MinIO delete failed: endpoint={}, bucket={}, objectKey={}",
                    properties.endpoint(), properties.bucket(), objectKey, exception);
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_ERROR, "从对象存储删除文件失败");
        }
    }

    private MinioClient requireClient() {
        if (client == null) throw new BusinessException(ErrorCode.OBJECT_STORAGE_NOT_CONFIGURED);
        return client;
    }
}

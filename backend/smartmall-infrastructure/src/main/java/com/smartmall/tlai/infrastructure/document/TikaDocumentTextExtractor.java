package com.smartmall.tlai.infrastructure.document;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.processing.DocumentTextExtractor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TikaDocumentTextExtractor implements DocumentTextExtractor {
    private static final int TIKA_WRITE_LIMIT = 210_000;
    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public String extract(InputStream inputStream, String filename) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        BodyContentHandler handler = new BodyContentHandler(TIKA_WRITE_LIMIT);
        try {
            parser.parse(inputStream, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (IOException | SAXException | TikaException exception) {
            if (WriteLimitReachedException.isWriteLimitReached(exception)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "文档提取文本超过 200000 个字符，请拆分后重新上传");
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "无法解析文档内容，请确认文件没有损坏且格式与扩展名一致");
        }
    }
}

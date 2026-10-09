package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.StoredObject;

public record KnowledgeDocumentDownload(KnowledgeDocument document, StoredObject object) {
}

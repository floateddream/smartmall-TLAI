package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.knowledge.model.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 按自然标点切分文本，并保留少量重叠上下文，适合中文业务文档。 */
@Component
public class KnowledgeTextSplitter {
    static final int MAX_CHUNK_CHARACTERS = 1200;
    static final int CHUNK_OVERLAP_CHARACTERS = 120;
    private static final int MIN_BOUNDARY_CHARACTERS = MAX_CHUNK_CHARACTERS / 2;

    public List<TextChunk> split(String source) {
        if (source == null || source.isBlank()) return List.of();
        String text = normalize(source);
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int targetEnd = Math.min(start + MAX_CHUNK_CHARACTERS, text.length());
            int end = targetEnd == text.length() ? targetEnd : findBoundary(text, start, targetEnd);
            String content = text.substring(start, end).trim();
            if (!content.isEmpty()) {
                chunks.add(new TextChunk(chunks.size(), content, estimateTokens(content)));
            }
            if (end >= text.length()) break;
            start = Math.max(start + 1, end - CHUNK_OVERLAP_CHARACTERS);
            while (start < end && Character.isWhitespace(text.charAt(start))) start++;
        }
        return List.copyOf(chunks);
    }

    private String normalize(String source) {
        return source.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private int findBoundary(String text, int start, int targetEnd) {
        int minimum = Math.min(start + MIN_BOUNDARY_CHARACTERS, targetEnd);
        for (int index = targetEnd - 1; index >= minimum; index--) {
            char character = text.charAt(index);
            if (character == '\n' || character == '。' || character == '！' || character == '？'
                    || character == '；' || character == '.' || character == '!' || character == '?') {
                return index + 1;
            }
        }
        return targetEnd;
    }

    private int estimateTokens(String content) {
        int cjk = 0;
        int other = 0;
        for (int offset = 0; offset < content.length();) {
            int codePoint = content.codePointAt(offset);
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL) {
                cjk++;
            } else if (!Character.isWhitespace(codePoint)) {
                other++;
            }
            offset += Character.charCount(codePoint);
        }
        return cjk + Math.max(1, (other + 3) / 4);
    }
}

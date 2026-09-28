package com.smartmall.tlai.ai.security;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCredentialCipherTest {
    private static final String MASTER_KEY = "smartmall-tlai-ai-master-key-2026";

    @Test
    void shouldEncryptAndDecryptCredential() {
        AiCredentialCipher cipher = new AiCredentialCipher(new AiProperties(MASTER_KEY, Duration.ofSeconds(10)));

        String encrypted = cipher.encrypt("sk-example-secret");

        assertThat(encrypted).startsWith("v1:").doesNotContain("sk-example-secret");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("sk-example-secret");
    }

    @Test
    void shouldRejectMissingMasterKey() {
        AiCredentialCipher cipher = new AiCredentialCipher(new AiProperties("", Duration.ofSeconds(10)));

        assertThatThrownBy(() -> cipher.encrypt("sk-example-secret"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AI_ENCRYPTION_KEY_MISSING));
    }

    @Test
    void shouldRejectShortTextMasterKey() {
        AiCredentialCipher cipher = new AiCredentialCipher(new AiProperties("too-short", Duration.ofSeconds(10)));

        assertThatThrownBy(() -> cipher.encrypt("sk-example-secret"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AI_ENCRYPTION_KEY_INVALID));
    }
}

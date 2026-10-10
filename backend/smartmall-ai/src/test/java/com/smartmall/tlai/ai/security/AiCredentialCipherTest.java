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

    @Test
    void shouldRejectCredentialEncryptedWithAnotherMasterKey() {
        AiCredentialCipher first = cipher(MASTER_KEY);
        AiCredentialCipher second = cipher("another-smartmall-master-key-2026");
        String encrypted = first.encrypt("example-sensitive-credential");

        assertDecryptionFailure(() -> second.decrypt(encrypted));
    }

    @Test
    void shouldRejectTamperedCiphertextWithoutLeakingSensitiveValues() {
        AiCredentialCipher cipher = cipher(MASTER_KEY);
        String encrypted = cipher.encrypt("example-sensitive-credential");
        char replacement = encrypted.charAt(encrypted.length() - 1) == 'A' ? 'B' : 'A';
        String tampered = encrypted.substring(0, encrypted.length() - 1) + replacement;

        assertDecryptionFailure(() -> cipher.decrypt(tampered));
    }

    private AiCredentialCipher cipher(String masterKey) {
        return new AiCredentialCipher(new AiProperties(masterKey, Duration.ofSeconds(10)));
    }

    private void assertDecryptionFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AI_CREDENTIAL_DECRYPTION_FAILED);
                    assertThat(error.getMessage()).doesNotContain("example-sensitive-credential", MASTER_KEY);
                });
    }
}

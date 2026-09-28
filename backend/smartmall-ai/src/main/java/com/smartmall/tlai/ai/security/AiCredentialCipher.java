package com.smartmall.tlai.ai.security;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** 使用带完整性校验的 AES-256-GCM 加密供应商密钥，数据库中不会出现明文 API Key。 */
@Component
public class AiCredentialCipher {
    private static final String PREFIX = "v1:";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] AAD = "smartmall-ai-credential-v1".getBytes(StandardCharsets.UTF_8);

    private final AiProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiCredentialCipher(AiProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new BusinessException(ErrorCode.AI_CREDENTIAL_MISSING);
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(AAD);
            byte[] encrypted = cipher.doFinal(plaintext.trim().getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.AI_CREDENTIAL_DECRYPTION_FAILED);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new BusinessException(ErrorCode.AI_CREDENTIAL_MISSING);
        }
        try {
            if (!ciphertext.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Unsupported credential format");
            }
            byte[] payload = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException("Invalid credential payload");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] encrypted = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (AEADBadTagException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.AI_CREDENTIAL_DECRYPTION_FAILED);
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.AI_CREDENTIAL_DECRYPTION_FAILED);
        }
    }

    private SecretKeySpec key() {
        if (!properties.masterKeyConfigured()) {
            throw new BusinessException(ErrorCode.AI_ENCRYPTION_KEY_MISSING);
        }

        String configuredKey = properties.masterKey();
        try {
            // 兼容之前文档中的 32 字节 Base64 密钥，已有密文无需重新录入。
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length == KEY_BYTES) {
                return new SecretKeySpec(decoded, "AES");
            }
        } catch (IllegalArgumentException exception) {
            // 普通文本本来就可能不是 Base64，继续按文本密钥处理。
        }

        byte[] textBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
        if (textBytes.length < KEY_BYTES) {
            throw new BusinessException(ErrorCode.AI_ENCRYPTION_KEY_INVALID,
                    "AI 密钥加密主密钥至少需要 32 个 ASCII 字符");
        }
        try {
            // 使用 SHA-256 固定派生出 AES-256 所需的 32 字节密钥。
            return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(textBytes), "AES");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}

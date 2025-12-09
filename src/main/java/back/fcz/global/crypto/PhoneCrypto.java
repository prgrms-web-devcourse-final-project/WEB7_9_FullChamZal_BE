package back.fcz.global.crypto;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/*
전화번호 암/복호화 클래스
 */
@Component
public class PhoneCrypto {

    private final SecretKeySpec secretKey;
    private final String shaSalt;

    public PhoneCrypto(
            @Value("${security.encryption.aes-key}") String aesKeyBase64,
            @Value("${security.encryption.sha-salt}") String shaSalt
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.shaSalt = shaSalt;
    }

    // AES-256 암호화
    public String encrypt(String plainPhone) {
        try {
            byte[] iv = genIv();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec parameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encrypted = cipher.doFinal(plainPhone.getBytes("UTF-8"));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ENCRYPTION_FAILED);
        }
    }

    // AES-256 복호화
    public String decrypt(String encryptedPhone) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPhone);

            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, 16);

            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec parameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DECRYPTION_FAILED);
        }
    }

    // SHA-256 해싱 (검색용, 고정 Salt 사용)
    public String hash(String plainPhone) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPhone = plainPhone + shaSalt;
            byte[] hash = digest.digest(saltedPhone.getBytes("UTF-8"));

            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.HASHING_FAILED);
        }
    }

    // 해시 검증 (입력 전화번호가 저장된 해시와 일치하는지)
    public boolean verifyHash(String plainPhone, String hashedPhone) {
        return hash(plainPhone).equals(hashedPhone);
    }

    // iv 생성
    public static byte[] genIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}

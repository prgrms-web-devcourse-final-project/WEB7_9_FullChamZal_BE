package back.fcz.global.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class PhoneCryptoTest {

    @Autowired
    private PhoneCrypto phoneCrypto;

    @Test
    @DisplayName("AES-256 암호화 후 복호화했을 때 원본 값이 복원되어야 한다")
    void aes_encrypt_decrypt_success() {
        // given
        String phone = "01012345678";

        // when
        String encrypted = phoneCrypto.encrypt(phone);
        String decrypted = phoneCrypto.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(phone);
    }

    @Test
    @DisplayName("SHA-256 해시는 동일한 입력에 대해 항상 동일한 해시값을 반환해야 한다 (고정 Salt 기반)")
    void sha_hash_should_be_consistent() {
        // given
        String phone = "01099990000";

        // when
        String hash1 = phoneCrypto.hash(phone);
        String hash2 = phoneCrypto.hash(phone);

        // then
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }

    @Test
    @DisplayName("verifyHash()는 원본 문자열을 동일한 해시로 비교하면 true를 반환해야 한다")
    void verifyHash_success() {
        // given
        String phone = "01022223333";
        String hashed = phoneCrypto.hash(phone);

        // when
        boolean result = phoneCrypto.verifyHash(phone, hashed);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("verifyHash()는 다른 입력값을 동일한 해시와 비교하면 false를 반환해야 한다")
    void verifyHash_fail() {
        // given
        String phone = "01022223333";
        String wrong = "01099998888";
        String hashed = phoneCrypto.hash(phone);

        // when
        boolean result = phoneCrypto.verifyHash(wrong, hashed);

        // then
        assertThat(result).isFalse();
    }
}

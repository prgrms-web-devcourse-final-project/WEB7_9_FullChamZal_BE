package back.fcz.domain.member.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PhoneMaskingUtil {

    private PhoneMaskingUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.warn("전화번호 마스킹 실패: null 또는 빈 값");
            throw new IllegalArgumentException("전화번호는 null이거나 비어 있을 수 없습니다.");
        }

        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        if (digitsOnly.length() != 11) {
            throw new IllegalArgumentException(
                    String.format("전화번호는 11자리여야 합니다. (입력: %d자리)", digitsOnly.length())
            );
        }

        String prefix = digitsOnly.substring(0, 3);

        String suffix = digitsOnly.substring(7, 11);

        return String.format("%s-****-%s", prefix, suffix);
    }

    // 전화번호 마스킹 처리
    public static String maskSafely(String phoneNumber) {
        try {
            return mask(phoneNumber);
        } catch (IllegalArgumentException e) {
            log.warn("전화번호 마스킹 실패, 기본값 반환 - error: {}", e.getMessage());
            return "***-****-****";
        }
    }

    // 마스킹 가능 여부
    public static boolean isValidFormat(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        return digitsOnly.length() == 11;
    }
}
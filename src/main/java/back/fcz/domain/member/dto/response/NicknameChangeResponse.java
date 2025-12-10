package back.fcz.domain.member.dto.response;

import java.time.LocalDateTime;

/**
 * 닉네임 변경 응답 DTO
 */
public record NicknameChangeResponse(
        String nickname,
        LocalDateTime nextChangeableDate  // 다음 변경 가능 날짜 (현재 + 90일)
) {
    public static NicknameChangeResponse of(String nickname, LocalDateTime nextChangeableDate) {
        return new NicknameChangeResponse(nickname, nextChangeableDate);
    }
}
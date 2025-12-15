package back.fcz.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

// 회원 정보 수정 응답 DTO
public record MemberUpdateResponse(

        @Schema(description = "수정 성공 메시지", example = "회원 정보가 수정되었습니다.")
        String message,

        @Schema(description = "수정된 항목 목록", example = "[\"닉네임\", \"전화번호\"]")
        List<String> updatedFields,

        @Schema(description = "다음 닉네임 변경 가능 날짜 (닉네임 변경 시에만)",
                example = "2025-03-11T10:30:00")
        LocalDateTime nextNicknameChangeDate
) {
    public static MemberUpdateResponse of(List<String> updatedFields, LocalDateTime nextNicknameChangeDate) {
        return new MemberUpdateResponse(
                "회원 정보가 수정되었습니다.",
                updatedFields,
                nextNicknameChangeDate
        );
    }

    public static MemberUpdateResponse of(List<String> updatedFields) {
        return new MemberUpdateResponse(
                "회원 정보가 수정되었습니다.",
                updatedFields,
                null
        );
    }
}

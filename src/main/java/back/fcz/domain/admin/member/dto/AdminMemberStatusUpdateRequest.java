package back.fcz.domain.admin.member.dto;

import back.fcz.domain.member.entity.MemberStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1-3 회원 상태 변경 요청 DTO
 * PATCH /api/v1/admin/members/{memberId}/status
 */
@Getter
@NoArgsConstructor
public class AdminMemberStatusUpdateRequest {

    /**
     * 변경할 회원 상태
     * - ACTIVE
     * - STOP
     * - EXIT
     */
    @NotNull(message = "변경할 회원 상태는 필수입니다.")
    private MemberStatus status;

    /**
     * 제재/변경 사유
     * - 아직 별도 테이블에 저장하진 않고, 나중에 제재 로그 생기면 연동 예정 (TODO)
     */
    private String reason;

    /**
     * 정지 종료 시각 (선택)
     * - 지금은 별도 필드에 저장하지 않고, 응답에만 그대로 실어줌 (TODO)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sanctionUntil;
}

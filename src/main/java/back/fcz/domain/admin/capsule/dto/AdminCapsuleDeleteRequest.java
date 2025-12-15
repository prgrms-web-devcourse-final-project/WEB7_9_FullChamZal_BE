package back.fcz.domain.admin.capsule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminCapsuleDeleteRequest {

    @NotNull
    private Boolean deleted;  // true → 삭제, false → 복구

    private String reason;    // 관리자가 남기는 메모 (나중에 제재 로그 테이블 생기면 같이 저장)
}

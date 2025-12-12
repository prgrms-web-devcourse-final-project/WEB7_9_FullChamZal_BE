package back.fcz.domain.admin.capsule.dto;

import back.fcz.domain.capsule.entity.Capsule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCapsuleSummaryResponse {

    private Long id;                 // capsule_id
    private String title;            // 제목
    private String writerNickname;   // 작성 당시 닉네임 (capsule.nickname)
    private String visibility;       // PUBLIC / PRIVATE
    private String unlockType;       // TIME / LOCATION / TIME_AND_LOCATION
    private LocalDateTime unlockAt;  // 해제 예정 시각 (시간 기반일 때)
    private LocalDateTime createdAt; // 생성 시각

    private int currentViewCount;    // 현재 조회 인원 (capsule.currentViewCount)
    private int maxViewCount;        // 최대 조회 인원 (선착순, null이면 무제한 → 0 으로 내려줄 수 있음)
    private boolean deleted;         // isDeleted

    // 통계성 데이터 (지금은 0으로 두고, 나중에 report/bookmark 테이블 붙이기)
    private long reportCount;
    private long bookmarkCount;

    public static AdminCapsuleSummaryResponse from(Capsule capsule) {
        return AdminCapsuleSummaryResponse.builder()
                .id(capsule.getCapsuleId())
                .title(capsule.getTitle())
                .writerNickname(capsule.getNickname())
                .visibility(capsule.getVisibility())
                .unlockType(capsule.getUnlockType())
                .unlockAt(capsule.getUnlockAt())
                .createdAt(capsule.getCreatedAt())
                .currentViewCount(capsule.getCurrentViewCount())
                .maxViewCount(capsule.getMaxViewCount()) // null 가능하면 0 처리하는 헬퍼 하나 둬도 됨
//                .deleted(capsule.isDeleted())
                .reportCount(0L)      // TODO: report 테이블 연동
                .bookmarkCount(0L)    // TODO: bookmark 테이블 연동
                .build();
    }
}

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
    private int maxViewCount;        // 최대 조회 인원
    private boolean deleted;         // isDeleted != 0

    private long reportCount;
    private long bookmarkCount;      // TODO

    public static AdminCapsuleSummaryResponse from(Capsule capsule) {
        return from(capsule, 0L, 0L);
    }

    public static AdminCapsuleSummaryResponse from(Capsule capsule, long reportCount, long bookmarkCount) {
        return AdminCapsuleSummaryResponse.builder()
                .id(capsule.getCapsuleId())
                .title(capsule.getTitle())
                .writerNickname(capsule.getNickname())
                .visibility(capsule.getVisibility())
                .unlockType(capsule.getUnlockType())
                .unlockAt(capsule.getUnlockAt())
                .createdAt(capsule.getCreatedAt())
                .currentViewCount(capsule.getCurrentViewCount())
                .maxViewCount(capsule.getMaxViewCount())
                .deleted(isDeleted(capsule))
                .reportCount(reportCount)
                .bookmarkCount(bookmarkCount)
                .build();
    }

    private static boolean isDeleted(Capsule capsule) {
        Integer v = capsule.getIsDeleted();
        return v != null && v != 0;
    }
}

package back.fcz.domain.admin.capsule.dto;

import back.fcz.domain.capsule.entity.Capsule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCapsuleDetailResponse {

    private Long id;

    // 기본 정보
    private String title;
    private String content;
    private String writerNickname;
    private String capsuleColor;
    private String capsulePackingColor;
    private String visibility;

    // 해제 조건
    private String unlockType;
    private LocalDateTime unlockAt;
    private String locationName;
    private Double locationLat;
    private Double locationLng;
    private int locationRadiusM;

    // 통계 / 상태
    private int currentViewCount;
    private Integer maxViewCount;
    private boolean deleted;
    private boolean protectedCapsule;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 간단 통계
    private long reportCount;
    private long bookmarkCount;

    public static AdminCapsuleDetailResponse from(Capsule capsule) {
        return from(capsule, 0L, 0L);
    }

    public static AdminCapsuleDetailResponse from(Capsule capsule, long reportCount, long bookmarkCount) {
        return AdminCapsuleDetailResponse.builder()
                .id(capsule.getCapsuleId())
                .title(capsule.getTitle())
                .content(capsule.getContent())
                .writerNickname(capsule.getNickname())
                .capsuleColor(capsule.getCapsuleColor())
                .capsulePackingColor(capsule.getCapsulePackingColor())
                .visibility(capsule.getVisibility())
                .unlockType(capsule.getUnlockType())
                .unlockAt(capsule.getUnlockAt())
                .locationName(capsule.getLocationName())
                .locationLat(capsule.getLocationLat())
                .locationLng(capsule.getLocationLng())
                .locationRadiusM(capsule.getLocationRadiusM())
                .currentViewCount(capsule.getCurrentViewCount())
                .maxViewCount(capsule.getMaxViewCount())
                .deleted(isDeleted(capsule))
                .protectedCapsule(isProtected(capsule))
                .createdAt(capsule.getCreatedAt())
                .updatedAt(capsule.getUpdatedAt())
                .reportCount(reportCount)
                .bookmarkCount(bookmarkCount)
                .build();
    }

    private static boolean isDeleted(Capsule capsule) {
        Integer v = capsule.getIsDeleted();
        return v != null && v != 0;
    }

    private static boolean isProtected(Capsule capsule) {
        Integer v = capsule.getIsProtected();
        return v != null && v == 1;
    }
}

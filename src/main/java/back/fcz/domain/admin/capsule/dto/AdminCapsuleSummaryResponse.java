package back.fcz.domain.admin.capsule.dto;

import back.fcz.domain.capsule.entity.Capsule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCapsuleSummaryResponse {

    private final Long id;
    private final String uuid;

    private final String title;
    private final String writerNickname;   // 작성 당시 닉네임
    private final String visibility;       // PUBLIC / PRIVATE
    private final String unlockType;       // TIME / LOCATION / TIME_AND_LOCATION
    private final LocalDateTime unlockAt;
    private final LocalDateTime unlockUntil;

    // 장소 정보
    private final String locationAlias;    // 별칭 (Capsule.locationName)
    private final String address;          // 실제 주소 (Capsule.address)
    private final Double locationLat;
    private final Double locationLng;

    // PRIVATE 캡슐일 때만 채우는 값 (CapsuleRecipient.recipientName)
    private final String recipientName;

    private final int currentViewCount;
    private final int maxViewCount;

    private final boolean deleted;         // isDeleted != 0
    private final boolean protectedCapsule; // isProtected == 1 (보호)
    private final boolean unlocked; // 조회 여부

    private final long reportCount;        // 캡슐 신고 수
    private final long bookmarkCount;      //

    private final LocalDateTime createdAt;

    public static AdminCapsuleSummaryResponse of(
            Capsule capsule,
            String recipientName,
            long reportCount,
            long bookmarkCount,
            boolean isUnlocked
    ) {
        boolean deleted = capsule.getIsDeleted() != 0;
        boolean protectedCapsule = capsule.getIsProtected() == 1; // ✅ 보호:1, 미보호:0

        return AdminCapsuleSummaryResponse.builder()
                .id(capsule.getCapsuleId())
                .uuid(capsule.getUuid())
                .title(capsule.getTitle())
                .writerNickname(capsule.getNickname())
                .visibility(capsule.getVisibility())
                .unlockType(capsule.getUnlockType())
                .unlockAt(capsule.getUnlockAt())
                .unlockUntil(capsule.getUnlockUntil())

                .locationAlias(capsule.getLocationName())
                .address(capsule.getAddress())
                .locationLat(capsule.getLocationLat())
                .locationLng(capsule.getLocationLng())

                .recipientName(recipientName)

                .currentViewCount(capsule.getCurrentViewCount())
                .maxViewCount(capsule.getMaxViewCount())

                .deleted(deleted)
                .protectedCapsule(protectedCapsule)
                .unlocked(isUnlocked)

                .reportCount(reportCount)
                .bookmarkCount(bookmarkCount)
                .createdAt(capsule.getCreatedAt())
                .build();
    }
}

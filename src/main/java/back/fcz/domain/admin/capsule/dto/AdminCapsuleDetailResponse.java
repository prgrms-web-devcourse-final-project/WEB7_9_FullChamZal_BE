package back.fcz.domain.admin.capsule.dto;

import back.fcz.domain.capsule.entity.Capsule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCapsuleDetailResponse {

    private final Long id;
    private final Long writerId;
    private final String writerNickname;

    private final String uuid;
    private final String title;
    private final String content;

    private final String visibility;
    private final String unlockType;
    private final LocalDateTime unlockAt;
    private final LocalDateTime unlockUntil;

    // 장소 정보
    private final String locationAlias;   // 별칭
    private final String address;         // 실제 주소
    private final Double locationLat;
    private final Double locationLng;
    private final int locationRadiusM;

    // PRIVATE 캡슐일 때만 채우는 값
    private final String recipientName;

    private final int currentViewCount;
    private final int maxViewCount;

    private final boolean deleted;
    private final boolean protectedCapsule; // 보호:1

    private final long reportCount;
    private final long bookmarkCount; // TODO

    private final LocalDateTime createdAt;

    public static AdminCapsuleDetailResponse of(
            Capsule capsule,
            String recipientName,
            long reportCount,
            long bookmarkCount
    ) {
        boolean deleted = capsule.getIsDeleted() != 0;
        boolean protectedCapsule = capsule.getIsProtected() == 1;

        return AdminCapsuleDetailResponse.builder()
                .id(capsule.getCapsuleId())
                .writerId(capsule.getMemberId() != null ? capsule.getMemberId().getMemberId() : null)
                .writerNickname(capsule.getNickname())

                .uuid(capsule.getUuid())
                .title(capsule.getTitle())
                .content(capsule.getContent())

                .visibility(capsule.getVisibility())
                .unlockType(capsule.getUnlockType())
                .unlockAt(capsule.getUnlockAt())
                .unlockUntil(capsule.getUnlockUntil())

                .locationAlias(capsule.getLocationName())
                .address(capsule.getAddress())
                .locationLat(capsule.getLocationLat())
                .locationLng(capsule.getLocationLng())
                .locationRadiusM(capsule.getLocationRadiusM())

                .recipientName(recipientName)

                .currentViewCount(capsule.getCurrentViewCount())
                .maxViewCount(capsule.getMaxViewCount())

                .deleted(deleted)
                .protectedCapsule(protectedCapsule)

                .reportCount(reportCount)
                .bookmarkCount(bookmarkCount)
                .createdAt(capsule.getCreatedAt())
                .build();
    }
}

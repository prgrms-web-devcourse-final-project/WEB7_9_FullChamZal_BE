package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;

import java.time.LocalDateTime;

public record CapsuleConditionResponseDTO(
        Long capsuleId,            // 캡슐 id
        String capsuleColor,         // 편지지 색상
        String capsulePackingColor,  // 편지지 봉투 색상
        String recipient,            // 수신자 이름
        String senderNickname,               // 송신자 이름
        String title,                // 제목
        String content,              // 내용 // 내용은 일정 글자수 넘어가면 ...으로 처리
        LocalDateTime createAt,      // 보낸 날짜
        boolean viewStatus,          // 조회 여부

        String unlockType,        // 해제 조건

        LocalDateTime unlockAt,    // 해제 세부 조건(열람 가능 시간) : 시간 기반 해제 일시
        LocalDateTime unlockUntil, // 해제 세부 조건(열람 마감 시간) : 시간 기반 해제 일시
        String locationName,       // 장소 이름(별명)
        Double locationLat,        // 해제 세부 조건(위도) : 위치 기반 해제 일시
        Double locationLng,         // 해제 세부 조건(경도) : 위치 기반 해제 일시
        int locationRadiusM,

        String result // 해제 성공 여부
) {
    //개인 캡슐이며 수신자가 회원인 경우
    public static CapsuleConditionResponseDTO from(Capsule capsule, CapsuleRecipient recipient) {
        return new CapsuleConditionResponseDTO(
                // 캡슐 정보
                capsule.getCapsuleId(),
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor(),
                recipient.getRecipientName(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getCurrentViewCount() > 0,

                capsule.getUnlockType(),

                capsule.getUnlockAt(),
                capsule.getUnlockUntil(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng(),
                capsule.getLocationRadiusM(),

                "SUCCESS"
        );
    }

    //개인 캡슐이며 수신자가 회원인 경우
    public static CapsuleConditionResponseDTO from(Capsule capsule) {
        return new CapsuleConditionResponseDTO(
                // 캡슐 정보
                capsule.getCapsuleId(),
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor(),
                capsule.getReceiverNickname(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getCurrentViewCount() > 0,

                capsule.getUnlockType(),

                capsule.getUnlockAt(),
                capsule.getUnlockUntil(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng(),
                capsule.getLocationRadiusM(),

                "SUCCESS"
        );
    }

    //공개 캡슐의 경우(수신자 없음)
    public static CapsuleConditionResponseDTO from(Capsule capsule, boolean viewStatus) {
        return new CapsuleConditionResponseDTO(
                // 캡슐 정보
                capsule.getCapsuleId(),
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor(),
                null,
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                viewStatus,

                capsule.getUnlockType(),

                capsule.getUnlockAt(),
                capsule.getUnlockUntil(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng(),
                capsule.getLocationRadiusM(),

                "SUCCESS"
        );
    }

    public static CapsuleConditionResponseDTO failFrom(Capsule capsule) {
        return new CapsuleConditionResponseDTO(
                capsule.getCapsuleId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,

                capsule.getUnlockType(),

                capsule.getUnlockAt(),
                capsule.getUnlockUntil(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng(),
                capsule.getLocationRadiusM(),

                "FAIL"
        );
    }
}
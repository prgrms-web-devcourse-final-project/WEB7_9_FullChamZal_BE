package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;

import java.time.LocalDateTime;

public record CapsuleReadResponseDTO(
        Long capsuleId,            // 캡슐 id
        String capsuleColor,         // 편지지 색상
        String capsulePackingColor,  // 편지지 봉투 색상
        String recipient,            // 수신자 이름
        String sender,               // 송신자 이름
        String title,                // 제목
        String content,              // 내용 // 내용은 일정 글자수 넘어가면 ...으로 처리
        LocalDateTime createAt,      // 보낸 날짜
        String unlockType,        // 해제 조건
        LocalDateTime unlockAt,   // 해제 세부 조건(시간) : 시간 기반 해제 일시
        String locationName,     // 장소 이름(별명)
        Double locationLat,       // 해제 세부 조건(위도) : 위치 기반 해제 일시
        Double locationLng       // 해제 세부 조건(경도) : 위치 기반 해제 일시
) {
    //개인 캡슐의 경우
    public static CapsuleReadResponseDTO from(Capsule capsule, CapsuleRecipient recipient) {
        return new CapsuleReadResponseDTO(
                // 캡슐 정보
                capsule.getCapsuleId(),
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor(),
                recipient.getRecipientName(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getUnlockType(),
                capsule.getUnlockAt(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng()
        );
    }

    //공개 캡슐의 경우(수신자 없음)
    public static CapsuleReadResponseDTO from(Capsule capsule) {
        return new CapsuleReadResponseDTO(
                // 캡슐 정보
                capsule.getCapsuleId(),
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor(),
                null,
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getUnlockType(),
                capsule.getUnlockAt(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng()
        );
    }
}

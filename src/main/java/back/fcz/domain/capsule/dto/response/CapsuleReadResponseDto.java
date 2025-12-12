package back.fcz.domain.capsule.dto.response;

import java.time.LocalDateTime;

public record CapsuleReadResponseDto(
        Long capsuleId,            // 캡슐 id
        String capsuleColor,         // 편지지 색상
        String capsulePackingColor,  // 편지지 봉투 색상
        String recipient,            // 수신자 이름
        String sender,               // 송신자 이름
        String title,                // 제목
        String content,              // 내용 // 내용은 일정 글자수 넘어가면 ...으로 처리
        LocalDateTime createAt,      // 보낸 날짜
        boolean viewStatus,          // 조회 여부 // currentViewCount > 0 -> 1

        String unlockType,        // 해제 조건

        LocalDateTime unlockAt,   // 해제 세부 조건(시간) : 시간 기반 해제 일시
        String locationName,     // 장소 이름(별명)
        Double locationLat,       // 해제 세부 조건(위도) : 위치 기반 해제 일시
        Double locationLng       // 해제 세부 조건(경도) : 위치 기반 해제 일시
) {
}

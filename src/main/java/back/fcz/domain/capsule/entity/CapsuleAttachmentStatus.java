package back.fcz.domain.capsule.entity;

public enum CapsuleAttachmentStatus {
    TEMP,   // 임시 저장 상태
    USED,   // 캡슐에 첨부된 상태
    DELETED, // 삭제되거나 필터링된 상태
    UPLOADING, // 서버에서 PUT url 발급 후 아직 업로드가 완료되지 않은 상태
    PENDING // 이미지 필터링 중인 상태
}

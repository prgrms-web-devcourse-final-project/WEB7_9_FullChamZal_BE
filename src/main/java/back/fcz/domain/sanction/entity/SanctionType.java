package back.fcz.domain.sanction.entity;

public enum SanctionType {
    STOP,       // 정지
    RESTORE,    // 해제(복구)
    EXIT,        // 강제 탈퇴(필요 시)
    AUTO_TEMPORARY_SUSPENSION  // 자동 임시 정지 (GPS 스푸핑 등)
}

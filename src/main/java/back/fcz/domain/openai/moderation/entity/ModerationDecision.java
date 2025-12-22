package back.fcz.domain.openai.moderation.entity;

public enum ModerationDecision {
    PASS,     // flagged=false
    FLAGGED,    // flagged=true
    ERROR,      // 호출 실패/파싱 실패 등
    SKIPPED     // 입력 텍스트가 사실상 없음
}

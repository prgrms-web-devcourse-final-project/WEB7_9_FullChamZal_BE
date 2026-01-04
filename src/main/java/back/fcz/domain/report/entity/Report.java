package back.fcz.domain.report.entity;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "report", indexes = {
        @Index(name = "idx_report_capsule_id", columnList = "capsule_id"),
        @Index(name = "idx_report_reporter_id", columnList = "reporter_id"),
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_created_at", columnList = "created_at")
})   // 스키마 이름에 맞게 조정 가능
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;                          // 신고 PK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;                 // 신고 대상 캡슐

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Member reporter;                 // 신고자(회원일 경우)

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", length = 30, nullable = false)
    private ReportReasonType reasonType;     // 신고 유형

    @Column(name = "reason_detail", columnDefinition = "TEXT", nullable = false)
    private String reasonDetail;             // 상세 사유

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReportStatus status;             // 처리 상태

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;         // 신고 일시

    @Column(name = "processed_at")
    private LocalDateTime processedAt;       // 처리 완료 일시

    @Column(name = "processed_by")
    private Long processedBy;                // 처리한 관리자 member_id

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;                // 관리자 메모

    @Builder
    private Report(
            Capsule capsule,
            Member reporter,
            ReportReasonType reasonType,
            String reasonDetail,
            ReportStatus status,
            LocalDateTime createdAt,
            LocalDateTime processedAt,
            Long processedBy,
            String adminMemo
    ) {
        this.capsule = capsule;
        this.reporter = reporter;
        this.reasonType = reasonType;
        this.reasonDetail = reasonDetail;
        this.status = status != null ? status : ReportStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.processedAt = processedAt;
        this.processedBy = processedBy;
        this.adminMemo = adminMemo;
    }

    // == 도메인 로직 ==

    /** 신고 검토 시작 (상태 PENDING → REVIEWING) */
    public void startReview(long adminId) {
        if (this.status != ReportStatus.PENDING) {
            return;
        }
        this.status = ReportStatus.REVIEWING;
        this.processedBy = adminId;
    }

    /** 신고 수락 */
    public void accept(long adminId, String adminMemo, LocalDateTime now) {
        this.status = ReportStatus.ACCEPTED;
        this.processedBy = adminId;
        this.processedAt = now;
        this.adminMemo = adminMemo;
    }

    /** 신고 기각 */
    public void reject(long adminId, String adminMemo, LocalDateTime now) {
        this.status = ReportStatus.REJECTED;
        this.processedBy = adminId;
        this.processedAt = now;
        this.adminMemo = adminMemo;
    }

    public boolean isDone() {
        return this.status == ReportStatus.ACCEPTED || this.status == ReportStatus.REJECTED;
    }
}

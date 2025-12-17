package back.fcz.domain.sanction.entity;

import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_sanction_history",
        indexes = {
                @Index(name = "idx_msh_member", columnList = "member_id"),
                @Index(name = "idx_msh_admin", columnList = "admin_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSanctionHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", nullable = false, length = 30)
    private SanctionType sanctionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", nullable = false, length = 20)
    private MemberStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", nullable = false, length = 20)
    private MemberStatus afterStatus;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "sanction_until")
    private LocalDateTime sanctionUntil;

    @Builder
    private MemberSanctionHistory(
            Long memberId,
            Long adminId,
            SanctionType sanctionType,
            MemberStatus beforeStatus,
            MemberStatus afterStatus,
            String reason,
            LocalDateTime sanctionUntil
    ) {
        this.memberId = memberId;
        this.adminId = adminId;
        this.sanctionType = sanctionType;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.reason = reason;
        this.sanctionUntil = sanctionUntil;
    }

    public static MemberSanctionHistory create(
            Long memberId,
            Long adminId,
            SanctionType sanctionType,
            MemberStatus beforeStatus,
            MemberStatus afterStatus,
            String reason,
            LocalDateTime sanctionUntil
    ) {
        return MemberSanctionHistory.builder()
                .memberId(memberId)
                .adminId(adminId)
                .sanctionType(sanctionType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .reason(reason)
                .sanctionUntil(sanctionUntil)
                .build();
    }
}

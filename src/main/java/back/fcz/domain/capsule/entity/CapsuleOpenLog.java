package back.fcz.domain.capsule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "capsule_openLog",
        indexes = {
                @Index(name = "idx_col_capsule_member_time",
                        columnList = "capsule_id, member_id, opened_at"),
                @Index(name = "idx_col_capsule_ip_time",
                        columnList = "capsule_id, ip_address, opened_at"),
                @Index(name = "idx_col_status",
                        columnList = "status"),
                @Index(name = "idx_col_capsule_member_status",
                        columnList = "capsule_id, member_id, status"),
                @Index(name = "idx_col_capsule_ip_status",
                        columnList = "capsule_id, ip_address, status")
        }
)
public class CapsuleOpenLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsuleId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "viewer_type", nullable = false)
    private String viewerType;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CapsuleOpenStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", length = 50)
    private AnomalyType anomalyType;

    public void updateStatus(CapsuleOpenStatus status) {
        this.status = status;
    }

    public void markAsAnomaly(AnomalyType anomalyType) {
        this.anomalyType = anomalyType;
        this.status = CapsuleOpenStatus.SUSPICIOUS;
    }
}

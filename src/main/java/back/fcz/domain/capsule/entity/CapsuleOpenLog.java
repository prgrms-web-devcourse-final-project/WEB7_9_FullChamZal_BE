package back.fcz.domain.capsule.entity;

import back.fcz.domain.member.entity.Member;
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
@Table(name = "capsule_openLog")
public class CapsuleOpenLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsuleId;

    //회원 엔티티의 이름이 정해저야 합니다. 임시로 Member로 했습니다.
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member memberId;

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
}

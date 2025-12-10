package back.fcz.domain.capsule.entity;

import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity

public class Capsule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long capsuleId;           // 캡슐 고유 ID

/*
    회원 엔티티의 이름이 정해져야 합니다. 임시로 Member로 했습니다.
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member memberId;            // 회원 고유 ID
*/
    @Column(name = "uuid", nullable = false)
    private String uuid;              // URL용 UUIDv4(난수화)
    @Column(name = "nickname", nullable = false)
    private String nickname;          // 작성 당시 닉네임
    @Column(name = "title")
    private String title;             // 캡슐 제목
    @Column(name = "content", nullable = false)
    private String content;           // 캡슐 본문
    @Column(name = "capsule_color", nullable = false)
    private String capsuleColor;       // 편지지 색상
    @Column(name = "capsule_packing_color", nullable = false)
    private String capsulePackingColor; // 편지지 봉투 색상
    @Column(name = "visibility", nullable = false)
    private String visibility;        // 공개범위 : PRIVATE(개인), PUBLIC(공개)
    @Column(name = "unlock_type", nullable = false)
    private String unlockType;       // 해제 조건 : TIME, LOCATION, TIME_AND_LOCATION
    @Column(name = "unlock_at")
    private LocalDateTime unlockAt;  // 해제 세부 조건 : 시간 기반 해제 일시
    @Column(name = "location_name")
    private String locationName;      // 장소 이름(별명)
    @Column(name = "location_lat")
    private int locationLat;       // 위도
    @Column(name = "location_lng")
    private int locationLng;       // 경도

    @Column(name = "location_radius_m")
    private int locationRadiusM;      // 조회 반경 : 50, 100, 300, 500, 1000
    @Column(name = "max_view_count")
    private int maxViewCount;         // 선착순 제한 인원(NULL이면 무제한)
    @Column(name = "current_view_count")
    private int currentViewCount;     // 현재 조회 인원
    @Column(name = "is_deleted")
    private boolean isDeleted;         // 삭제 여부(Soft Delete, 삭제 전0, 삭제후 1)
    @Column(name = "is_protected")
    private boolean isProtected;      // 보호 여부

}

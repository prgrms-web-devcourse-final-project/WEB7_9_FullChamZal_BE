package back.fcz.domain.capsule.entity;

import back.fcz.domain.member.entity.Member;
import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Capsule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long capsuleId;           // 캡슐 고유 ID


    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member memberId;            // 회원 고유 ID

    @Column(name = "uuid", nullable = false)
    private String uuid;              // URL용 UUIDv4(난수화)

    @Column(name = "nickname", nullable = false)
    private String nickname;          // 작성 당시 닉네임
    @Column(name = "title")
    private String title;             // 캡슐 제목
    @Column(name = "content", nullable = false)
    private String content;           // 캡슐 본문

    @Column(name = "cap_password")
    private String capPassword; // 캡슐 비밀번호

    @Column(name = "capsule_color", nullable = false)
    private String capsuleColor;       // 편지지 색상
    @Column(name = "capsule_packing_color", nullable = false)
    private String capsulePackingColor; // 편지지 봉투 색상
    @Column(name = "visibility", nullable = false)
    private String visibility;        // 공개범위 : PRIVATE(개인), PUBLIC(공개)

    @Column(name = "unlock_type", nullable = false)
    private String unlockType;       // 해제 조건 : TIME, LOCATION, TIME_AND_LOCATION
    @Column(name = "unlock_at")
    private LocalDateTime unlockAt;    // 캡슐 열람 가능 시간
    @Column(name = "unlock_until")
    private LocalDateTime unlockUntil; // 캡슐 열람 마감 시간
    @Column(name = "location_name")
    private String locationName;      // 장소 이름(별명)
    @Column(name = "location_lat")
    private Double locationLat;       // 위도
    @Column(name = "location_lng")
    private Double locationLng;       // 경도

    @Column(name = "location_radius_m")
    private int locationRadiusM;      // 조회 반경 : 50, 100, 300, 500, 1000
    @Column(name = "max_view_count")
    private int maxViewCount;         // 선착순 제한 인원(NULL이면 무제한)
    @Column(name = "current_view_count")
    private int currentViewCount = 0;     // 현재 조회 인원
    @Column(name = "is_deleted")
    private int isDeleted = 0;         // 삭제 여부(Soft Delete, 삭제 전 0, 삭제후 1, , 관리자 삭제 후 2)
    @Column(name = "is_protected")
    private int isProtected = 0;      // 보호 여부(보호 : 0, 미보호 : 1)

    public void setMemberId(Member member){
        this.memberId = member;
    }

    public void setUuid(String uuid){
        this.uuid = uuid;
    }

    public void setCapPassword(String capPassword){
        this.capPassword = capPassword;
    }

    public void setProtected(int protectedValue){
        this.isProtected = protectedValue;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setContent(String content){
        this.content = content;
    }

    public void increasedViewCount() {
        this.currentViewCount++;
    }

    public void setIsDeleted(int i) {
        this.isDeleted = i;
    }
}

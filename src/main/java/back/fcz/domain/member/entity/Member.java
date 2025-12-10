package back.fcz.domain.member.entity;

import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "phone_number", nullable = false, length = 500)
    private String phoneNumber; // AES-256 암호화된 전화번호 (IV 포함)

    @Column(name = "phone_hash", nullable = false, unique = true, length = 64)
    private String phoneHash; // SHA-256 해시 (검색용)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id", length = 100)
    private String oauthId;

    @Builder
    private Member(String userId, String passwordHash, String name, String nickname,
                   String phoneNumber, String phoneHash, MemberStatus status, MemberRole role,
                   OAuthProvider oauthProvider, String oauthId) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.phoneHash = phoneHash;
        this.status = status != null ? status : MemberStatus.ACTIVE;
        this.role = role != null ? role : MemberRole.USER;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }
}

package back.fcz.domain.member.entity;

import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member",
        indexes = {
                @Index(name = "idx_member_phone_hash", columnList = "phone_hash", unique = true),
                @Index(name = "idx_member_user_id", columnList = "user_id", unique = true)
        }
)
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

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", length = 20)
    private String nickname;

    @Column(name = "phone_number", length = 500)
    private String phoneNumber; // AES-256 암호화된 전화번호 (IV 포함)

    @Column(name = "phone_hash", unique = true, length = 64)
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

    private LocalDateTime nicknameChangedAt;

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
        this.nicknameChangedAt = null;
    }

    public static Member create(String userId, String passwordHash, String name, String nickname,
                                String encryptedPhone, String phoneHash) {
        Member member = new Member(
                userId,
                passwordHash,
                name,
                nickname,
                encryptedPhone,
                phoneHash,
                MemberStatus.ACTIVE,
                MemberRole.USER,
                null,
                null
        );

        return member;
    }

    public static Member createOAuth(String userId, String name, String oauthId) {
        Member member = new Member(
                userId,
                null,
                name,
                null,
                null,
                null,
                MemberStatus.ACTIVE,
                MemberRole.USER,
                OAuthProvider.GOOGLE,
                oauthId
        );

        return member;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE && this.getDeletedAt() == null;
    }

    public void updateNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 null이거나 빈 값일 수 없습니다.");
        }

        this.nickname = newNickname;
        this.nicknameChangedAt = LocalDateTime.now();
    }

    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("비밀번호 해시는 null이거나 빈 값일 수 없습니다.");
        }

        this.passwordHash = newPasswordHash;
    }

    public void updatePhoneNumber(String encryptedPhone, String phoneHash) {
        if (encryptedPhone == null || encryptedPhone.isBlank()) {
            throw new IllegalArgumentException("암호화된 전화번호는 null이거나 빈 값일 수 없습니다.");
        }
        if (phoneHash == null || phoneHash.isBlank()) {
            throw new IllegalArgumentException("전화번호 해시는 null이거나 빈 값일 수 없습니다.");
        }

        this.phoneNumber = encryptedPhone;
        this.phoneHash = phoneHash;
    }

    public void anonymize() {
        this.userId = "deleted_user_" + this.memberId;
        this.name = "탈퇴회원";
        this.phoneNumber = "DELETED_" + this.memberId;
        this.phoneHash = "DELETED_" + this.memberId;
    }

    public void updateStatus(MemberStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("회원 상태는 null일 수 없습니다.");
        }
        this.status = newStatus;
    }

    // 테스트를 위해서 작성한 팩토리 메소드
    public static Member testMember(Long id, String userId, String name) {
        Member m = new Member(
                userId,
                "test-password",
                name,
                name,           // nickname
                "encrypted-phone",
                "hash-phone",
                MemberStatus.ACTIVE,
                MemberRole.USER,
                null,
                null
        );

        // ID 강제 주입 (테스트에서만)
        m.memberId = id;

        return m;
    }
}

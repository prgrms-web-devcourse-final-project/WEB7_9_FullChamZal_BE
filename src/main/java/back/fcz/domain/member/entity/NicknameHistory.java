package back.fcz.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "nickname_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NicknameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "older_nickname", nullable = false)
    private String olderNickname;

    @Column(name = "new_nickname")
    private String newNickname;

    @Builder
    private NicknameHistory(Long memberId, LocalDateTime changedAt, String olderNickname, String newNickname) {
        this.memberId = memberId;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
        this.olderNickname = olderNickname;
        this.newNickname = newNickname;
    }
}

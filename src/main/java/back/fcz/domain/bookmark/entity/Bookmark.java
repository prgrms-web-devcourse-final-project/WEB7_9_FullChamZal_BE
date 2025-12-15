package back.fcz.domain.bookmark.entity;

import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bookmark",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bookmark_member_capsule",
                        columnNames = {"member_id", "capsule_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_member_deleted",
                        columnList = "member_id, deleted_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "capsule_id", nullable = false)
    private Long capsuleId;

    @Builder
    private Bookmark(Long memberId, Long capsuleId) {
        this.memberId = memberId;
        this.capsuleId = capsuleId;
    }

    // 북마크 재활성화
    public void restore() {
        this.clearDeletedAt();
    }
}

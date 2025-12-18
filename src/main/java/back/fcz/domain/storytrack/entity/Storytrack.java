package back.fcz.domain.storytrack.entity;

import back.fcz.domain.member.entity.Member;
import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Storytrack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storytrack_id")
    private Long storytrackId;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "title")
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "track_type")
    private String trackType;

    @Column(name = "is_public")
    private int isPublic; // 비공개 0, 공개 1
}

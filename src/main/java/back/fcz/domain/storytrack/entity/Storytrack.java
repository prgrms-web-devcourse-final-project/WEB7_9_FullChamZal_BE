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
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "track_type", nullable = false)
    private String trackType;

    @Column(name = "is_public", nullable = false)
    private int isPublic; // 비공개 0, 공개 1

    @Column(name = "price")
    private int price;

    @Column(name = "total_steps", nullable = false)
    private int totalSteps;
}

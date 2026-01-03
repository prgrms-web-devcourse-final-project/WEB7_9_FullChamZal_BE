package back.fcz.domain.storytrack.entity;

import back.fcz.domain.member.entity.Member;
import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "storytrack", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorytrackAttachment> storytrackAttachment = new ArrayList();

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "track_type", nullable = false)
    private String trackType; // 순회 방식: SEQUENTIAL(순차), FREE(자유)

    @Column(name = "is_public", nullable = false)
    private int isPublic; // 비공개 0, 공개 1

    @Column(name = "price")
    private int price;

    @Column(name = "total_steps", nullable = false)
    private int totalSteps;

    @Column(name = "is_deleted", nullable = false)
    private int isDeleted; // 미삭제 0, 삭제 1

    @Builder.Default
    @OneToMany(mappedBy = "storytrack", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorytrackStep> steps = new ArrayList<>();

    public void setTotalSteps(int steps){
        this.totalSteps = steps;
    }

    public void setIsDeleted(int isDeleted){
        this.isDeleted = isDeleted;
    }

    public void addStep(StorytrackStep step) {
        steps.add(step);
        step.setStorytrack(this);
    }
}

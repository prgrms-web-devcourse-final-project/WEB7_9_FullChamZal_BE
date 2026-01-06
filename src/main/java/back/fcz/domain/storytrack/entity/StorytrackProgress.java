package back.fcz.domain.storytrack.entity;

import back.fcz.domain.member.entity.Member;
import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StorytrackProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storytrack_progress_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "storytrack_id", nullable = false)
    private Storytrack storytrack;

    @Column(name = "completed_steps", nullable = false)
    private int completedSteps; // 총 완료한 단계의 합

    @Column(name = "last_completed_steps")
    private int lastCompletedStep;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void completeStep(StorytrackStep step, int totalSteps) {
        if (isStepCompleted(step.getStepOrder())) {
            return;
        }

        completedStepSet.add(
                StorytrackProgressStep.builder()
                        .progress(this)
                        .step(step)
                        .completedAt(LocalDateTime.now())
                        .build()
        );

        this.completedSteps += 1;
        this.lastCompletedStep = step.getStepOrder();

        if (this.completedSteps == totalSteps) {
            this.completedAt = LocalDateTime.now();
        }
    }

    // 진행 상황에 따른 진행 단계
    @OneToMany(mappedBy = "progress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StorytrackProgressStep> completedStepSet = new HashSet<>();

    public boolean isStepCompleted(int stepOrder) {
        for (StorytrackProgressStep progressStep : completedStepSet) {
            if (progressStep.getStep().getStepOrder() == stepOrder) {
                return true;
            }
        }
        return false;
    }
}

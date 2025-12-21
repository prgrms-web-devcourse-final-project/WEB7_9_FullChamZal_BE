package back.fcz.domain.storytrack.entity;

import back.fcz.domain.member.entity.Member;
import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
    private int completedSteps;

    @Column(name = "last_completed_steps")
    private int lastCompletedStep;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void completeStep(int stepOrder, int totalSteps) {
        this.completedSteps += 1;
        this.lastCompletedStep = stepOrder;

        if (this.completedSteps == totalSteps) {
            this.completedAt = LocalDateTime.now();
        }
    }
}

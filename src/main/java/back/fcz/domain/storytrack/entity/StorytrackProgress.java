package back.fcz.domain.storytrack.entity;

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
public class StorytrackProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storytrack_progress_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "storytrack_id")
    private Storytrack storytrack;

    @Column(name = "completed_steps")
    private int completedSteps;

    @Column(name = "last_completed_steps")
    private int last_completed_step;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}

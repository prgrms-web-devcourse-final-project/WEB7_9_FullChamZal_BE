package back.fcz.domain.storytrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StorytrackProgressStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "storytrack_progress_id", nullable = false)
    private StorytrackProgress progress;

    @ManyToOne
    @JoinColumn(name = "storytrack_step_id", nullable = false)
    private StorytrackStep step;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}

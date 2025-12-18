package back.fcz.domain.storytrack.entity;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StorytrackStep extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storytrack_step_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "capsule_id")
    private Capsule capsule;

    @ManyToOne
    @JoinColumn(name = "storytrack_id")
    private Storytrack storytrack;

    @Column(name = "step_order")
    private int stepOrder;

    public void setCapsule(Capsule updateCapsule) {
        this.capsule = updateCapsule;
    }
}
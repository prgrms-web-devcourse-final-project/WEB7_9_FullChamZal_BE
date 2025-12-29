// src/test/java/back/fcz/domain/capsule/service/CapsuleHardDeleteServiceTest.java
package back.fcz.domain.capsule.service;

import back.fcz.domain.bookmark.entity.Bookmark;
import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.*;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.openai.moderation.entity.*;
import back.fcz.domain.openai.moderation.repository.ModerationAuditLogRepository;
import back.fcz.domain.report.entity.*;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.domain.storytrack.entity.*;
import back.fcz.domain.storytrack.repository.StorytrackStepRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CapsuleHardDeleteService.class)
class CapsuleHardDeleteServiceTest {

    @Autowired CapsuleHardDeleteService capsuleHardDeleteService;

    @Autowired CapsuleRepository capsuleRepository;
    @Autowired CapsuleAttachmentRepository capsuleAttachmentRepository;
    @Autowired CapsuleLikeRepository capsuleLikeRepository;
    @Autowired CapsuleOpenLogRepository capsuleOpenLogRepository;
    @Autowired CapsuleRecipientRepository capsuleRecipientRepository;
    @Autowired PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;

    @Autowired BookmarkRepository bookmarkRepository;
    @Autowired ModerationAuditLogRepository moderationAuditLogRepository;
    @Autowired ReportRepository reportRepository;
    @Autowired StorytrackStepRepository storytrackStepRepository;

    @Autowired EntityManager em;

    @Test
    void hardDeleteOnce_deletes_children_then_capsule() {
        LocalDateTime now = LocalDateTime.now();

        // Member 생성 (간단히 persist)
        Member member = Member.builder()
                .userId("u1")
                .passwordHash("pw")
                .name("n")
                .nickname("nick")
                .phoneNumber("enc")
                .phoneHash("hash")
                .status(back.fcz.domain.member.entity.MemberStatus.ACTIVE)
                .role(back.fcz.domain.member.entity.MemberRole.USER)
                .build();
        em.persist(member);

        // 후보 Capsule 생성 (TIME, unlockAt 과거, PRIVATE, isProtected=0, isDeleted=1, deletedAt not null)
        Capsule capsule = Capsule.builder()
                .memberId(member)
                .uuid(UUID.randomUUID().toString())
                .nickname("삭제후보")
                .title("t")
                .content("c")
                .capsuleColor("PINK")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .capPassword("pw")
                .unlockType("TIME")
                .unlockAt(now.minusDays(1))
                .isDeleted(1)
                .isProtected(0)
                .currentViewCount(0)
                .build();
        em.persist(capsule);
        em.flush();

        // BaseEntity createdAt/deletedAt 보정(테스트에서 auditing 안 켜져도 조건 통과시키기)
        ReflectionTestUtils.setField(capsule, "createdAt", now.minusDays(10));
        ReflectionTestUtils.setField(capsule, "deletedAt", now.minusDays(2));
        em.flush();

        Long capsuleId = capsule.getCapsuleId();

        // 자식 데이터 생성: Bookmark
        bookmarkRepository.save(Bookmark.builder()
                .memberId(member.getMemberId())
                .capsuleId(capsuleId)
                .build());

        // ModerationAuditLog
        moderationAuditLogRepository.save(ModerationAuditLog.builder()
                .actionType(ModerationActionType.CAPSULE_CREATE)
                .actorMemberId(member.getMemberId())
                .capsuleId(capsuleId)
                .decision(ModerationDecision.PASS)
                .flagged(false)
                .inputHash("hash")
                .model("omni-moderation-latest")
                .rawResponseJson("{}")
                .build());

        // Report
        reportRepository.save(Report.builder()
                .capsule(capsule)
                .reporter(member)
                .reasonType(ReportReasonType.SPAM)
                .reasonDetail("test")
                .status(ReportStatus.PENDING)
                .createdAt(now)
                .build());

        // StorytrackStep (capsule FK)
        Storytrack st = Storytrack.builder()
                .member(member)
                .title("st")
                .trackType("SEQUENTIAL")
                .isPublic(1)
                .isDeleted(0)
                .totalSteps(1)
                .build();
        em.persist(st);

        StorytrackStep step = StorytrackStep.builder()
                .storytrack(st)
                .capsule(capsule)
                .stepOrder(1)
                .build();
        em.persist(step);

        em.flush();

        // 실행
        int deleted = capsuleHardDeleteService.hardDeleteOnce(100);

        // 검증
        assertThat(deleted).isEqualTo(1);
        assertThat(capsuleRepository.findById(capsuleId)).isEmpty();
        assertThat(bookmarkRepository.countByCapsuleId(capsuleId)).isZero();
        assertThat(moderationAuditLogRepository.count()).isZero();
        assertThat(reportRepository.count()).isZero();
        assertThat(storytrackStepRepository.findAll()).isEmpty();
    }
}

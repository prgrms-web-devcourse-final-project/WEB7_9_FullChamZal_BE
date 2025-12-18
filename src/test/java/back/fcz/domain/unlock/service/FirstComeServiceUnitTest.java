package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class FirstComeServiceUnitTest {
    @Autowired
    private FirstComeService firstComeService;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private Capsule testCapsule;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .userId("testuser")
                .passwordHash("password")
                .name("테스트")
                .nickname("테스터")
                .phoneNumber("01012345678")
                .phoneHash("hash")
                .build();
        memberRepository.save(testMember);

        // 선착순 3명 제한 캡슐 생성
        testCapsule = Capsule.builder()
                .memberId(testMember)
                .uuid("test-uuid")
                .nickname("테스터")
                .title("선착순 테스트")
                .content("선착순 3명")
                .visibility("PUBLIC")
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().minusDays(1))
                .capsuleColor("blue")
                .capsulePackingColor("red")
                .maxViewCount(3) // 선착순 3명
                .currentViewCount(0)
                .build();
        capsuleRepository.save(testCapsule);
    }

    @Test
    @DisplayName("동시에 여러 요청 - 정확히 maxViewCount만큼만 성공해야 함")
    void concurrentRequests() throws InterruptedException {
        // given
        int threadCount = 100; // 100명이 동시에 시도
        int maxViewCount = 3; // 3명만 성공해야 함

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100개 스레드가 동시에 조회수 증가 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    // 각 스레드마다 다른 회원으로 시도
                    Member member = Member.builder()
                            .userId("concurrent" + index)
                            .passwordHash("password")
                            .name("동시" + index)
                            .nickname("동시" + index)
                            .phoneNumber("010" + String.format("%08d", index))
                            .phoneHash("hash" + index)
                            .build();
                    memberRepository.save(member);

                    firstComeService.tryIncrementViewCountAndSaveRecipient(
                            testCapsule.getCapsuleId(),
                            member.getMemberId(),
                            LocalDateTime.now()
                    );
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.FIRST_COME_CLOSED) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(maxViewCount); // 정확히 3명만 성공
        assertThat(failCount.get()).isEqualTo(threadCount - maxViewCount); // 나머지 97명은 실패

        Capsule result = capsuleRepository.findById(testCapsule.getCapsuleId()).orElseThrow();
        assertThat(result.getCurrentViewCount()).isEqualTo(maxViewCount);
    }
}
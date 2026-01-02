package back.fcz.domain.sanction.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.MemberStatusCache;
import back.fcz.domain.sanction.constant.SanctionConstants;
import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.entity.SanctionType;
import back.fcz.domain.sanction.properties.SanctionProperties;
import back.fcz.domain.sanction.repository.MemberSanctionHistoryRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanctionServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberSanctionHistoryRepository sanctionHistoryRepository;

    @Mock
    private SanctionProperties sanctionProperties;

    @Mock
    private MemberStatusCache memberStatusCache;

    private SanctionConstants sanctionConstants;
    private SanctionService sanctionService;

    @BeforeEach
    void setUp() {
        sanctionConstants = new SanctionConstants(sanctionProperties, memberRepository);

        Member systemAdmin = createSystemAdmin();
        lenient().when(memberRepository.findByUserId("SYSTEM"))
                .thenReturn(Optional.of(systemAdmin));

        sanctionService = new SanctionService(
                memberRepository,
                sanctionHistoryRepository,
                sanctionConstants,
                memberStatusCache
        );
    }

    // 테스트용 회원 생성
    private Member createMockMember(Long memberId, MemberStatus status) {
        Member member = Member.create(
                "testUser",
                "encodedPassword",
                "홍길동",
                "테스터",
                "encryptedPhone",
                "phoneHash"
        );
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "status", status);
        ReflectionTestUtils.setField(member, "role", MemberRole.USER);
        return member;
    }

    // 시스템 관리자 생성
    private Member createSystemAdmin() {
        Member admin = Member.create(
                "SYSTEM",
                "systemPassword",
                "시스템",
                "시스템",
                "systemPhone",
                "systemHash"
        );
        ReflectionTestUtils.setField(admin, "memberId", 999L);
        ReflectionTestUtils.setField(admin, "status", MemberStatus.ACTIVE);
        ReflectionTestUtils.setField(admin, "role", MemberRole.ADMIN);
        return admin;
    }

    // ========== 자동 정지 처리 테스트 ==========

    @Test
    @DisplayName("자동 정지 처리 - 성공")
    void applyAutoSuspension_success() {
        // Given
        Long memberId = 1L;
        String reason = "의심 활동 누적 (점수: 100점)";
        int days = 7;

        Member member = createMockMember(memberId, MemberStatus.ACTIVE);
        Member systemAdmin = createSystemAdmin();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // When
        sanctionService.applyAutoSuspension(memberId, reason, days);

        // Then
        // 회원 상태가 STOP으로 변경되었는지 확인
        assertEquals(MemberStatus.STOP, member.getStatus(), "회원 상태가 STOP으로 변경되어야 함");

        // 제재 이력이 저장되었는지 확인
        ArgumentCaptor<MemberSanctionHistory> historyCaptor = ArgumentCaptor.forClass(MemberSanctionHistory.class);
        verify(sanctionHistoryRepository).save(historyCaptor.capture());

        MemberSanctionHistory history = historyCaptor.getValue();
        assertEquals(memberId, history.getMemberId(), "제재 대상 회원 ID가 일치해야 함");
        assertEquals(999L, history.getAdminId(), "시스템 관리자 ID가 일치해야 함");
        assertEquals(SanctionType.AUTO_TEMPORARY_SUSPENSION, history.getSanctionType(),
                "제재 유형이 자동 임시 정지여야 함");
        assertEquals(MemberStatus.ACTIVE, history.getBeforeStatus(), "변경 전 상태가 ACTIVE여야 함");
        assertEquals(MemberStatus.STOP, history.getAfterStatus(), "변경 후 상태가 STOP이어야 함");
        assertTrue(history.getReason().contains("자동 제재:"), "사유에 자동 제재 접두사가 포함되어야 함");
        assertTrue(history.getReason().contains(reason), "사유에 원본 사유가 포함되어야 함");
        assertNotNull(history.getSanctionUntil(), "제재 해제 일시가 설정되어야 함");
    }

    @Test
    @DisplayName("자동 정지 처리 - 제재 해제 일시 계산 확인")
    void applyAutoSuspension_sanctionUntilCalculation() {
        // Given
        Long memberId = 1L;
        String reason = "테스트 사유";
        int days = 7;

        Member member = createMockMember(memberId, MemberStatus.ACTIVE);
        Member systemAdmin = createSystemAdmin();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByUserId("SYSTEM")).thenReturn(Optional.of(systemAdmin));

        LocalDateTime before = LocalDateTime.now();

        // When
        sanctionService.applyAutoSuspension(memberId, reason, days);

        LocalDateTime after = LocalDateTime.now();

        // Then
        ArgumentCaptor<MemberSanctionHistory> historyCaptor = ArgumentCaptor.forClass(MemberSanctionHistory.class);
        verify(sanctionHistoryRepository).save(historyCaptor.capture());

        MemberSanctionHistory history = historyCaptor.getValue();
        LocalDateTime sanctionUntil = history.getSanctionUntil();

        // 제재 해제 일시가 현재 시간 + days 범위 내에 있는지 확인
        assertTrue(sanctionUntil.isAfter(before.plusDays(days).minusSeconds(5)),
                "제재 해제 일시가 설정된 기간 이후여야 함");
        assertTrue(sanctionUntil.isBefore(after.plusDays(days).plusSeconds(5)),
                "제재 해제 일시가 적절한 범위 내에 있어야 함");
    }

    @Test
    @DisplayName("자동 정지 처리 - 이미 정지된 회원도 처리 가능")
    void applyAutoSuspension_alreadyStopped() {
        // Given
        Long memberId = 1L;
        String reason = "추가 위반";
        int days = 14;

        Member member = createMockMember(memberId, MemberStatus.STOP);
        Member systemAdmin = createSystemAdmin();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByUserId("SYSTEM")).thenReturn(Optional.of(systemAdmin));

        // When
        sanctionService.applyAutoSuspension(memberId, reason, days);

        // Then
        ArgumentCaptor<MemberSanctionHistory> historyCaptor = ArgumentCaptor.forClass(MemberSanctionHistory.class);
        verify(sanctionHistoryRepository).save(historyCaptor.capture());

        MemberSanctionHistory history = historyCaptor.getValue();
        assertEquals(MemberStatus.STOP, history.getBeforeStatus(),
                "이미 정지된 상태에서도 이력이 기록되어야 함");
        assertEquals(MemberStatus.STOP, history.getAfterStatus(),
                "변경 후 상태도 STOP이어야 함");
    }

    @Test
    @DisplayName("자동 정지 처리 - 회원 없음 예외")
    void applyAutoSuspension_memberNotFound() {
        // Given
        Long memberId = 999L;
        String reason = "테스트";
        int days = 7;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sanctionService.applyAutoSuspension(memberId, reason, days)
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode(),
                "회원을 찾을 수 없는 경우 MEMBER_NOT_FOUND 예외가 발생해야 함");

        // 제재 이력이 저장되지 않았는지 확인
        verify(sanctionHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("자동 정지 처리 - 시스템 관리자 없음 예외")
    void applyAutoSuspension_systemAdminNotFound() {
        // Given
        Long memberId = 1L;
        String reason = "테스트";
        int days = 7;

        Member member = createMockMember(memberId, MemberStatus.ACTIVE);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByUserId("SYSTEM")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                IllegalStateException.class,
                () -> sanctionService.applyAutoSuspension(memberId, reason, days),
                "시스템 관리자를 찾을 수 없으면 IllegalStateException이 발생해야 함"
        );

        // 회원 상태는 변경되었지만 이력 저장 시 예외 발생
        assertEquals(MemberStatus.STOP, member.getStatus(),
                "예외 발생 전에 회원 상태는 변경됨 (트랜잭션 롤백은 실제 환경에서만 동작)");
    }

    // ========== 자동 정지 해제 테스트 ==========

    @Test
    @DisplayName("자동 정지 해제 - 성공")
    void restoreSuspension_success() {
        // Given
        Long memberId = 1L;

        Member member = createMockMember(memberId, MemberStatus.STOP);
        Member systemAdmin = createSystemAdmin();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByUserId("SYSTEM")).thenReturn(Optional.of(systemAdmin));

        // When
        sanctionService.restoreSuspension(memberId);

        // Then
        // 회원 상태가 ACTIVE로 변경되었는지 확인
        assertEquals(MemberStatus.ACTIVE, member.getStatus(), "회원 상태가 ACTIVE로 변경되어야 함");

        // 복구 이력이 저장되었는지 확인
        ArgumentCaptor<MemberSanctionHistory> historyCaptor = ArgumentCaptor.forClass(MemberSanctionHistory.class);
        verify(sanctionHistoryRepository).save(historyCaptor.capture());

        MemberSanctionHistory history = historyCaptor.getValue();
        assertEquals(memberId, history.getMemberId(), "복구 대상 회원 ID가 일치해야 함");
        assertEquals(999L, history.getAdminId(), "시스템 관리자 ID가 일치해야 함");
        assertEquals(SanctionType.RESTORE, history.getSanctionType(), "제재 유형이 복구여야 함");
        assertEquals(MemberStatus.STOP, history.getBeforeStatus(), "변경 전 상태가 STOP이어야 함");
        assertEquals(MemberStatus.ACTIVE, history.getAfterStatus(), "변경 후 상태가 ACTIVE여야 함");
        assertTrue(history.getReason().contains("자동 제재 기간 만료"),
                "사유에 자동 제재 기간 만료가 포함되어야 함");
        assertNull(history.getSanctionUntil(), "복구 시에는 sanctionUntil이 null이어야 함");
    }

    @Test
    @DisplayName("자동 정지 해제 - 이미 활성화된 회원도 처리 가능")
    void restoreSuspension_alreadyActive() {
        // Given
        Long memberId = 1L;

        Member member = createMockMember(memberId, MemberStatus.ACTIVE);
        Member systemAdmin = createSystemAdmin();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByUserId("SYSTEM")).thenReturn(Optional.of(systemAdmin));

        // When
        sanctionService.restoreSuspension(memberId);

        // Then
        ArgumentCaptor<MemberSanctionHistory> historyCaptor = ArgumentCaptor.forClass(MemberSanctionHistory.class);
        verify(sanctionHistoryRepository).save(historyCaptor.capture());

        MemberSanctionHistory history = historyCaptor.getValue();
        assertEquals(MemberStatus.ACTIVE, history.getBeforeStatus(),
                "이미 활성 상태에서도 이력이 기록되어야 함");
        assertEquals(MemberStatus.ACTIVE, history.getAfterStatus(),
                "변경 후 상태도 ACTIVE여야 함");
    }

    @Test
    @DisplayName("자동 정지 해제 - 회원 없음 예외")
    void restoreSuspension_memberNotFound() {
        // Given
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sanctionService.restoreSuspension(memberId)
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode(),
                "회원을 찾을 수 없는 경우 MEMBER_NOT_FOUND 예외가 발생해야 함");

        // 복구 이력이 저장되지 않았는지 확인
        verify(sanctionHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("자동 정지 해제 - 시스템 관리자 없음 예외")
    void restoreSuspension_systemAdminNotFound() {
        // Given
        Long memberId = 1L;

        Member member = createMockMember(memberId, MemberStatus.STOP);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByUserId("SYSTEM")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                IllegalStateException.class,
                () -> sanctionService.restoreSuspension(memberId),
                "시스템 관리자를 찾을 수 없으면 IllegalStateException이 발생해야 함"
        );
    }

    // ========== 통합 시나리오 테스트 ==========

    @Test
    @DisplayName("통합 시나리오 - 정지 처리 후 해제")
    void integrationScenario_suspendAndRestore() {
        // Given
        Long memberId = 1L;
        String reason = "의심 활동";
        int days = 7;

        Member member = createMockMember(memberId, MemberStatus.ACTIVE);
        Member systemAdmin = createSystemAdmin();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByUserId("SYSTEM")).thenReturn(Optional.of(systemAdmin));

        // When - 정지
        sanctionService.applyAutoSuspension(memberId, reason, days);

        // Then - 정지 확인
        assertEquals(MemberStatus.STOP, member.getStatus(), "정지 후 상태가 STOP이어야 함");

        // When - 해제
        sanctionService.restoreSuspension(memberId);

        // Then - 해제 확인
        assertEquals(MemberStatus.ACTIVE, member.getStatus(), "해제 후 상태가 ACTIVE여야 함");

        // 총 2번의 이력 저장 확인 (정지 1번 + 해제 1번)
        verify(sanctionHistoryRepository, times(2)).save(any(MemberSanctionHistory.class));
    }
}
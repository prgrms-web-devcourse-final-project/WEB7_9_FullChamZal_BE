package back.fcz.domain.capsule.service;

import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.*;
import back.fcz.domain.member.dto.response.MemberDetailResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.member.service.MemberService;
import back.fcz.domain.sanction.constant.SanctionConstants;
import back.fcz.domain.sanction.properties.SanctionProperties;
import back.fcz.domain.sanction.service.MonitoringService;
import back.fcz.domain.unlock.dto.UnlockValidationResult;
import back.fcz.domain.unlock.service.FirstComeService;
import back.fcz.domain.unlock.service.UnlockService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.infra.storage.PresignedUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapsuleReadServiceTest {

    @Mock
    private CapsuleRepository capsuleRepository;

    @Mock
    private CapsuleRecipientRepository capsuleRecipientRepository;

    @Mock
    private CapsuleOpenLogRepository capsuleOpenLogRepository;

    @Mock
    private PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private CapsuleAttachmentRepository capsuleAttachmentRepository;

    @Mock
    private PhoneCrypto phoneCrypto;

    @Mock
    private UnlockService unlockService;

    @Mock
    private FirstComeService firstComeService;

    @Mock
    private MemberService memberService;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private MonitoringService monitoringService;

    @Mock
    private CapsuleOpenLogService capsuleOpenLogService;

    @Mock
    private SanctionConstants sanctionConstants;

    @Mock
    private SanctionProperties sanctionProperties;

    @Mock
    private PresignedUrlProvider presignedUrlProvider;

    private CapsuleReadService capsuleReadService;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = createMember(1L, "testUser", "테스터");

        lenient().when(sanctionConstants.getScoreByAnomaly(any(AnomalyType.class)))
                .thenAnswer(invocation -> {
                    AnomalyType type = invocation.getArgument(0);
                    return switch (type) {
                        case IMPOSSIBLE_MOVEMENT -> 50;
                        case TIME_MANIPULATION -> 30;
                        case RAPID_RETRY -> 20;
                        case LOCATION_RETRY -> 15;
                        case SUSPICIOUS_PATTERN -> 10;
                        case NONE -> 0;
                    };
                });

        capsuleReadService = new CapsuleReadService(
                capsuleRepository,
                capsuleRecipientRepository,
                phoneCrypto,
                unlockService,
                firstComeService,
                memberRepository,
                publicCapsuleRecipientRepository,
                capsuleOpenLogRepository,
                memberService,
                currentUserContext,
                bookmarkRepository,
                monitoringService,
                capsuleAttachmentRepository,
                presignedUrlProvider,
                capsuleOpenLogService,
                sanctionConstants
        );

        lenient().when(capsuleAttachmentRepository.findAllByCapsule_CapsuleIdAndStatus(
                anyLong(),
                any(CapsuleAttachmentStatus.class)
        )).thenReturn(Collections.emptyList());

        lenient().doNothing().when(capsuleOpenLogService).saveLogInNewTransaction(any(CapsuleOpenLog.class));
    }

    // ========== 헬퍼 메서드 ==========

    private Member createMember(Long memberId, String userId, String nickname) {
        Member member = Member.create(
                userId,
                "encodedPassword",
                "홍길동",
                nickname,
                "encryptedPhone",
                "phoneHash"
        );
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "status", MemberStatus.ACTIVE);
        ReflectionTestUtils.setField(member, "role", MemberRole.USER);
        return member;
    }

    private Capsule createPrivateCapsule(Long capsuleId, int isProtected, String password) {
        Capsule capsule = Capsule.builder()
                .memberId(testMember)
                .uuid("test-uuid-1234")
                .nickname("테스터")
                .content("테스트 캡슐 내용")
                .capsuleColor("BLUE")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .capPassword(password)
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().plusDays(1))
                .isProtected(isProtected)
                .build();
        ReflectionTestUtils.setField(capsule, "capsuleId", capsuleId);
        return capsule;
    }

    private Capsule createPublicCapsule(Long capsuleId) {
        Capsule capsule = Capsule.builder()
                .memberId(testMember)
                .uuid("test-uuid-public")
                .nickname("테스터")
                .content("공개 캡슐 내용")
                .capsuleColor("GREEN")
                .capsulePackingColor("YELLOW")
                .visibility("PUBLIC")
                .unlockType("LOCATION")
                .locationName("테스트 장소")
                .locationLat(37.5665)
                .locationLng(126.9780)
                .locationRadiusM(100)
                .build();
        ReflectionTestUtils.setField(capsule, "capsuleId", capsuleId);
        return capsule;
    }

    private CapsuleConditionRequestDTO createRequestDto(
            Long capsuleId,
            Double lat,
            Double lng,
            String password
    ) {
        return new CapsuleConditionRequestDTO(
                capsuleId,
                LocalDateTime.now(),  // unlockAt
                lat,                   // locationLat
                lng,                   // locationLng
                password,              // password
                "Mozilla/5.0",         // userAgent
                "192.168.1.1",         // ipAddress
                LocalDateTime.now()    // serverTime
        );
    }

    private void setupSecurityContext(Long memberId) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                memberId, null, null
        );
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ========== 공개 캡슐 테스트 ==========

    @Nested
    @DisplayName("공개 캡슐 조회")
    class PublicCapsuleTests {

        @Test
        @DisplayName("공개 캡슐 첫 조회 - 조건 충족 시 성공")
        void publicCapsule_firstAccess_success() {
            // Given
            setupSecurityContext(1L);
            Capsule publicCapsule = createPublicCapsule(1L);
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, 37.5665, 126.9780, null);

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(publicCapsule));
            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);
            when(capsuleOpenLogRepository.existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                    1L, 1L, CapsuleOpenStatus.SUCCESS
            )).thenReturn(false);

            UnlockValidationResult successResult = UnlockValidationResult.success();
            when(unlockService.validateTimeAndLocationConditions(
                    eq(publicCapsule),
                    any(LocalDateTime.class),
                    anyDouble(),
                    anyDouble(),
                    any(LocalDateTime.class),
                    eq(1L),
                    anyString()
            )).thenReturn(successResult);

            when(firstComeService.hasFirstComeLimit(publicCapsule)).thenReturn(false);

            doNothing().when(firstComeService).saveRecipientWithoutFirstCome(
                    eq(1L), eq(1L), any(CapsuleConditionRequestDTO.class)
            );

            when(bookmarkRepository.existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(anyLong(), eq(1L)))
                    .thenReturn(false);

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            verify(firstComeService, times(1)).saveRecipientWithoutFirstCome(
                    eq(1L), eq(1L), any(CapsuleConditionRequestDTO.class)
            );

            verify(capsuleOpenLogService, never()).saveLogInNewTransaction(any(CapsuleOpenLog.class));
        }

        @Test
        @DisplayName("공개 캡슐 첫 조회 - 조건 미충족 시 실패")
        void publicCapsule_firstAccess_conditionNotMet() {
            // Given
            setupSecurityContext(1L);
            Capsule publicCapsule = createPublicCapsule(1L);
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, 37.5665, 126.9780, null);

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(publicCapsule));
            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);
            when(capsuleOpenLogRepository.existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                    1L, 1L, CapsuleOpenStatus.SUCCESS
            )).thenReturn(false);

            UnlockValidationResult failResult = UnlockValidationResult.conditionFailed();
            when(unlockService.validateTimeAndLocationConditions(
                    eq(publicCapsule),
                    any(LocalDateTime.class),
                    anyDouble(),
                    anyDouble(),
                    any(LocalDateTime.class),
                    eq(1L),
                    anyString()
            )).thenReturn(failResult);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            ArgumentCaptor<CapsuleOpenLog> logCaptor = ArgumentCaptor.forClass(CapsuleOpenLog.class);
            verify(capsuleOpenLogService, times(1)).saveLogInNewTransaction(logCaptor.capture());

            CapsuleOpenLog savedLog = logCaptor.getValue();
            assertEquals(CapsuleOpenStatus.FAIL_LOCATION, savedLog.getStatus());
        }

        @Test
        @DisplayName("공개 캡슐 첫 조회 - 이상 감지 시 의심 점수 증가 및 예외 발생")
        void publicCapsule_firstAccess_anomalyDetected() {
            // Given
            setupSecurityContext(1L);
            Capsule publicCapsule = createPublicCapsule(1L);
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, 37.5665, 126.9780, null);

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(publicCapsule));
            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);
            when(capsuleOpenLogRepository.existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                    1L, 1L, CapsuleOpenStatus.SUCCESS
            )).thenReturn(false);

            UnlockValidationResult anomalyResult = UnlockValidationResult.anomalyDetected(
                    false,
                    AnomalyType.IMPOSSIBLE_MOVEMENT,
                    sanctionConstants.getScoreByAnomaly(AnomalyType.IMPOSSIBLE_MOVEMENT)
            );
            when(unlockService.validateTimeAndLocationConditions(
                    eq(publicCapsule),
                    any(LocalDateTime.class),
                    anyDouble(),
                    anyDouble(),
                    any(LocalDateTime.class),
                    eq(1L),
                    anyString()
            )).thenReturn(anomalyResult);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertEquals(ErrorCode.GPS_SPOOFING_SUSPECTED, exception.getErrorCode());

            verify(monitoringService, times(1)).incrementSuspicionScore(
                    eq(1L),
                    eq(50)
            );

            ArgumentCaptor<CapsuleOpenLog> logCaptor = ArgumentCaptor.forClass(CapsuleOpenLog.class);
            verify(capsuleOpenLogService, times(2)).saveLogInNewTransaction(logCaptor.capture());

            CapsuleOpenLog anomalyLog = logCaptor.getAllValues().get(1);
            assertEquals(AnomalyType.IMPOSSIBLE_MOVEMENT, anomalyLog.getAnomalyType());
        }

        @Test
        @DisplayName("공개 캡슐 - 비로그인 상태로 접근 시 UNAUTHORIZED 예외")
        void publicCapsule_notLoggedIn_throwsUnauthorized() {
            // Given
            clearSecurityContext();
            Capsule publicCapsule = createPublicCapsule(1L);
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, 37.5665, 126.9780, null);

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(publicCapsule));

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        }
    }

    // ========== 개인 캡슐 isProtected=1 테스트 ==========

    @Nested
    @DisplayName("개인 캡슐 - isProtected=1 (JWT 인증)")
    class ProtectedPrivateCapsuleTests {

        @Test
        @DisplayName("보호된 캡슐 첫 조회 - 수신자 본인, 조건 충족 시 성공")
        void protectedCapsule_firstAccess_validRecipient_success() {
            // Given
            setupSecurityContext(1L);
            Capsule protectedCapsule = createPrivateCapsule(1L, 1, null);
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, null, null, null);

            CapsuleRecipient recipient = CapsuleRecipient.builder()
                    .capsuleId(protectedCapsule)
                    .recipientName("홍길동")
                    .recipientPhone("encryptedPhone")
                    .recipientPhoneHash("phoneHash")
                    .isSenderSelf(0)
                    .build();

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(protectedCapsule));
            when(capsuleRecipientRepository.findByCapsuleId_CapsuleId(1L))
                    .thenReturn(Optional.of(recipient));

            InServerMemberResponse userResponse = new InServerMemberResponse(
                    1L, "testUser", "홍길동", "테스터",
                    "encryptedPhone", "phoneHash", MemberRole.USER
            );
            when(currentUserContext.getCurrentUser()).thenReturn(userResponse);

            MemberDetailResponse detailResponse = new MemberDetailResponse(
                    1L, "testUser", "홍길동", "테스터", "01012345678",
                    MemberStatus.ACTIVE, MemberRole.USER, null,
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(memberService.getDetailMe(userResponse)).thenReturn(detailResponse);

            when(phoneCrypto.verifyHash("01012345678", "phoneHash")).thenReturn(true);

            when(capsuleOpenLogRepository.existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                    1L, 1L, CapsuleOpenStatus.SUCCESS
            )).thenReturn(false);

            UnlockValidationResult successResult = UnlockValidationResult.success();
            when(unlockService.validateUnlockConditionsForPrivate(
                    eq(protectedCapsule),
                    any(LocalDateTime.class),
                    any(),
                    any(),
                    any(LocalDateTime.class),
                    eq(1L),
                    anyString()
            )).thenReturn(successResult);

            when(bookmarkRepository.existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(anyLong(), eq(1L)))
                    .thenReturn(false);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            verify(capsuleOpenLogService, times(1)).saveLogInNewTransaction(any(CapsuleOpenLog.class));
            verify(capsuleRecipientRepository, times(1)).save(recipient);
        }

        @Test
        @DisplayName("보호된 캡슐 - 수신자가 아닌 회원 접근 시 403 예외")
        void protectedCapsule_invalidRecipient_throwsForbidden() {
            // Given
            setupSecurityContext(1L);
            Capsule protectedCapsule = createPrivateCapsule(1L, 1, null);
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, null, null, null);

            CapsuleRecipient recipient = CapsuleRecipient.builder()
                    .capsuleId(protectedCapsule)
                    .recipientName("다른사람")
                    .recipientPhone("otherPhone")
                    .recipientPhoneHash("otherHash")
                    .isSenderSelf(0)
                    .build();

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(protectedCapsule));
            when(capsuleRecipientRepository.findByCapsuleId_CapsuleId(1L))
                    .thenReturn(Optional.of(recipient));

            InServerMemberResponse userResponse = new InServerMemberResponse(
                    1L, "testUser", "홍길동", "테스터",
                    "encryptedPhone", "phoneHash", MemberRole.USER
            );
            when(currentUserContext.getCurrentUser()).thenReturn(userResponse);

            MemberDetailResponse detailResponse = new MemberDetailResponse(
                    1L, "testUser", "홍길동", "테스터", "01012345678",
                    MemberStatus.ACTIVE, MemberRole.USER, null,
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(memberService.getDetailMe(userResponse)).thenReturn(detailResponse);

            when(phoneCrypto.verifyHash("01012345678", "otherHash")).thenReturn(false);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertEquals(ErrorCode.CAPSULE_NOT_RECEIVER, exception.getErrorCode());

            ArgumentCaptor<CapsuleOpenLog> logCaptor = ArgumentCaptor.forClass(CapsuleOpenLog.class);
            verify(capsuleOpenLogService, times(1)).saveLogInNewTransaction(logCaptor.capture());

            CapsuleOpenLog savedLog = logCaptor.getValue();
            assertEquals(CapsuleOpenStatus.FAIL_PERMISSION, savedLog.getStatus());
        }
    }

    // ========== 개인 캡슐 isProtected=0 테스트 ==========

    @Nested
    @DisplayName("개인 캡슐 - isProtected=0 (비밀번호 인증)")
    class UnprotectedPrivateCapsuleTests {

        @Test
        @DisplayName("비보호 캡슐 - 비밀번호 검증 성공, 회원 로그인 상태")
        void unprotectedCapsule_validPassword_loggedInMember() {
            // Given
            setupSecurityContext(1L);
            Capsule unprotectedCapsule = createPrivateCapsule(1L, 0, "hashedPassword");
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, null, null, "1234");

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(unprotectedCapsule));

            when(phoneCrypto.verifyHash("1234", "hashedPassword")).thenReturn(true);

            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);

            when(capsuleOpenLogRepository.existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                    1L, 1L, CapsuleOpenStatus.SUCCESS
            )).thenReturn(false);

            UnlockValidationResult successResult = UnlockValidationResult.success();
            when(unlockService.validateUnlockConditionsForPrivate(
                    eq(unprotectedCapsule),
                    any(LocalDateTime.class),
                    any(),
                    any(),
                    any(LocalDateTime.class),
                    eq(1L),
                    anyString()
            )).thenReturn(successResult);

            when(bookmarkRepository.existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(anyLong(), eq(1L)))
                    .thenReturn(false);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            ArgumentCaptor<CapsuleOpenLog> logCaptor = ArgumentCaptor.forClass(CapsuleOpenLog.class);
            verify(capsuleOpenLogService, times(1)).saveLogInNewTransaction(logCaptor.capture());

            CapsuleOpenLog savedLog = logCaptor.getValue();
            assertEquals("MEMBER", savedLog.getViewerType());
            assertEquals(CapsuleOpenStatus.SUCCESS, savedLog.getStatus());
        }

        @Test
        @DisplayName("비보호 캡슐 - 비밀번호 검증 성공, 비회원 상태")
        void unprotectedCapsule_validPassword_guest() {
            // Given
            clearSecurityContext();
            Capsule unprotectedCapsule = createPrivateCapsule(1L, 0, "hashedPassword");
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, null, null, "1234");

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(unprotectedCapsule));
            when(phoneCrypto.verifyHash("1234", "hashedPassword")).thenReturn(true);

            when(capsuleOpenLogRepository.existsByCapsuleId_CapsuleIdAndIpAddressAndStatus(
                    1L, "192.168.1.1", CapsuleOpenStatus.SUCCESS
            )).thenReturn(false);

            UnlockValidationResult successResult = UnlockValidationResult.success();
            when(unlockService.validateUnlockConditionsForPrivate(
                    eq(unprotectedCapsule),
                    any(LocalDateTime.class),
                    any(),
                    any(),
                    any(LocalDateTime.class),
                    isNull(),
                    anyString()
            )).thenReturn(successResult);

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            ArgumentCaptor<CapsuleOpenLog> logCaptor = ArgumentCaptor.forClass(CapsuleOpenLog.class);
            verify(capsuleOpenLogService, times(1)).saveLogInNewTransaction(logCaptor.capture());

            CapsuleOpenLog savedLog = logCaptor.getValue();
            assertEquals("GUEST", savedLog.getViewerType());
            assertEquals(CapsuleOpenStatus.SUCCESS, savedLog.getStatus());
            assertNull(savedLog.getMemberId());
        }

        @Test
        @DisplayName("비보호 캡슐 - 비밀번호 불일치 시 예외")
        void unprotectedCapsule_invalidPassword_throwsException() {
            // Given
            setupSecurityContext(1L);
            Capsule unprotectedCapsule = createPrivateCapsule(1L, 0, "hashedPassword");
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, null, null, "wrongPassword");

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(unprotectedCapsule));

            when(phoneCrypto.verifyHash("wrongPassword", "hashedPassword")).thenReturn(false);

            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertEquals(ErrorCode.CAPSULE_PASSWORD_NOT_MATCH, exception.getErrorCode());

            ArgumentCaptor<CapsuleOpenLog> logCaptor = ArgumentCaptor.forClass(CapsuleOpenLog.class);
            verify(capsuleOpenLogService, times(1)).saveLogInNewTransaction(logCaptor.capture());

            CapsuleOpenLog savedLog = logCaptor.getValue();
            assertEquals(CapsuleOpenStatus.FAIL_PASSWORD, savedLog.getStatus());
        }

        @Test
        @DisplayName("비보호 캡슐 - 비밀번호 미입력 시 예외")
        void unprotectedCapsule_noPassword_throwsException() {
            // Given
            setupSecurityContext(1L);
            Capsule unprotectedCapsule = createPrivateCapsule(1L, 0, "hashedPassword");
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, null, null, null);

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(unprotectedCapsule));

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertEquals(ErrorCode.CAPSULE_PASSWORD_REQUIRED, exception.getErrorCode());
        }

        @Test
        @DisplayName("비보호 캡슐 재조회 - 회원 IP 변경 시에도 성공")
        void unprotectedCapsule_reAccess_member_differentIp() {
            // Given
            setupSecurityContext(1L);
            Capsule unprotectedCapsule = createPrivateCapsule(1L, 0, "hashedPassword");
            CapsuleConditionRequestDTO requestDto = createRequestDto(1L, null, null, "1234");

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(unprotectedCapsule));
            when(phoneCrypto.verifyHash("1234", "hashedPassword")).thenReturn(true);
            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);

            when(capsuleOpenLogRepository.existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                    1L, 1L, CapsuleOpenStatus.SUCCESS
            )).thenReturn(true);

            when(bookmarkRepository.existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(anyLong(), eq(1L)))
                    .thenReturn(false);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            verify(unlockService, never()).validateUnlockConditionsForPrivate(
                    any(), any(), any(), any(), any(), any(), any()
            );
        }
    }

    // ========== 작성자 자신의 캡슐 조회 테스트 ==========

    @Nested
    @DisplayName("작성자 자신의 캡슐 조회")
    class SelfCapsuleReadTests {

        @Test
        @DisplayName("작성자가 자신의 공개 캡슐 조회 - 검증 없이 성공")
        void selfRead_publicCapsule_success() {
            // Given
            setupSecurityContext(1L);
            Capsule publicCapsule = createPublicCapsule(1L);

            when(capsuleRepository.findById(1L)).thenReturn(Optional.of(publicCapsule));
            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);

            when(publicCapsuleRecipientRepository
                    .existsByCapsuleId_CapsuleIdAndMemberId(1L, 1L))
                    .thenReturn(false);

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.capsuleRead(1L);

            // Then
            assertNotNull(result);

            verify(unlockService, never()).validateTimeAndLocationConditions(
                    any(), any(), any(), any(), any(), any(), any()
            );
        }

        @Test
        @DisplayName("작성자가 타인의 캡슐 조회 시도 - 예외 발생")
        void selfRead_othersCapsule_throwsException() {
            // Given
            setupSecurityContext(1L);
            Member otherMember = createMember(2L, "otherUser", "다른사람");
            Capsule othersCapsule = Capsule.builder()
                    .memberId(otherMember)
                    .uuid("other-uuid")
                    .nickname("다른사람")
                    .content("다른 사람의 캡슐")
                    .capsuleColor("BLUE")
                    .capsulePackingColor("RED")
                    .visibility("PRIVATE")
                    .unlockType("TIME")
                    .unlockAt(LocalDateTime.now().plusDays(1))
                    .isProtected(1)
                    .build();
            ReflectionTestUtils.setField(othersCapsule, "capsuleId", 2L);

            when(capsuleRepository.findById(2L)).thenReturn(Optional.of(othersCapsule));
            when(currentUserContext.getCurrentMemberId()).thenReturn(1L);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.capsuleRead(2L)
            );

            assertEquals(ErrorCode.NOT_SELF_CAPSULE, exception.getErrorCode());
        }
    }
}
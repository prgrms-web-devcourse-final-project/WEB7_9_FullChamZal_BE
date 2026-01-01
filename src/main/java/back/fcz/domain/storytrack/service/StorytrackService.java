package back.fcz.domain.storytrack.service;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.storytrack.dto.PathResponse;
import back.fcz.domain.storytrack.dto.StorytrackMemberType;
import back.fcz.domain.storytrack.dto.request.CreateStorytrackRequest;
import back.fcz.domain.storytrack.dto.request.JoinStorytrackRequest;
import back.fcz.domain.storytrack.dto.request.UpdatePathRequest;
import back.fcz.domain.storytrack.dto.response.*;
import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackProgress;
import back.fcz.domain.storytrack.entity.StorytrackStep;
import back.fcz.domain.storytrack.repository.StorytrackProgressRepository;
import back.fcz.domain.storytrack.repository.StorytrackRepository;
import back.fcz.domain.storytrack.repository.StorytrackStepRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StorytrackService {

    private final StorytrackRepository storytrackRepository;
    private final StorytrackProgressRepository storytrackProgressRepository;
    private final StorytrackStepRepository storytrackStepRepository;
    private final CapsuleRepository capsuleRepository;
    private final MemberRepository memberRepository;

    private final CapsuleReadService capsuleReadService;

    // 삭제
    // 생성자 : 스토리트랙 삭제
    @Transactional
    public DeleteStorytrackResponse deleteStorytrack(Long memberId, Long storytrackId) {

        // 삭제할 대상 스토리트랙 조회
        Storytrack targetStorytrack = storytrackRepository.findById(storytrackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_NOT_FOUND));

        // 요청한 사람과 스토리트랙 생성자가 동일한지 확인
        if (!Objects.equals(targetStorytrack.getMember().getMemberId(), memberId)) {
            throw new BusinessException(ErrorCode.NOT_STORYTRACK_CREATER);
        }

        // 스토리트랙 참여자가 존재하면 미 삭제
        if (storytrackProgressRepository.countActiveParticipants(storytrackId) > 0) {
            throw new BusinessException(ErrorCode.PARTICIPANT_EXISTS);
        }

        // 삭제 - 소프트딜리트
        targetStorytrack.setIsDeleted(1);
        targetStorytrack.markDeleted();

        // 스토리트랙 단계 삭제
        List<StorytrackStep> targetSteps = storytrackStepRepository.findAllByStorytrack_StorytrackId(storytrackId);

        for (StorytrackStep step : targetSteps) {
            step.markDeleted();
        }

        // 트랜잭션으로 인해 삭제 후 다시 DB 저장 문제 해결을 위해 삭제
        // storytrackRepository.save(targetStorytrack);
        return new DeleteStorytrackResponse(
                storytrackId,
                storytrackId + "번 스토리트랙이 삭제 되었습니다."
        );
    }

    // 참여자 : 참여자 삭제(참여 중지)
    @Transactional
    public DeleteParticipantResponse deleteParticipant(Long memberId, Long storytrackId) {

        // 스토리트랙 참여자 조회
        StorytrackProgress targetMember = storytrackProgressRepository.findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(storytrackId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        // 삭제 - 소프트딜리트
        targetMember.markDeleted();

        // 트랜잭션으로 인해 삭제 후 다시 DB 저장 문제 해결을 위해 삭제
        // storytrackProgressRepository.save(targetMember);

        return new DeleteParticipantResponse(
                "스토리트랙 참여를 종료했습니다."
        );
    }

    // 수정
    // 스토리트랙 경로 수정
    @Transactional
    public UpdatePathResponse updatePath(UpdatePathRequest request, Long loginMemberId) {
        // 스토리트랙 경로 조회
        StorytrackStep targetStep = (StorytrackStep) storytrackStepRepository.findByStorytrack_StorytrackIdAndStepOrder(request.storytrackId(), request.stepOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_PAHT_NOT_FOUND));

        // 요청한 사람과 스토리트랙 작성자가 같은지 확인
        if (!Objects.equals(targetStep.getStorytrack().getMember().getMemberId(), loginMemberId)) {
            throw new BusinessException(ErrorCode.NOT_STORYTRACK_CREATER);
        }

        // 새 캡슐
        Capsule updateCapsule = capsuleRepository.findById(request.updatedCapsuleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        // 경로 수정
        targetStep.setCapsule(updateCapsule);

        storytrackStepRepository.save(targetStep);

        return UpdatePathResponse.from(updateCapsule, targetStep);
    }

    // 생성
    // 스토리 트랙 생성
    @Transactional
    public CreateStorytrackResponse createStorytrack(CreateStorytrackRequest request, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 스토리트랙 생성
        Storytrack storytrack = Storytrack.builder()
                .member(member)
                .title(request.title())
                .description(request.description())
                .trackType(request.trackType())
                .isPublic(request.isPublic())
                .price(request.price())
                .totalSteps(request.capsuleList().size())
                .isDeleted(0)
                .build();

        storytrackRepository.save(storytrack);

        int stepOrder = 1;

        // 스토리트랙 스탭 생성
        for (Long capsuleId : request.capsuleList()) {

            Capsule capsule = capsuleRepository.findById(capsuleId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

            // 스토리트랙 스탭 캡슐이 비공개 상태 캡슐일 때
            if (capsule.getVisibility().equals("PRIVATE")
                    || capsule.getVisibility().equals("SELF")) {
                throw new BusinessException(ErrorCode.CAPSULE_NOT_PUBLIC);
            }

            StorytrackStep step = StorytrackStep.builder()
                    .capsule(capsule)
                    .stepOrder(stepOrder++)
                    .build();

            storytrack.addStep(step);
        }

        return CreateStorytrackResponse.from(storytrack);
    }


    // 스토리트랙 참여 회원 생성
    @Transactional
    public JoinStorytrackResponse joinParticipant(JoinStorytrackRequest request, Long memberId) {
        // 멤버 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        // 스토리트랙 존재 확인
        Storytrack storytrack = storytrackRepository.findById(request.storytrackId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_NOT_FOUND));

        // 참여자 확인
        if(Objects.equals(storytrack.getMember().getMemberId(), memberId)){
            throw new BusinessException(ErrorCode.STORYTRACK_CREATOR_NOT_JOIN);
        }

        // 스토리트랙 상태 확인
        if (storytrack.getIsPublic() == 0) {
            throw new BusinessException(ErrorCode.STORYTRACK_NOT_PUBLIC);
        }

        // 스토리트랙 참여자 존재 확인 -> 존재하면 이미 존재 중이라고 예외 처리
        if(storytrackProgressRepository.existsByMember_MemberIdAndStorytrack_StorytrackIdAndDeletedAt(memberId, request.storytrackId(), null)){
            throw new BusinessException(ErrorCode.PARTICIPANT_ALREADY_JOIN);
        }

        // 참여자 생성
        StorytrackProgress participant = StorytrackProgress.builder()
                .member(member)
                .storytrack(storytrack)
                .completedSteps(0)
                .lastCompletedStep(0)
                .build();

        storytrackProgressRepository.save(participant);

        return JoinStorytrackResponse.from(storytrack, participant);
    }

    private Pageable createPageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50); // max 50

        return PageRequest.of(safePage, safeSize, sort);
    }

    // 조회
    // 전체 스토리 트랙 목록 조회 -> 삭제된 스토리트랙(isDelete = 1)은 조회에서 제외
    public PageResponse<TotalStorytrackResponse> readTotalStorytrack(Long memberId, int page, int size) {

        Pageable pageable = createPageable(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "storytrackId") // 생성한 순서대로 조회
        );

        Page<TotalStorytrackResponse> responsePage =
                storytrackRepository.findPublicStorytracksWithMemberType(memberId, pageable);

        return new PageResponse<>(responsePage);
    }

    // 스토리트랙 조회 -> 스토리트랙에 대한 간략한 조회 : 삭제된 스토리트랙은 미조회
    // 대략적인 경로(순서), 총 인원 수, 완료한 인원 수, 스토리트랙 제작자(닉네임)
    public StorytrackDashBoardResponse storytrackDashboard(
            Long memberId,
            Long storytrackId,
            int page,
            int size
    ) {
        // 스토리트랙
        Storytrack storytrack = storytrackRepository
                .findByStorytrackIdAndIsDeleted(storytrackId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_NOT_FOUND));

        int totalParticipant = storytrackProgressRepository.countByStorytrack_StorytrackId(storytrackId);
        int completeProgress = storytrackProgressRepository.countByStorytrack_StorytrackIdAndCompletedAtIsNotNull(storytrackId);

        // 로그인한 사용자가 해당 대시보드와 어떤 관계인지 표시
        StorytrackMemberType memberType = resolveMemberType(memberId, storytrack);

        // 스토리트랙 경로
        Pageable pageable = createPageable(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "stepOrder")
        );

        Page<StorytrackStep> paths = storytrackStepRepository.findStepsWithCapsule(storytrackId, pageable);

        Page<PathResponse> responsePage =
                paths.map(PathResponse::from);

        List<Long> completedCapsuleIds =
                storytrackStepRepository.findCompletedCapsuleIds(
                        storytrackId,
                        memberId
                );

        return StorytrackDashBoardResponse.of(
                storytrack,
                responsePage,
                totalParticipant,
                completeProgress,
                memberType,
                completedCapsuleIds
        );
    }

    private StorytrackMemberType resolveMemberType(
            Long memberId,
            Storytrack storytrack
    ) {
        // 스토리트랙 생성자
        if (storytrack.getMember().getMemberId().equals(memberId)) {
            return StorytrackMemberType.CREATOR;
        }

        // 현재 참여 상태 조회 (soft delete 제외됨)
        Optional<StorytrackProgress> progressOpt =
                storytrackProgressRepository
                        .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(
                                storytrack.getStorytrackId(),
                                memberId
                        );

        // 참여 이력 없음
        if (progressOpt.isEmpty()) {
            return StorytrackMemberType.NOT_JOINED;
        }

        StorytrackProgress progress = progressOpt.get();

        // 완료 여부
        if (progress.getCompletedAt() != null) {
            return StorytrackMemberType.COMPLETED;
        }

        // 참여 중
        return StorytrackMemberType.PARTICIPANT;
    }


    // 생성, 참여 : 스토리트랙 경로 조회 -> 삭제된 스토리트랙 미조회
    // 단계, 각 단계의 캡슐 조회
    public StorytrackPathResponse storytrackPath(
            Long storytrackId,
            int page,
            int size
    ) {

        Storytrack storytrack = storytrackRepository
                .findByStorytrackIdAndIsDeleted(storytrackId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_NOT_FOUND));

        Pageable pageable = createPageable(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "stepOrder")
        );

        Page<StorytrackStep> steps =
                storytrackStepRepository.findStepsWithCapsule(storytrackId, pageable);

        Page<PathResponse> responsePage =
                steps.map(PathResponse::from);

        return new StorytrackPathResponse(
                storytrack.getStorytrackId(),
                storytrack.getTitle(),
                storytrack.getDescription(),
                storytrack.getTotalSteps(),
                new PageResponse<> (responsePage)
        );
    }


    // 생성자 : 생성한 스토리트랙 목록 조회 -> 삭제된 스토리트랙 미조회
    public PageResponse<CreaterStorytrackListResponse> createdStorytrackList(
            Long memberId,
            int page,
            int size
    ) {

        Pageable pageable = createPageable(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "storytrackId") // 생성한 순서대로 조회
        );

        Page<CreaterStorytrackListResponse> responsePage =
                storytrackRepository.findCreatedStorytracksWithMemberCount(
                        memberId,
                        pageable
                );

        return new PageResponse<> (responsePage);
    }

    // 참여자 : 참여한 스토리트랙 목록 조회 -> 삭제된 스토리트랙 목록 미조회 추가
    public PageResponse<ParticipantStorytrackListResponse> joinedStorytrackList(
            Long memberId,
            int page,
            int size
    ) {

        Pageable pageable = createPageable(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id") // 참여한 순서대로 조회
        );

        Page<ParticipantStorytrackListResponse> responsePage =
                storytrackProgressRepository
                        .findJoinedStorytracksWithMemberCount(memberId, pageable);

        return new PageResponse<> (responsePage);
    }

    // 참여자 : 스토리트랙 진행 상세 조회 -> 삭제된 스토리트랙 미조회 추가
    public ParticipantProgressResponse storytrackProgress(Long storytrackId, Long memberId) {

        StorytrackProgress progress =
                storytrackProgressRepository
                        .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(storytrackId, memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        return ParticipantProgressResponse.from(progress);
    }

    // 스토리트랙 캡슐 열람 전 참여자 검증
    public void validateParticipant(Long memberId, Long storytrackId) {

        boolean exists = storytrackProgressRepository
                .existsByMember_MemberIdAndStorytrack_StorytrackId(memberId, storytrackId);

        if (!exists) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
    }

    // 스토리트랙 단계 검증
    public void validateStepAccess(
            Long memberId,
            Long storytrackId,
            Long capsuleId
    ) {
        // 진행 정보 조회
        StorytrackProgress progress = storytrackProgressRepository
                .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(storytrackId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        // FREE 타입이면 그냥 바로 넘어가기
        if ("FREE".equals(progress.getStorytrack().getTrackType())) {
            return;
        }

        // 캡슐이 속한 스텝 조회
        StorytrackStep step = storytrackStepRepository
                .findByCapsule_CapsuleIdAndStorytrack_StorytrackId(capsuleId, storytrackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STEP_NOT_FOUND));

        // SEQUENTIAL 검증
        int expectedStepOrder = progress.getLastCompletedStep() + 1;

        if (step.getStepOrder() != expectedStepOrder) {
            throw new BusinessException(ErrorCode.INVALID_STEP_ORDER);
        }
    }

    @Transactional
    public CapsuleConditionResponseDTO openCapsuleAndUpdateProgress(
            Long memberId,
            Long storytrackId,
            CapsuleConditionRequestDTO request
    ) {
        // 참여자 진행 정보 조회
        StorytrackProgress progress =
                storytrackProgressRepository
                        .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(storytrackId, memberId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND)
                        );

        // 현재 캡슐이 속한 step 조회
        StorytrackStep step =
                storytrackStepRepository
                        .findByCapsule_CapsuleIdAndStorytrack_StorytrackId(
                                request.capsuleId(), storytrackId
                        )
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.STEP_NOT_FOUND)
                        );

        // 캡슐 조건 검증 + 오픈
        CapsuleConditionResponseDTO response =
                capsuleReadService.conditionAndRead(request);

        if (!"SUCCESS".equals(response.result())) {
            return response;
        }

        // 진행 상태 업데이트
        progress.completeStep(
                step.getStepOrder(),
                progress.getStorytrack().getTotalSteps()
        );

        return response;
    }
}
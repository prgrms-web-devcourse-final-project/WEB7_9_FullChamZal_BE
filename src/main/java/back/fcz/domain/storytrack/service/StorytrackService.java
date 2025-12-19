package back.fcz.domain.storytrack.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
// import back.fcz.domain.storytrack.dto.response.StorytrackDashBoardResponse;
import back.fcz.domain.storytrack.dto.PathResponse;
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
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorytrackService {

    private final StorytrackRepository storytrackRepository;
    private final StorytrackProgressRepository storytrackProgressRepository;
    private final StorytrackStepRepository storytrackStepRepository;
    private final CapsuleRepository capsuleRepository;
    private final MemberRepository memberRepository;

    // 삭제
    // 생성자 : 스토리트랙 삭제
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
        targetStorytrack.markDeleted();

        // 스토리트랙 단계 삭제
        List<StorytrackStep> targetSteps = storytrackStepRepository.findAllByStorytrack_StorytrackId(storytrackId);

        for (StorytrackStep step : targetSteps) {
            step.markDeleted();
        }

        storytrackRepository.save(targetStorytrack);
        return new DeleteStorytrackResponse(
                storytrackId,
                storytrackId + "번 스토리트랙이 삭제 되었습니다."
        );
    }

    // 참여자 : 참여자 삭제(참여 중지)
    public DeleteParticipantResponse deleteParticipant(Long memberId, Long storytrackId) {

        // 스토리트랙 참여자 조회
        StorytrackProgress targetMember = storytrackProgressRepository.findByMember_MemberIdAndStorytrack_StorytrackId(memberId, storytrackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        // 삭제 - 소프트딜리트
        targetMember.markDeleted();

        storytrackProgressRepository.save(targetMember);

        return new DeleteParticipantResponse(
                "스토리트랙 참여를 종료했습니다."
        );
    }

    // 수정
    // 스토리트랙 경로 수정
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
                .isDeleted(0)
                .build();

        storytrack.setTotalSteps(storytrack.getSteps().size());
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
    public JoinStorytrackResponse joinParticipant(JoinStorytrackRequest request, Long memberId) {
        // 멤버 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        // 스토리트랙 존재 확인
        Storytrack storytrack = storytrackRepository.findById(request.storytrackId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_NOT_FOUND));

        // 스토리트랙 상태 확인
        if (storytrack.getIsPublic() == 0) {
            throw new BusinessException(ErrorCode.STORYTRACK_NOT_PUBLIC);
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

    // 조회
    // 전체 스토리 트랙 목록 조회
    public Page<TotalStorytrackResponse> readTotalStorytrack(int page, int size) {

        Pageable pageable = PageRequest.of(
                page,
                size
        );

        Page<Storytrack> storytracks = storytrackRepository.findByIsPublic(1, pageable);

        return storytracks.map(TotalStorytrackResponse::from);
    }

    // 스토리트랙 조회 -> 스토리트랙에 대한 간략한 조회
    // 대략적인 경로(순서), 총 인원 수, 완료한 인원 수, 스토리트랙 제작자(닉네임)
    public StorytrackDashBoardResponse storytrackDashboard(
            Long storytrackId,
            Pageable pageable
    ) {
        Storytrack storytrack = storytrackRepository
                .findByStorytrackIdAndIsDeleted(storytrackId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_NOT_FOUND));

        int totalParticipant = storytrackProgressRepository.countByStorytrack_StorytrackId(storytrackId);
        int completeProgress = storytrackProgressRepository.countByStorytrack_StorytrackIdAndCompletedAtIsNotNull(storytrackId);

        // 스토리트랙 경로
        Page<StorytrackStep> stepPage =
                storytrackStepRepository.findStepsWithCapsule(storytrackId, pageable);

        Page<PathResponse> pathPage =
                stepPage.map(PathResponse::from);

        return StorytrackDashBoardResponse.of(
                storytrack,
                pathPage,
                totalParticipant,
                completeProgress
        );
    }


    // 생성, 참여 : 스토리트랙 경로 조회
    // 단계, 각 단계의 캡슐 조회
//    public storytrackPathResponse storytrackPath(){
//
//    }

    // 생성자 : 생성한 스토리트랙 목록 조회
//    public createrStorytrackListResponse createdStorytrackList(){
//
//    }

    // 생성자 : 스토리트랙 참여자 목록 조회

    // 참여자 : 참여 가능한 스토리트랙 목록 조회

    // 참여자 : 참여한 스토리트랙 목록 조회
//    public participantStorytrackListResponse joinStorytrackList(){
//
//    }

    // 참여자 : 스토리트랙 진행 상세 조회
//    public participantProgressResponse progressRead(){
//
//    }

    // 스토리트랙 캡슐 상세 조회
    /* public CapsuleDTO readCapsuelStorytrack(){
       // 생성자인지 참여자인지 확인

       // 참여자면 캡슐 조건 확인
         // 스토리트랙 타입 확인
            // 스토리트랙의 단계에 맞나? -> 조건 미충족 시 예외
       // 캡슐 조건이 맞나 -> 조건 미충족 시 예외
    */
}
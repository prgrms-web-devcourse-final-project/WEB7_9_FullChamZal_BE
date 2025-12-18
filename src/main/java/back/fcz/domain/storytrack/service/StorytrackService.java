package back.fcz.domain.storytrack.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.storytrack.dto.request.UpdatePathRequest;
import back.fcz.domain.storytrack.dto.response.DeleteParticipantResponse;
import back.fcz.domain.storytrack.dto.response.DeleteStorytrackResponse;
import back.fcz.domain.storytrack.dto.response.UpdatePathResponse;
import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackProgress;
import back.fcz.domain.storytrack.entity.StorytrackStep;
import back.fcz.domain.storytrack.repository.StorytrackProgressRepository;
import back.fcz.domain.storytrack.repository.StorytrackRepository;
import back.fcz.domain.storytrack.repository.StorytrackStepRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StorytrackService {

    private final StorytrackRepository storytrackRepository;
    private final StorytrackProgressRepository storytrackProgressRepository;
    private final StorytrackStepRepository storytrackStepRepository;
    private final CapsuleRepository capsuleRepository;

    // 삭제
    // 생성자 : 스토리트랙 삭제
    public DeleteStorytrackResponse deleteStorytrack(Long memberId, Long storytrackId){

        // 삭제할 대상 스토리트랙 조회
        Storytrack targetStorytrack = storytrackRepository.findById(storytrackId)
                .orElseThrow(()-> new BusinessException(ErrorCode.STORYTRACK_NOT_FOUND));

        // 요청한 사람과 스토리트랙 생성자가 동일한지 확인
        if(!Objects.equals(targetStorytrack.getMember().getMemberId(), memberId)){
            throw new BusinessException(ErrorCode.NOT_STORYTRACK_CREATER);
        }

        // 스토리트랙 참여자가 존재하면 미 삭제
        if(storytrackProgressRepository.countActiveParticipants(storytrackId) > 0){
            throw new BusinessException(ErrorCode.PARTICIPANT_EXISTS);
        }

        // 삭제 - 소프트딜리트
        targetStorytrack.markDeleted();

        // 스토리트랙 단계 삭제
        List<StorytrackStep> targetSteps = storytrackStepRepository.findAllByStorytrack_StorytrackId(storytrackId);

        for(StorytrackStep step : targetSteps){
            step.markDeleted();
        }

        storytrackRepository.save(targetStorytrack);
        return new DeleteStorytrackResponse(
                storytrackId,
                storytrackId + "번 스토리트랙이 삭제 되었습니다."
        );
    }

    // 참여자 : 참여자 삭제(참여 중지)
    public DeleteParticipantResponse deleteParticipant(Long memberId, Long storytrackId){

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
    public UpdatePathResponse updatePath (UpdatePathRequest request, Long storytrackStepId, Long loginMemberId){
        // 스토리트랙 경로 조회
        StorytrackStep targetStep = storytrackStepRepository.findById(storytrackStepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_PAHT_NOT_FOUND));

        // 요청한 사람과 스토리트랙 작성자가 같은지 확인
        if(!Objects.equals(targetStep.getStorytrack().getMember().getMemberId(), loginMemberId)){
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
//    public createStorytrackResponse createStorytrack(){
//
//    }

    // 스토리트랙 참여 회원 생성
//    public joinStorytrackResponse joinParticipant(){
//
//    }

    // 조회
    // 전체 스토리 트랙 조회
//    public totalStorytrackResponse readTotalStorytrack(){
//
//    }

    // 스토리트랙 상세 조회
//    public storytrackDashboardResponse storytrackDashBoard(){
//
//    }

    // 스토리트랙 경로 조회
//    public storytrackPathResponse storytrackPath(){
//
//    }

    // 생성자 : 생성한 스토리트랙 목록 조회
//    public createrStorytrackListResponse createdStorytrackList(){
//
//    }

    // 참여자 : 참여한 스토리트랙 목록 조회
//    public participantStorytrackListResponse joinStorytrackList(){
//
//    }

    // 참여자 : 스토리트랙 진행 상세 조회
//    public participantProgressResponse progressRead(){
//
//    }

    // 스토리트랙 캡슐 상세 조회
    // 참여자인지 생성자인지 검증 로직 필요
    // CapsuleReadService의 publicCapsuleLogic() 사용 -> 공개 캡슐 조회
}

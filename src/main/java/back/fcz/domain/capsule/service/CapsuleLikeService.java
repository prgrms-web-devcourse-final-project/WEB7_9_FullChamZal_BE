package back.fcz.domain.capsule.service;


import back.fcz.domain.capsule.DTO.response.CapsuleLikeResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleLike;
import back.fcz.domain.capsule.repository.CapsuleLikeRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleLikeService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleLikeRepository capsuleLikeRepository;
    private final MemberRepository memberRepository;
    private final CurrentUserContext currentUserContext;

    public CapsuleLikeResponse likeUp(Long capsuleId) {
        Long memberId = currentUserContext.getCurrentMemberId();
        Capsule capsule = capsuleRepository.findById(capsuleId).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        if(!capsule.getVisibility().equals("PUBLIC")){
            throw new BusinessException(ErrorCode.NOT_PUBLIC);
        }

        //중복된 요청인지 체크
        //같은 사용자가 같은 캡슐에 좋아요를 계속 누르는 것을 방지
        if(capsuleLikeRepository.existsByCapsuleIdMemberId(capsuleId, memberId)){
            throw new BusinessException(ErrorCode.DUPLICATE_LIKE_REQUEST);
        }

        //자신의 캡슐에 자신이 좋아요 누르는 것을 방지
        if(capsule.getMemberId().getMemberId().equals(memberId)){
            throw new BusinessException(ErrorCode.SELF_LIKE_NOT_ALLOWED);
        }

        //좋아요 객체 생성
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        CapsuleLike capsuleLike = CapsuleLike.builder()
                .capsuleId(capsule)
                .memberId(member)
                .build();
        capsuleLikeRepository.save(capsuleLike);
        //해당 캡슐의 좋아요 값 +1
        capsuleRepository.incrementLikeCount(capsuleId);
        capsule = capsuleRepository.findById(capsuleId).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        return CapsuleLikeResponse.from(capsule.getLikeCount(), "좋아요 증가처리 성공");
    }


    public CapsuleLikeResponse likeDown(Long capsuleId) {
        Capsule capsule = capsuleRepository.findById(capsuleId).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        //공개캡슐만 좋아요기능이 가능
        if(!capsule.getVisibility().equals("PUBLIC")){
            throw new BusinessException(ErrorCode.NOT_PUBLIC);
        }

        Long memberId = currentUserContext.getCurrentMemberId();
        //좋아요를 누른적이 없는데 좋아요 감소 요청을 하는 경우
        if(!capsuleLikeRepository.existsByCapsuleIdMemberId(capsuleId, memberId)){
            throw new BusinessException(ErrorCode.LIKE_DECREASED_FAIL);
        }

        //좋아요 객체 삭제
        capsuleLikeRepository.deleteByCapsuleId_CapsuleIdAndMemberId_MemberId(capsuleId, memberId);

        //해당 캡슐의 좋아요 값 -1
        capsuleRepository.decrementLikeCount(capsuleId);

        capsule = capsuleRepository.findById(capsuleId).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        return CapsuleLikeResponse.from(capsule.getLikeCount(), "좋아요 감소처리 성공");
    }

    public CapsuleLikeResponse readLike(Long capsuleId) {
        Capsule capsule = capsuleRepository.findById(capsuleId).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        return CapsuleLikeResponse.from(capsule.getLikeCount(), "좋아요 수 읽기 성공");
    }
}

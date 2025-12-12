package back.fcz.domain.admin.member.service;

import back.fcz.domain.admin.member.dto.*;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    /**
     * 관리자용 회원 목록 조회
     * - 일단은 아주 단순히 status/keyword 정도만 필터링하는 구조로 시작
     * - 나중에 QueryDSL 등으로 리팩토링 가능
     */
    public PageResponse<AdminMemberSummaryResponse> searchMembers(AdminMemberSearchRequest cond) {

        Pageable pageable = PageRequest.of(
                cond.getPage(),
                cond.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // TODO: 추후 status, keyword, from~to 조건을 사용해서 고도화
        Page<Member> page = memberRepository.findAll(pageable);

        Page<AdminMemberSummaryResponse> mapped = page.map(member ->
                AdminMemberSummaryResponse.of(
                        member,
                        0L, // TODO: report count
                        0L, // TODO: blocked capsule count
                        0L  // TODO: capsule count
                )
        );

        return new PageResponse<>(mapped);
    }
    // 1-2 회원 상세 조회
    public AdminMemberDetailResponse getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));
        // ↑ ErrorCode에 ADMIN_MEMBER_NOT_FOUND 없으면 하나 추가 필요 (아래 참고)

        // TODO: 여기에서 캡슐/신고/북마크/스토리트랙/알림/전화번호 인증 로그 등을
        // repository / QueryDSL 로 조회해서 DetailResponse에 채워넣을 예정.
        // 지금은 기본 member 정보만 내려주는 버전.
        return AdminMemberDetailResponse.basicOf(member);
    }

    /**
     * 1-3 회원 상태 변경
     */
    @Transactional
    public AdminMemberStatusUpdateResponse updateMemberStatus(
            Long memberId,
            AdminMemberStatusUpdateRequest request
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));

        // TODO: 자기 자신 계정 상태 변경 금지 규칙을 넣고 싶으면 여기서 처리
        // ex) SecurityContext에서 현재 admin id 꺼내서 비교

        // TODO: EXIT -> ACTIVE 같은 전이 막고 싶으면 여기서 체크 후
        // new BusinessException(ErrorCode.ADMIN_MEMBER_STATUS_CHANGE_NOT_ALLOWED) 던지기
        member.changeStatus(request.getStatus());

        // JPA 영속 상태이기 때문에 별도 save() 없이 트랜잭션 종료 시 자동 flush

        return AdminMemberStatusUpdateResponse.of(
                member,
                request.getReason(),
                request.getSanctionUntil()
        );
    }
}

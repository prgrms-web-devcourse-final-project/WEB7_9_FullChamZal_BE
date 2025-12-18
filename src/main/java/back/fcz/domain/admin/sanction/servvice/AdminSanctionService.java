package back.fcz.domain.admin.sanction.service;

import back.fcz.domain.admin.sanction.dto.AdminSanctionSummaryResponse;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.repository.MemberSanctionHistoryRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSanctionService {

    private final MemberRepository memberRepository;
    private final MemberSanctionHistoryRepository sanctionHistoryRepository;

    public PageResponse<AdminSanctionSummaryResponse> getMemberSanctions(Long memberId, int page, int size) {

        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size <= 0 ? 20 : size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<MemberSanctionHistory> historyPage = sanctionHistoryRepository.findByMemberId(memberId, pageable);
        Page<AdminSanctionSummaryResponse> mapped = historyPage.map(AdminSanctionSummaryResponse::from);

        return new PageResponse<>(mapped);
    }
}

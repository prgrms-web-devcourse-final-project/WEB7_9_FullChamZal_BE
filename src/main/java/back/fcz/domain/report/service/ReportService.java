package back.fcz.domain.report.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.report.dto.request.ReportRequest;
import back.fcz.domain.report.entity.Report;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final CurrentUserContext currentUserContext;
    private final MemberRepository memberRepository;
    private final CapsuleRepository capsuleRepository;
    private final ReportRepository reportRepository;

    public Long createReport(ReportRequest reportRequest) {
        Member reporter = null;

        if (currentUserContext.isAuthenticated()) {
            Long memberId = currentUserContext.getCurrentMemberId();

            reporter = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        }

        Capsule capsule = capsuleRepository.findByCapsuleId(reportRequest.capsuleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        Report report = Report.builder()
                .capsule(capsule)
                .reporter(reporter)
                .reasonType(reportRequest.reasonType())
                .reasonDetail(reportRequest.reasonDetail())
                .build();

        Report savedReport = reportRepository.save(report);

        return savedReport.getId();
    }
}

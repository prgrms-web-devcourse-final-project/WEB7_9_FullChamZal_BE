package back.fcz.domain.admin.report.service;

import back.fcz.domain.admin.report.dto.AdminReportStatusUpdateRequest;
import back.fcz.domain.admin.report.dto.AdminReportStatusUpdateResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.report.entity.Report;
import back.fcz.domain.report.entity.ReportReasonType;
import back.fcz.domain.report.entity.ReportStatus;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.repository.MemberSanctionHistoryRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock
    ReportRepository reportRepository;

    @Mock
    MemberSanctionHistoryRepository memberSanctionHistoryRepository;

    @Mock
    CurrentUserContext currentUserContext;

    AdminReportService service;



    @BeforeEach
    void setUp() {
        service = new AdminReportService(reportRepository, memberSanctionHistoryRepository, currentUserContext);
    }

    @Test
    void 이미_DONE인_신고면_ADMIN_REPORT_ALREADY_DONE() {
        Report report = Report.builder()
                .capsule(dummyCapsule())
                .reasonType(ReportReasonType.SPAM)
                .reasonDetail("x")
                .status(ReportStatus.ACCEPTED)
                .build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(currentUserContext.getCurrentMemberId()).thenReturn(10L);

        AdminReportStatusUpdateRequest req = new AdminReportStatusUpdateRequest();
        ReflectionTestUtils.setField(req, "status", "REJECTED");
        ReflectionTestUtils.setField(req, "action", "NONE");

        BusinessException ex = catchThrowableOfType(() -> service.updateReportStatus(1L, req), BusinessException.class);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ADMIN_REPORT_ALREADY_DONE);
    }

    @Test
    void REVIEWING으로_변경은_PENDING일때만_가능() {
        Report report = Report.builder()
                .capsule(dummyCapsule())
                .reasonType(ReportReasonType.SPAM)
                .reasonDetail("x")
                .status(ReportStatus.PENDING)
                .build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(currentUserContext.getCurrentMemberId()).thenReturn(99L);

        AdminReportStatusUpdateRequest req = new AdminReportStatusUpdateRequest();
        ReflectionTestUtils.setField(req, "status", "REVIEWING");
        ReflectionTestUtils.setField(req, "action", "NONE");

        AdminReportStatusUpdateResponse res = service.updateReportStatus(1L, req);

        assertThat(res.getStatus()).isEqualTo("REVIEWING");
        assertThat(report.getProcessedBy()).isEqualTo(99L);
        verify(memberSanctionHistoryRepository, never()).save(any());
    }

    @Test
    void ACCEPTED_그리고_HIDE_CAPSULE이면_캡슐_보호처리() {
        Capsule capsule = dummyCapsule();
        Report report = Report.builder()
                .capsule(capsule)
                .reasonType(ReportReasonType.HATE)
                .reasonDetail("bad")
                .status(ReportStatus.REVIEWING)
                .build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(currentUserContext.getCurrentMemberId()).thenReturn(5L);

        AdminReportStatusUpdateRequest req = new AdminReportStatusUpdateRequest();
        ReflectionTestUtils.setField(req, "status", "ACCEPTED");
        ReflectionTestUtils.setField(req, "action", "HIDE_CAPSULE");
        ReflectionTestUtils.setField(req, "processMemo", "hide it");

        AdminReportStatusUpdateResponse res = service.updateReportStatus(1L, req);

        assertThat(res.getStatus()).isEqualTo("ACCEPTED");
        assertThat(capsule.getIsProtected()).isEqualTo(1);
        assertThat(report.getProcessedAt()).isNotNull();
        assertThat(report.getProcessedBy()).isEqualTo(5L);
    }

    @Test
    void ACCEPTED_그리고_SUSPEND_MEMBER이면_작성자_STOP_전환_및_제재로그저장() {
        Member writer = Member.testMember(3L, "u", "writer");
        // 캡슐에 작성자 연결
        Capsule capsule = Capsule.builder()
                .capsuleId(10L)
                .memberId(writer)
                .uuid("uuid")
                .nickname("writerNick")
                .receiverNickname(null)
                .title("t")
                .content("c")
                .capsuleColor("a")
                .capsulePackingColor("b")
                .visibility("PUBLIC")
                .unlockType("TIME")
                .locationRadiusM(0)
                .maxViewCount(0)
                .currentViewCount(0)
                .isDeleted(0)
                .isProtected(0)
                .build();

        Report report = Report.builder()
                .capsule(capsule)
                .reasonType(ReportReasonType.ETC)
                .reasonDetail("x")
                .status(ReportStatus.REVIEWING)
                .build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(currentUserContext.getCurrentMemberId()).thenReturn(7L);

        AdminReportStatusUpdateRequest req = new AdminReportStatusUpdateRequest();
        ReflectionTestUtils.setField(req, "status", "ACCEPTED");
        ReflectionTestUtils.setField(req, "action", "SUSPEND_MEMBER");
        ReflectionTestUtils.setField(req, "processMemo", "stop");

        service.updateReportStatus(1L, req);

        assertThat(writer.getStatus()).isEqualTo(MemberStatus.STOP);

        ArgumentCaptor<MemberSanctionHistory> captor =
                ArgumentCaptor.forClass(MemberSanctionHistory.class);
        verify(memberSanctionHistoryRepository).save(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    private Capsule dummyCapsule() {
        return Capsule.builder()
                .capsuleId(10L)
                .uuid("uuid")
                .nickname("writerNick")
                .receiverNickname(null)
                .title("t")
                .content("c")
                .capsuleColor("a")
                .capsulePackingColor("b")
                .visibility("PUBLIC")
                .unlockType("TIME")
                .locationRadiusM(0)
                .maxViewCount(0)
                .currentViewCount(0)
                .isDeleted(0)
                .isProtected(0)
                .build();
    }
}

package back.fcz.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReasonType {

    SPAM("스팸/광고"),
    OBSCENITY("음란물"),
    HATE("혐오/차별"),
    FRAUD("사기/금전 요구"),
    ETC("기타");

    private final String description;
}

package back.fcz.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatus {

    ACTIVE("활성"),
    STOP("정지"),
    EXIT("탈퇴");

    private final String description;
}

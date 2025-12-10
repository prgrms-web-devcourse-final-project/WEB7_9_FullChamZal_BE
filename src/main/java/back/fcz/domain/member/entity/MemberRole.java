package back.fcz.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

    USER("일반 회원"),
    ADMIN("관리자");

    private final String description;
}

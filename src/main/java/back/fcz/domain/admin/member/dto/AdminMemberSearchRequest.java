package back.fcz.domain.admin.member.dto;

import back.fcz.domain.member.entity.MemberStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 관리자 회원 목록 조회용 검색 조건 DTO
 * (Controller에서 쿼리 파라미터를 받아서 이 객체로 묶어 Service로 넘김)
 */
@Getter
@Builder
public class AdminMemberSearchRequest {

    // 0부터 시작하는 페이지 번호
    private final int page;

    // 페이지 크기
    private final int size;

    // 회원 상태 필터 (null이면 전체)
    private final MemberStatus status;

    // 닉네임 / userId / 전화번호 등 키워드 검색
    private final String keyword;

    // 가입일 시작 (null이면 제한 없음)
    private final LocalDate from;

    // 가입일 끝 (null이면 제한 없음)
    private final LocalDate to;

    public static AdminMemberSearchRequest of(
            Integer page,
            Integer size,
            MemberStatus status,
            String keyword,
            LocalDate from,
            LocalDate to
    ) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? 20 : size;

        return AdminMemberSearchRequest.builder()
                .page(safePage)
                .size(safeSize)
                .status(status)
                .keyword(keyword)
                .from(from)
                .to(to)
                .build();
    }
}

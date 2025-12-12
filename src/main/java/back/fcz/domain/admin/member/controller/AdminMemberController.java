package back.fcz.domain.admin.member.controller;

import back.fcz.domain.admin.member.dto.*;
import back.fcz.domain.admin.member.service.AdminMemberService;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
@Tag(name = "Admin-Member", description = "관리자 - 회원 관리 API")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    /**
     * 1-1 회원 목록 조회
     *
     * GET /api/v1/admin/members
     */
    @GetMapping
    @Operation(summary = "회원 목록 조회", description = "관리자가 회원 리스트를 페이지네이션으로 조회합니다.")
    public ApiResponse<PageResponse<AdminMemberSummaryResponse>> getMembers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AdminMemberSearchRequest cond = AdminMemberSearchRequest.of(
                page,
                size,
                status,
                keyword,
                from,
                to
        );

        PageResponse<AdminMemberSummaryResponse> result = adminMemberService.searchMembers(cond);

        return ApiResponse.success(result);
    }

    // 1-2 회원 상세 조회
    @GetMapping("/{memberId}")
    @Operation(summary = "회원 상세 조회", description = "관리자가 특정 회원의 상세 정보를 조회합니다.")
    public ApiResponse<AdminMemberDetailResponse> getMemberDetail(
            @PathVariable Long memberId
    ) {
        AdminMemberDetailResponse detail = adminMemberService.getMemberDetail(memberId);
        return ApiResponse.success(detail);
    }

    /**
     * 1-3 회원 상태 변경
     */
    @PatchMapping("/{memberId}/status")
    @Operation(summary = "회원 상태 변경", description = "관리자가 회원의 상태를 변경합니다. (ACTIVE / STOP / EXIT)")
    public ApiResponse<AdminMemberStatusUpdateResponse> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestBody @Valid AdminMemberStatusUpdateRequest request
    ) {
        AdminMemberStatusUpdateResponse response = adminMemberService.updateMemberStatus(memberId, request);
        return ApiResponse.success(response);
    }
}

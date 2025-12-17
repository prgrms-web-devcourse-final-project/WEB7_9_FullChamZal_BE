package back.fcz.domain.storytrack.controller;

import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.storytrack.dto.request.UpdatePathRequest;
import back.fcz.domain.storytrack.dto.response.DeleteParticipantResponse;
import back.fcz.domain.storytrack.dto.response.DeleteStorytrackResponse;
import back.fcz.domain.storytrack.dto.response.UpdatePathResponse;
import back.fcz.domain.storytrack.service.StorytrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storytrack")
public class storytrackController {

    // 로그인 회원 확인
    private final CurrentUserContext currentUserContext;

    // 스토리트랙 서비스
    private final StorytrackService storytrackService;

    //삭제
    // 작성자 - 스토리트랙 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<DeleteStorytrackResponse> deleteStorytrack(
            @RequestParam Long storytrackId
    ){
        // Long loginMember = currentUserContext.getCurrentUser().memberId();
        // 포스트맨 테스트용
        Long loginMember = 1L;

        return ResponseEntity.ok(storytrackService.deleteStorytrack(loginMember, storytrackId));
    }

    // 참여자 - 참여 스토리트랙 삭제(참여종료)
    @DeleteMapping("/delete/participant")
    public ResponseEntity<DeleteParticipantResponse> deleteParticipant(
            @RequestParam Long storytrackId
    ){
        // Long loginMember = currentUserContext.getCurrentUser().memberId();

        // 포스트맨 테스트용
        Long loginMember = 2L;

        return ResponseEntity.ok(storytrackService.deleteParticipant(loginMember, storytrackId));
    }

    // 수정
    // 작성자 - 스토리트랙 경로 수정
    @PutMapping("/update")
    public ResponseEntity<UpdatePathResponse> updatePath(
            @RequestParam Long storytrackStepId,
            @RequestBody UpdatePathRequest request
    ){
         // Long loginMember = currentUserContext.getCurrentUser().memberId();

        // 포스트맨 테스트용
        Long loginMember = 1L;

        return ResponseEntity.ok(storytrackService.updatePath(request, storytrackStepId, loginMember));
    }


    // 생성
    // 스토리트랙 생성

    // 스토리트랙 참여자 생성

    // 조회
    // 전체 스토리트랙 조회

    // 스토리트랙 상세 조회

    // 스토리트랙 경로 조회

    // 생성자 : 생성한 스토리트랙 목록 조회

    // 참여자 : 참여한 스토리트랙 목록 조회

    // 참여자 : 스토리트랙 진행 상세 조회

    // 스토리트랙 캡슐 오픈
}

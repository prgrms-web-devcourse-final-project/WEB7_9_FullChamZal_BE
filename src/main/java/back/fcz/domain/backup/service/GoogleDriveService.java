package back.fcz.domain.backup.service;

import back.fcz.domain.backup.entity.Backup;
import back.fcz.domain.backup.repository.BackupRepository;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Google Drive API와의 통신을 전담하는 서비스
 * - Google OAuth 2.0 토큰 발급 및 갱신 처리
 * - Google Drive API를 이용해 캡슐의 CSV 파일 업로드
 * - access Token 만료 시 refresh Token 기반 자동 갱신
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {
    @Value("${google.drive.client-id}")
    private String clientId;

    @Value("${google.drive.client-secret}")
    private String clientSecret;

    @Value("${google.drive.redirect-uri}")
    private String redirectUri;

    private final BackupRepository tokenRepository;

    private final RestTemplate restTemplate;

    /**
     * Google OAuth 2.0 로그인 및 Google Drive 권한 동의 화면 URL 생성
     * - drive.file 권한만 요청 (파일 생성/수정)
     * - offline access 설정으로 refresh 토큰 발급 유도
     */
    public String generateAuthUrl() {
        return UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "https://www.googleapis.com/auth/drive.file")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build().toUriString();
    }

    /**
     * 사용자가 Google 로그인 및 권한 동의를 완료한 후,
     * Google로 부터 전달받은 인가 코드를 사용해 토큰 발급
     */
    public Map<String, Object> getTokensFromGoogle(String code) {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        return restTemplate.postForObject("https://oauth2.googleapis.com/token", params, Map.class);
    }

    /**
     * access 토큰 유효성 검사 및 갱신 처리
     * 토큰 만료 시점이 임박했거나 만료된 경우, refresh 토큰을 사용해 새 access 토큽 발급
     */
    private String getOrRefreshAccessToken(Backup backup) {
        // access 토큰 만료가 현재 시각으로부터 5분 이상 남았다면, 토큰 재발급 X
        if (backup.getExpiryDate().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return backup.getAccessToken();
        }

        // refresh 토큰으로 새 access 토큰 요청
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", backup.getRefreshToken());
        params.add("grant_type", "refresh_token");

        Map<String, Object> response = restTemplate.postForObject("https://oauth2.googleapis.com/token", params, Map.class);

        // access 토큰 갱신
        if (response != null && response.containsKey("access_token")) {
            String newAccessToken = (String) response.get("access_token");
            long expiresIn = ((Number) response.get("expires_in")).longValue();

            backup.updateAccessToken(newAccessToken, expiresIn);
            tokenRepository.save(backup);
            return newAccessToken;
        }
        throw new BusinessException(ErrorCode.GOOGLE_TOKEN_UPDATE_FAIL);
    }

    /**
     * 캡슐 데이터를 CSV 파일로 변환하여 Google Drive에 업로드
     */
    public void uploadCapsule(Backup token, Capsule capsule) throws Exception {
        // access 토큰이 유효한 지 확인
        String validAccessToken = getOrRefreshAccessToken(token);
        AccessToken googleAccessToken = new AccessToken(validAccessToken, null);
        GoogleCredentials credentials = GoogleCredentials.create(googleAccessToken);

        Drive driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("dear.___")
                .build();

        // 캡슐 데이터를 csv 문자열로 변환
        String csvData = convertToCsv(capsule);

        File fileMetadata = new File();
        // 파일명: [dear.___] title_Nickname.csv
        String fileName = String.format("[dear.___] %s_%s.csv", capsule.getTitle(), capsule.getNickname());

        fileMetadata.setName(fileName);
        fileMetadata.setMimeType("text/csv");

        ByteArrayContent mediaContent = ByteArrayContent.fromString("text/csv", csvData);
        driveService.files().create(fileMetadata, mediaContent).setFields("id").execute();
    }

    /**
     * 캡슐 엔티티를 CSV 문자열로 변환
     */
    private String convertToCsv(Capsule c) {
        String header = "제목,내용,전송자,(캡슐 해제) 장소,(캡슐 해제) 주소,(캡슐 해제) 시작 시간,(캡슐 해제) 마감 시간\n";

        String title = c.getTitle();
        String content = c.getContent();
        String senderNick = c.getNickname();
        String locationName = c.getLocationName() != null ? c.getLocationName() : "";
        String address = c.getAddress() != null ? c.getAddress() : "";
        String unlockAt = c.getUnlockAt() != null ? c.getUnlockAt().toString() : "";
        String unlockUntil = c.getUnlockUntil() != null ? c.getUnlockUntil().toString() : "";

        // 3. CSV 데이터 생성
        return header + String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                escapeCsv(title),
                escapeCsv(content),
                escapeCsv(senderNick),
                escapeCsv(locationName),
                escapeCsv(address),
                escapeCsv(unlockAt),
                escapeCsv(unlockUntil)
        );
    }

    /**
     * CSV 내의 따옴표(")를 처리하기 위한 헬퍼 메서드
     */
    private String escapeCsv(String data) {
        if (data == null) return "";
        return data.replace("\"", "\"\"");
    }
}

# Dear.___
> **“Real-time이 주지 못하는 Right-time의 감동."** <br />
> ”빠름”이 당연해진 시대, 진짜 특별함은 “적절한 순간에 도달하는 경험”에서 만들어집니다.

<img width="1897" height="695" alt="image" src="https://github.com/user-attachments/assets/72d312c3-566b-47a5-8571-5b032e5e5a6f" />

---

## 📚 목차
- [📋 프로젝트 개요](#-프로젝트-개요)
  - [즉각성 피로 사회의 안티테제](#즉각성-피로-사회의-안티테제)
- [📅 개발 기간 & 팀 구성](#-개발-기간--팀-구성)
- [🚀 실제 배포 사이트](#-실제-배포-사이트)
- [⚙️ 핵심 기능](#️-핵심-기능)
  - [🔒 개인 캡슐](#-개인-캡슐-11-프라이빗-메시지)
  - [🌍 공개 캡슐](#-공개-캡슐)
  - [📍 GPS 기반 위치 해제 캡슐](#-gps-기반-위치-해제-캡슐)
  - [⏰ 선착순 · 특정 시간 한정 캡슐](#-선착순--특정-시간-한정-캡슐)
  - [🧭 스토리트랙](#-스토리트랙-연속-해제-콘텐츠)
  - [🛡️ 이상 감지 모니터링 & 접근 제어](#️-이상-감지-모니터링--접근-제어)
  - [🔐 안전한 인증·접근 구조](#-안전한-인증접근-구조)
  - [📊 로그 기반 상태 관리](#-로그-기반-상태-관리)
- [📹 시연 영상](#-시연-영상)
- [📊 시스템 아키텍처](#-시스템-아키텍처)
- [🏗️ 기술 스택](#️-기술-스택)
- [🛠️ Backend 시작하기](#️-backend-시작하기)
- [📝 협업 규칙](#-협업-규칙)
- [📚 참고 자료](#-참고-자료)
- [📄 라이선스](#-라이선스)
- [📞 문의 및 이슈](#-문의-및-이슈)

---

## 📋 프로젝트 개요
**Dear.___**(이하 디어)는 시간과 공간이라는 물리적 조건을 디지털 메시지에 접목시킨 감성 커뮤니케이션 플랫폼입니다. <br />
특정 날짜가 되거나, 특정 장소에 도달해야만 열람할 수 있는 디지털 타임 캡슐을 통해 즉각적 소통에 지친 현대인들에게 ‘기다림의 가치’를 되새기게 합니다.

### 즉각성 피로 사회의 안티테제

청년층은 ‘실시간 트렌드’와 ‘커뮤니티성 콘텐츠’에 높은 관심을 보이지만, 동시에 **짧고 강렬한 콘텐츠의 연속 소비로 인한 피로를 경험**하고 있습니다. 틱톡, 유튜브 숏츠, 인스타 릴스 등 15초 영상을 끝없이 스크롤하는 일상 속에서 ‘**설렘의 시간**’은 사라졌습니다.

디어는 **‘시간’과 ‘공간’이라는 제약**을 오히려 경험의 **핵심 가치**로 제시합니다. 우리는 즉각성의 반대편이 아니라, Right-time 경험의 새로운 기준입니다.

---

## 📅 개발 기간 & 팀 구성

- **개발 기간**: 2025-12-03 ~ 2026-01-07
- [**Frontend Github**](https://github.com/prgrms-web-devcourse-final-project/WEB6_7_FullChamZal_FE): 노영권(팀장), 김지호(팀원), 이상엽(팀원), 정민경(팀원)


|              Backend(PO)               |              Backend(팀장)               |             Backend(팀원)             |              Backend(팀원)               |               Backend(팀원)               |               Backend(팀원)               |
| :---------------------------------------: | :---------------------------------------: | :------------------------------------: | :---------------------------------------: | :---------------------------------------: | :---------------------------------------: |
| ![](https://github.com/samuel426.png)  | ![](https://github.com/Hyeseung-OH.png)  | ![](https://github.com/qivvoon.png)  | ![](https://github.com/Plectranthus.png)  | ![](https://github.com/LimByeongSu.png)  | ![](https://github.com/otr995.png)  |
| [우성현](https://github.com/samuel426) | [오혜승](https://github.com/Hyeseung-OH) | [강지원](https://github.com/qivvoon) | [양희원](https://github.com/Plectranthus) | [임병수](https://github.com/LimByeongSu) | [허성찬](https://github.com/otr995) |

---

## 🚀 실제 배포 사이트
https://web.dear4u.cloud/

---

## ⚙️ 핵심 기능

### 🔐 안전한 인증·접근 구조
<p align="center"> <img width="500" alt="image" src="https://github.com/user-attachments/assets/fa110385-b5f3-41a8-bc4e-7c91952b9861" /> </p>

- 쿠키 기반 JWT 인증 (Access / Refresh Token)
- Redis 기반 토큰 블랙리스트 및 제한 관리
- URL + 비밀번호 방식과 회원 인증 방식 병행 지원
- 전화번호 및 개인정보 AES-256, SHA-256 암호화 저장 정책
- 개인정보 최소 수집 및 비회원 접근 시 보안 정책 적용
- 구글 소셜 로그인 지원

---

### 🔒 개인 캡슐 (1:1 프라이빗 메시지)
<p align="center"> <img width="600" alt="image" src="https://github.com/user-attachments/assets/e75a29dd-cc49-4589-836f-cc908f356b83" /> </p>

- 연인 · 가족 · 친구 등 **특정 수신자만 열 수 있는 개인 메시지**
- 시간 / 장소 / 시간+장소 조건으로 열람 제어
- 미래의 나에게 보내는 기록, 기념일 메시지, 목표 달성 후 열어보는 편지 등 활용


### 🌍 공개 캡슐
<p align="center"> <img width="600" alt="image" src="https://github.com/user-attachments/assets/881bc971-25b8-4d0f-ad21-aa1adf5f1953" /> </p>

- 관광지 · 명소 · 특정 장소에 남겨진 **익명의 메시지**
- 장소 / 시간+장소 조건으로 열람 제어
- **같은 장소에 도착한 사람만 발견 가능**
- “이 장소에 온 당신에게”라는 메시지를 통해 낯선 사람과의 **공간 기반 연결 경험** 제공
- 인스타그래머블한 순간 + 우연한 발견의 감동


### 📍 GPS 기반 위치 해제 캡슐
<p align="center"> <img width="600" alt="image" src="https://github.com/user-attachments/assets/960e4a31-92e7-4c20-a0bd-71e3095b03e4" /> </p>

- 특정 **장소에 실제로 도달해야만 열리는 메시지**
- GPS 좌표 + 반경 기반 검증으로 현실 공간과 디지털 메시지를 연결
- “여기에 와야 읽을 수 있어”라는 **물리적 퀘스트 경험** 제공


### ⏰ 선착순 · 특정 시간 한정 캡슐

- 특정 **시간과 장소**에 동시에 도달한 사람만 열 수 있는 이벤트성 콘텐츠
- 예시
    - `2026-01-01 00:00`
    - 광화문 광장 **선착순 100명**에게만 공개되는 신년 메시지
- 실시간 참여 욕구를 **제한된 경험의 가치**로 전환

---

### 🧭 스토리트랙 (연속 해제 콘텐츠)
<p align="center"> <img width="600" alt="image" src="https://github.com/user-attachments/assets/81aece14-fbd3-4ecf-9de5-363714710d51" /> </p>

- 여러 장소를 순회하며 **순차적으로 열리는 캡슐 묶음**
- 여행·데이트·미션을 하나의 **스토리 흐름으로 게임화**
- 예시
    - 제주도 성산일출봉 → 월정리 해변 → 한라산 정상
    - 각 장소마다 열리는 연인의 메시지
   
---   

### 🛡️ 이상 감지 모니터링 & 접근 제어

- 캡슐 열람 시 **시간·위치 조작 여부를 지속적으로 검증**
- 다음과 같은 이상 패턴을 탐지:
    - 비정상적인 GPS 이동
    - 서버 시간 대비 과도한 클라이언트 시간 차이
    - 반복적인 조건 실패 요청
- 이상 접근이 감지될 경우 단계적 대응 적용:
    - Rate Limit 제한
    - IP 기반 차단
    - 의심 점수 누적 및 TTL 기반 제재
- 서비스 신뢰성을 유지하기 위한 **보안·악용 방지 핵심 기능**

---

### 📊 로그 기반 상태 관리

- 모든 캡슐 열람 시 **열람 로그 기록**
- 로그 기반으로:
    - 캡슐 수정 가능 여부 판단
    - 공개 캡슐 조회수 집계
    - 이상 접근 탐지 및 통계 활용

---

## 📹 시연 영상
[![시연 영상](https://img.youtube.com/vi/82a3T85IkBY/0.jpg)](https://youtu.be/82a3T85IkBY)

---

## 📊 시스템 아키텍처
<img width="1252" height="1112" alt="image" src="https://github.com/user-attachments/assets/24ddd8fd-7501-4811-9909-6c1991492a60" />

---

## 🏗️ 기술 스택

### Backend
```
/* Language & Framework */
Java 21+
Spring Boot 3.5.6
Spring Security
Spring Data JPA
Spring Validation
Spring aop

/* Database & Cache */
MySQL Connector-J 9.1.0
H2 Database 2.x
Spring Data Redis

/* Library */
resilience4j

/* Test */
Junit, Assertj
JMeter (동시성 테스트)
```

### External APIs
```
/* SMS 문자 발송 */
솔라피

/* AI api (콘텐츠 필터링용) */
OpenAI API

/* 간편 로그인 관련 */
Google OAuth 2.0

/* 구글 드라이브 백업 */
Google API
```

### DevOps & Tools
```
/* API Documentation */
SpringDoc OpenAPI 2.5.0 (Swagger UI)

/* Development Tools */
Git, GitHub
Spring Boot Docker Compose Integration
Cursor
Intellij

/* CI/CD */
GitHub Actions

/* Deployment */
Vercel – Frontend Deployment
AWS ec2, S3, Nginx – Backend Deployment
```

---

## 🛠️ Backend 시작하기

프로젝트를 실행하기 전에 다음 항목들이 설치되어 있어야 합니다.

- **Java 21 이상**
- **Gradle 8.x** (Wrapper 포함)
- **Docker & Docker Compose**

> ⚠️ **Redis는 별도 설치가 필요하지 않습니다.**  
> `docker-compose` 실행 시 Redis 컨테이너가 자동으로 함께 실행됩니다.

---

### 🐳 인프라 구성 (Local)

본 프로젝트는 로컬 개발 환경에서 Docker Compose 기반으로 Redis를 자동 실행합니다.

| 구성 요소 | 기술 스택 | 비고 |
|---------|---------|------|
| Application | Spring Boot 3.5.6 | Java 21 기반 |
| Database | H2 (기본) / MySQL | 개발/테스트 환경 |
| Cache / Security | Redis | docker-compose 자동 실행 |

---

### ▶️ 실행 방법 (권장)

#### 1. Redis 컨테이너 실행

프로젝트 루트 디렉토리에서 다음 명령어를 실행합니다.
```bash
docker-compose up -d
```

- Redis 컨테이너가 자동으로 실행됩니다
- **Port:** `6379`
- 별도 설정 없이 바로 사용 가능합니다

#### 2. Backend 실행

##### 터미널에서 실행
```bash
./gradlew bootRun
```

##### IDE에서 실행

IntelliJ IDEA 등의 IDE를 사용하는 경우, Active Profile을 다음과 같이 설정합니다.
```
Active Profile: dev
```

---

### 📌 Redis 관련 참고 사항

Redis는 다음 기능에 사용됩니다.

- **JWT 토큰 블랙리스트** 관리
- **SMS 인증 재시도 제한**
- **의심 점수 / Rate Limit / 제재 TTL 관리**
- **다양한 캐싱 정책**

> ⚠️ **주의:**  
> `docker-compose`를 사용하지 않을 경우 위 기능들은 정상적으로 동작하지 않을 수 있습니다.  
> 로컬 테스트 기준으로는 `docker-compose` 실행을 필수로 권장합니다.

---

### 🔐 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하고 아래 내용을 참고하여 설정합니다.
```env
# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Database (H2 - default)
DB_URL=jdbc:h2:./db_dev;MODE=MySQL
DB_USERNAME=dummy
DB_PASSWORD=dummy

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Crypto / Security
AES_KEY=your-aes-key-here
SHA_SALT=your-sha-salt-here
JWT_SECRET_KEY=your-jwt-secret-here

# SMS (CoolSMS)
COOLSMS_API_KEY=your-coolsms-api-key
COOLSMS_API_SECRET=your-coolsms-api-secret
COOLSMS_FROM_NUMBER=your-phone-number

# OpenAI
OPENAI_API_KEY=sk-your-openai-key

# Google OAuth
GOOGLE_OAUTH_ID=your-google-oauth-id
GOOGLE_OAUTH_SECRET=your-google-oauth-secret
GOOGLE_OAUTH_REDIRECT=your-redirect-uri

# Google Drive
GOOGLE_DRIVE_LOCAL_REDIRECT=your-local-redirect
GOOGLE_DRIVE_REDIRECT=your-prod-redirect

# AWS S3
S3_BUCKET_NAME=your-bucket-name
S3_REGION=your-region
```

> ⚠️ **보안 주의사항:**  
> - `.env` 파일은 반드시 `.gitignore`에 포함되어야 합니다.
> - 실제 Secret 값은 개인별로 발급받아 사용해야 합니다.
> - `dummy` 값은 실제 서비스 키로 교체해야 정상 작동합니다.

---

### 📎 참고사항

- **기본 실행 DB는 H2입니다.**
  - 별도 MySQL 설정 없이 즉시 실행 가능합니다.
  - H2 Console: `http://localhost:8080/h2-console`

- **Redis 미실행 시**
  - 일부 보안/제한 기능은 비활성화될 수 있습니다.

- **API 문서 (Swagger UI)**
```
  http://localhost:8080/swagger-ui.html
```

---

## 📝 협업 규칙

### Git 브랜치 전략 (GitHub Flow)

```
main (배포용, 직접 Push 금지)
    ├── dev (개발용, 직접 Push 금지, 병합 후 브랜치 삭제 권장)
    ├── feature/user-login
    ├── feature/github-api-integration
    ├── feature/analysis-orchestration
    └── feature/community-board
```

### 커밋 컨벤션

```
<타입>(<도메인>): <제목>
```

#### 타입 종류
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 코드 리팩토링
- `chore`: 빌드 설정, 패키지 관리
- `docs`: 문서 수정
- `test`: 테스트 코드
- `init`: 프로젝트 초기 설정
- `style`: 코드 스타일 변경
- `merge`: 브랜치 병합 및 최신화

#### 예시
```
feat(analysis): GitHub 저장소 메타데이터 수집 API 구현
```

### 코드 리뷰 프로세스

1. **PR 생성**: feature 브랜치 → dev 브랜치
2. **리뷰 요청**: 최소 1명 이상의 팀원
3. **리뷰 체크리스트**
   - 기능이 의도대로 동작하는가?
   - 코딩 컨벤션을 준수하는가?
   - 보안 취약점이 없는가?
   - 예외 처리가 적절한가?
4. **승인 후 병합**: 매일 오전, 오후 특정 병합 시간에 병합

---

## 📚 참고 자료
- Spring Boot / Security / Data JPA
- Redis
- OpenAI API
- Google OAuth / Drive API
- Swagger / Resilience4j

---

## 📄 라이선스
이 프로젝트는 데브코스 최종 프로젝트로 제작되었으며, 학습 및 포트폴리오 목적으로 사용됩니다.

---

## 📞 문의 및 이슈
프로젝트 관련 문의사항이나 버그 제보는 GitHub Issues를 통해 등록해 주세요.


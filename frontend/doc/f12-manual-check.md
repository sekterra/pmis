# F12 수동 확인

## 사용자가 직접 볼 항목

1. 플랫폼 로그인 → 테넌트 상세(ACTIVE) → [테넌트 접속] → 사유 입력/미입력 → [접속 준비] → [새 탭에서 열기]
2. 새 탭에서 교환 성공 → 지원 접속 띠와 표시 이름 → 메뉴(관리자) 사용
3. 사용자 등록·임시 비밀번호 확인 → [지원 접속 종료] → 종료 화면
4. 티켓 만료·재사용·잘못된 주소 화면
5. 테넌트 관리자로 로그인 → `/access/support-access` 기록·Drawer
6. 플랫폼 `/support-accesses` 필터·Drawer

값(주소, 계정, 비밀번호, 티켓)은 이 문서에 적지 않는다.

## 자동 테스트로 대신하는 항목

- `support access rules` — 티켓 파싱, 카운트다운, 쿼리
- `platform support access services` / `tenant support access services` — 발급·교환 본문
- `support access entry flow` — 진입 성공/실패, 민감값 잔존, 영역 차단
- `support access modal` — 2단계 링크·카운트다운 0
- `tenant action buttons` — ACTIVE일 때만 테넌트 접속

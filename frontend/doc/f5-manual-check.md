# 서버 메뉴 수동 확인

테넌트 앱(`npm run dev`)으로 로그인한다. 주소, 계정, 비밀번호는 여기에 적지 않는다. 백엔드를 먼저 띄운다.

아래만 직접 본다. 나머지 판정은 `src/core/menu/flow.test.tsx`와 `src/access.test.ts`가 목으로 확인한다.

- 실제 `menus/my` 응답으로 사이드 메뉴가 나오는지
- TENANT_ADMIN과 권한이 적은 사용자의 메뉴·버튼 차이
- 주소창으로 권한이 없는 화면에 들어가면 403인지
- 언어를 바꾸면 메뉴 이름이 바뀌는지
- 화면을 써 본 전체 사용감

1. 그룹 아래 화면, 표에 없는 메뉴 제외, 빈 그룹 숨김, 정렬, 두 번째 화면의 로컬 이름. 자동 테스트: `flow.test.tsx`의 `nests known screens, drops unknown menus, and hides an empty group`, `uses the local name for a secondary screen and refreshes the server name`
2. actions 있음/없음, 메뉴 없음, 로딩·실패 시 false. 자동 테스트: `access.test.ts`, `flow.test.tsx`의 `keeps every action false until the menu is ready`
3. 로그인·세션 복구·언어 변경·비밀번호 강제 중 미호출·변경 성공 후 호출·로그아웃과 다른 탭·요청 합치기. 자동 테스트: `loads once after login and session recovery, again after a language change, and not during a forced password change`, `loads after a successful password change and clears the menu on logout`, `shares one request when loads overlap`
4. 실패 안내와 다시 시도, 401은 실패 안내가 아님. 자동 테스트: `shows a retry after a load failure and recovers without treating 401 as that failure`
5. READ 없는 직접 접근은 403, READ가 있으면 통과, 로딩 중에는 본문이 보이지 않음, 환영은 가드 밖. 자동 테스트: `sends a direct visit without read access to the 403 page and skips unrelated paths`
6. READ만 있으면 등록·수정·사용 중지 버튼이 없고, 권한이 있으면 보인다. 자동 테스트: `hides write buttons for a read-only user and shows them when every action is granted`
7. Access 토큰과 메뉴는 브라우저 저장소에 없다. 자동 테스트: `does not store the access token or the menu in browser storage`
8. 플랫폼 모드는 `menus/my`를 호출하지 않는다. 자동 테스트: `does not call menus in platform mode`. 플랫폼 산출물 문자열 검색에도 없다

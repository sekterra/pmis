# 공통코드 화면 수동 확인

테넌트 앱(`npm run dev`)으로 로그인한다. 주소, 계정, 비밀번호는 여기에 적지 않는다. 백엔드를 먼저 띄운다.

아래만 직접 본다. 나머지 화면 흐름은 `src/pages/mdm/common-code/flow.test.tsx`가 목으로 확인한다.

- 등록, 수정, 사용 중지, 복구가 실제 서버에 반영되는지
- 서버가 돌려준 오류 문구가 화면에 그대로 보이는지
- 9. 좁은 화면에서 그룹 패널 아래에 코드 패널이 쌓이는지
- 화면을 써 본 전체 사용감

1. `/mdm/common-code`가 열리고, 그룹을 고르기 전에는 오른쪽 안내 문구가 보인다. 자동 테스트: `flow.test.tsx`의 `shows the empty hint and does not load codes before a group is chosen`
2. 그룹 검색, 솔루션 코드, 사용 여부 조회가 동작한다. 사용 중지 그룹은 흐리고 [복구]가 있다. 자동 테스트: `flow.test.tsx`의 `searches groups with page, size, keyword, solution, and use flag`, `shows restore on a disabled group and hides disable`
3. 그룹 행을 고르면 주소에 `?group=`이 생기고, 새로고침 후에도 그 그룹의 코드가 보인다. 자동 테스트: `flow.test.tsx`의 `puts the selected group in the query and loads that group on entry`, `loads codes from an existing group query without another click`
4. [그룹 등록]에서 기본 정보와 언어별 이름 탭을 저장한다. 이름이 빈 언어는 빠진다. 그룹 코드는 수정 화면에서 바꿀 수 없다. 저장 중에는 저장이 한 번만 나간다. 자동 테스트: `flow.test.tsx`의 `saves a group without empty languages and reloads the list`, `keeps the group code read-only when editing`, `disables save while the request is in flight`
5. [코드 등록]/[수정]에서 코드 값, 날짜(시작≤종료), 정렬, 확장 속성, 변경 사유, 언어별 이름을 저장한다. 자동 테스트: `flow.test.tsx`의 `saves code fields and blocks an end date before the start date`, `does not save a code value longer than the group limit`
6. 계층형 그룹은 코드가 트리로 보이고, 상위 코드에서 자신과 하위는 고를 수 없다. 부모 없는 행은 맨 위에 보인다. 자동 테스트: `flow.test.tsx`의 `shows a hierarchy as a tree and omits self and descendants from parents`
7. [사용 중지] 확인창에 논리 삭제(use_yn=N)가 적혀 있다. 취소하면 호출이 없고, 확인하면 목록을 다시 부른다. 사용 중인 코드가 있는 그룹은 서버 문구가 있고, 문구가 비어 있으면 화면 문구가 보인다. [복구] 후 목록이 다시 불러와지고 선택한 그룹이 남는다. 자동 테스트: `flow.test.tsx`의 `explains logical delete and skips the call on cancel`, `reloads the list after a confirmed disable`, `shows the server conflict message`, `uses the local conflict text when the server message is empty`, `reloads after restore and keeps the selected group`
8. [변경 이력]에서 시각, 유형 문구, 사유, 사용자 pk가 보이고, 행을 펼치면 변경 전/후 항목이 보인다. 자동 테스트: `flow.test.tsx`의 `pages history and shows the change, reason, user, and value diff`
9. 좁은 화면에서는 그룹 패널 아래에 코드 패널이 쌓인다. 직접 확인 필요(가로 폭에 따른 배치라 자동 테스트로 보지 않는다)
10. 플랫폼 콘솔 메뉴에는 이 화면이 없다. 직접 확인 필요(F3에서 플랫폼 빌드 결과로 이미 확인했다. 라우트 테스트를 중복으로 넣지 않는다. 이 단계의 `build:platform`으로 다시 확인한다)

오류 경로 자동 테스트: `flow.test.tsx`의 `shows a field error under the input`, `shows a permission notice when the list is forbidden`, `keeps the selected group when a later list call fails`

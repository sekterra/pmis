# 다국어 화면 수동 확인

테넌트 앱(`npm run dev`)으로 로그인한다. 주소, 계정, 비밀번호는 여기에 적지 않는다. 백엔드를 먼저 띄운다.

아래만 직접 본다. 나머지 화면 흐름은 `src/pages/mdm/i18n/flow.test.tsx`가 목으로 확인한다.

- 등록, 수정, 사용 중지, 복구가 실제 서버에 반영되는지
- 서버가 돌려준 오류 문구가 화면에 그대로 보이는지
- 좁은 화면에서 네임스페이스 아래에 메시지 표가 쌓이는지
- 화면을 써 본 전체 사용감

1. 네임스페이스를 고르기 전에는 안내가 보이고 메시지 목록을 부르지 않는다. 자동 테스트: `flow.test.tsx`의 `shows the empty hint and does not load messages before a namespace is chosen`
2. 네임스페이스 검색과 사용 여부는 page, size, keyword, useYn으로 조회한다. 자동 테스트: `flow.test.tsx`의 `searches namespaces with page, size, keyword, and use flag`
3. 행을 고르면 `?ns=`가 생기고, 그 쿼리로 다시 열어도 메시지를 불러온다. 자동 테스트: `flow.test.tsx`의 `puts the selected namespace in the query and loads it on entry`, `loads messages from an existing namespace query without another click`
4. 네임스페이스 등록, 수정 시 코드 읽기 전용, 저장 연타. 자동 테스트: `flow.test.tsx`의 `saves a namespace and reloads the list`, `keeps the namespace code read-only when editing`, `disables save while the request is in flight`
5. 사용 중지 확인창, 취소, 확인 후 재조회, 복구 후 선택 유지. 자동 테스트: `flow.test.tsx`의 `explains that a disabled namespace leaves messages out of bundles and skips cancel`, `reloads after a confirmed namespace disable`, `reloads after restore and keeps the selected namespace`
6. 메시지 등록은 빈 번역을 빼고, 수정은 키를 잠그고 버전을 보여 준다. 삭제와 복구는 목록을 다시 부른다. 자동 테스트: `flow.test.tsx`의 `saves a message without empty languages`, `keeps the message key read-only and shows the version when editing`, `explains logical delete and skips the call on cancel`, `reloads after a confirmed message disable and keeps the namespace`, `reloads after restoring a message`
7. 좁은 화면 배치. 직접 확인 필요(가로 폭에 따른 배치라 자동 테스트로 보지 않는다)
8. 플랫폼 콘솔에는 이 화면이 없다. 직접 확인 필요(이 단계의 `build:platform` 산출물로 확인한다)

오류 경로 자동 테스트: `flow.test.tsx`의 `shows a field error under the input`, `shows the server conflict message`, `uses the local conflict text when the server message is empty`, `shows a permission notice when the list is forbidden`, `keeps the selected namespace when a later list call fails`

규칙 자동 테스트: `rules.test.ts`

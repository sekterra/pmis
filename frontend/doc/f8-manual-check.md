# F8 수동 확인

자동 테스트로 확인한 항목은 아래에 테스트 이름만 적는다. 아래 목록은 실제 서버에 로그인한 뒤 직접 본다. 주소, 계정, 비밀번호는 적지 않는다.

## 직접 볼 항목

1. 부서 메뉴가 서버 메뉴의 상위 그룹 아래에 보인다.
2. 회사 선택, 기준일, 종료된 부서 포함으로 조직도가 바뀌는지.
3. 최상위·하위 부서 등록, 수정(이름·상위 이동), 종료, 재개.
4. 변경 이력의 유형·이름·상위 번호·사유. 소속 인원 이름·직위·재직 상태.
5. 권한이 조회뿐인 계정에서 등록·수정·종료·재개 버튼이 없는지. 사람 조회 권한이 없으면 소속 인원 안내가 보이는지.
6. 화면 문구와 트리가 쓰기 편한지.

## 자동 테스트

- `department tree`: 부모 누락, 순서, 종료 표시, 조상 펼침
- `dept query` / `dept service`: 트리·이력·사람 파라미터, 등록·수정 본문
- `department rules`: 코드 길이, 유효기간, 상위 변경, 오류 칸
- `useCodeOptions` > `loads the group again after logout clears the cache`
- `company rules`: 회사 오류 문구 i18n 키
- `department page` > `loads a company tree and opens history and people once`
- `department page` > `adds a child with the parent filled and sends an effective date when the name changes`
- `department page` > `closes a department and reopens a closed one`
- `department page` > `shows a duplicate on the code field, a server close message, and hides buttons`
- `department page` > `blocks a direct visit without read and keeps the department route out of the platform area`

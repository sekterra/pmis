# F7 수동 확인

자동 테스트로 확인한 항목은 아래에 테스트 이름만 적는다. 아래 목록은 실제 서버에 로그인한 뒤 직접 본다. 주소, 계정, 비밀번호는 적지 않는다.

## 직접 볼 항목

1. 회사 메뉴가 서버 메뉴의 상위 그룹 아래에 보인다.
2. 목록 조회, 검색(검색어·유형·사용 여부), 초기화.
3. 회사 등록. 코드 형식, 유형, 국가, 상위 회사 검색과 선택 해제, 이름 번역.
4. 수정에서 회사 코드가 바뀌지 않는지. 저장 후 목록 반영.
5. 삭제 확인 후 사용 중지 표시와 복구. 하위 회사·부서·소속·마지막 자사로 막히면 서버 문구가 보이는지.
6. 권한이 조회뿐인 계정에서 등록·수정·삭제·복구 버튼이 없는지.
7. 화면 문구와 표가 쓰기 편한지.

## 자동 테스트

- `company query` / `company service`: 목록 파라미터, 등록·수정 본문
- `company rules`: 회사 코드, 사업자번호 공백, 상위 회사 제외, 오류 칸
- `code options` / `useCodeOptions`: 사용 중·유효기간, 언어 이름, 캐시, 빈 그룹
- `company page` > `searches, saves, edits a read-only code, deletes, and restores`
- `company page` > `shows field errors and a server message, and hides buttons without permission`
- `company page` > `blocks a direct visit without read and keeps the company route out of the platform area`

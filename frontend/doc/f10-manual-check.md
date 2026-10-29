# F10 수동 확인

자동 테스트로 확인한 항목은 아래에 테스트 이름만 적는다. 아래 목록은 실제 서버에 로그인한 뒤 직접 본다. 주소, 계정, 비밀번호는 적지 않는다.

## 직접 볼 항목

1. 역할 목록·등록·수정·삭제·복구.
2. 권한 표에서 켜기·끄기와 조회 권한 자동 연동, 메뉴 이름 전체 켜기·끄기.
3. 저장 후 그 역할을 가진 사용자의 메뉴가 다음 로그인 또는 새로고침 때 바뀌는지.
4. 테넌트 관리자 역할은 권한 표가 읽기 전용인지, 이름 수정은 되는지.
5. 권한이 적은 계정에서 등록·수정·삭제가 막히는지.
6. 화면 문구와 표가 쓰기 편한지.

## 자동 테스트

- `role query`: 목록 파라미터, 등록·수정 본문
- `permission matrix`: 메뉴 순서, 기능 열 순서, 없는 칸, 조회 연동, 메뉴 전체, 변경 여부
- `role rules`: 역할 코드 형식, `ROLE.DUPLICATE`, `ROLE.READ_REQUIRED`
- `role page` > `searches, creates a role, edits permissions, and restores`
- `role page` > `loads permissions once per role and keeps the admin role read only`
- `role page` > `shows server rejection, duplicate fields, and read-only actions`
- `role page` > `shows a menu-row error when read permission is missing`
- `person page` > `fills assignments when the person object is replaced during a slow load`

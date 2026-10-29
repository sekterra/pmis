# F11 수동 확인

자동 테스트로 확인한 항목은 아래에 테스트 이름만 적는다. 아래 목록은 실제 서버에 로그인한 뒤 직접 본다. 주소, 계정, 비밀번호는 적지 않는다.

## 직접 볼 항목

1. 사용자 등록과 임시 비밀번호 모달. 모달을 닫은 뒤 그 값으로 로그인하고 비밀번호를 바꾼다.
2. 역할에 맞는 메뉴가 보이는지.
3. 잠금, 휴면, 해제, 비밀번호 초기화, 삭제, 복구.
4. 역할 편집에서 기간과 철회.
5. 본인 행에서는 잠금·휴면·삭제·초기화·역할 편집이 없는지.
6. 권한이 적은 계정의 화면.
7. 화면 문구와 표가 쓰기 편한지.

## 자동 테스트

- `user query`: 목록 파라미터, 등록·수정 본문, 역할 교체, 상태 본문
- `user rules`: 로그인 아이디, 상태별 동작, 본인 판정, 역할 날짜, 오류 칸
- `user page` > `searches, creates a user, and clears the temporary password`
- `user page` > `changes status, resets a password, and saves role grants`
- `user page` > `hides actions by permission and protects the signed-in user`
- `department history parent label`: 이름, 번호, 없음

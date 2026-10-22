# F9 수동 확인

자동 테스트로 확인한 항목은 아래에 테스트 이름만 적는다. 아래 목록은 실제 서버에 로그인한 뒤 직접 본다. 주소, 계정, 비밀번호는 적지 않는다.

## 직접 볼 항목

1. 사람 등록·수정, 소속 추가·종료·주 소속 변경.
2. 프로필 이미지 올리기·삭제·보기.
3. 부서 화면 소속 인원에서 사람 화면으로 이동.
4. 권한이 적은 계정에서 등록·수정·삭제·이미지 올리기가 막히는지.
5. 화면 문구와 목록이 쓰기 편한지.

## 자동 테스트

- `people query`: 목록 파라미터, 등록·수정 본문, 소속 본문, 종료 기본값
- `people service`: 업로드 FormData, `fileId`, 소속 종료 주소
- `request body headers`: Content-Type 제거, blob 오류
- `person rules`: 퇴직 종료일, 부서장, 첫 소속, 오류 칸
- `profile image`: 업로드 다음 연결, 실패 시 연결 없음, blob URL, 404
- `person page` > `searches the list and opens a person from a row`
- `person page` > `creates a person and adds the first assignment as primary`
- `person page` > `warns before changing the primary assignment, then ends and restores`
- `person page` > `shows a server delete message, hides actions, and treats a missing photo as empty`
- `person page` > `opens the drawer from a person query and blocks the route outside the tenant area`
- `department page` > `loads a company tree and opens history and people once` (행 클릭 이동)
- `department page` > `keeps the newer department when an older history response arrives late`

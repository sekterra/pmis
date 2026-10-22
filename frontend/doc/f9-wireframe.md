# F9 사람 화면

합의 문서 `org-screens-wireframe.md` 1절·4절을 따른다. 아래는 이 단계 지시서가 정한 차이다.

- 소속 인원 행은 사람 화면이 생겼으므로 `/org/person?personPk=` 로 이동한다. `PERSON:READ` 가 있을 때만 이동한다.
- 사용자 계정 연결은 읽기 전용으로도 보이지 않는다. 지시서가 계정 화면으로 미룬다.
- 목록은 회사·부서 화면과 같이 antd `Table` 이다. 사진 유무 열을 더한다. 행마다 이미지를 받지 않는다.
- 프로필 이미지 설정 본문의 필드명은 `fileId` 다. 업로드 응답의 `filePk` 를 그 값으로 보낸다.
- 사람 수정은 화면에 없는 사무실 전화·설명을 상세 값으로 다시 보낸다. 등록 때는 보내지 않는다.
- 소속 수정은 화면에 없는 설명을 상세 값으로 다시 보낸다.
- 소속 등록은 `PERSON:CREATE`, 수정·종료·복구는 `PERSON:UPDATE`, 삭제는 `PERSON:DELETE` 다. 명세의 소속 API 권한을 따른다.
- 같은 회사·부서의 현재 소속 중복은 부서 칸, 사원번호 중복은 사원번호 칸에 표시한다.
- 회사 선택은 사용 중 회사 첫 페이지(size 200)다.
- 클라이언트 크기 검사는 문서의 기본 상한 50MB 다. 저장소 설정이 더 작으면 서버 문구를 보여 준다.

## 목록

- 검색: 검색어, 회사, 부서(회사를 고른 뒤 그 회사의 트리), 고용 유형, 재직 상태, 사용 여부. 조회·초기화.
- 열: 이름, 이메일, 휴대전화, 회사, 부서, 직위, 사원번호, 재직 상태, 사진 유무, 사용 여부. 회사부터 재직 상태는 주 소속 요약이고 없으면 `-`.
- 행 클릭 또는 상세로 Drawer. 사용 중지 행은 흐리게.

## Drawer

- 등록: 기본 정보만. 저장하면 수정 모드가 되고 소속 탭이 열린다.
- 기본 정보: 이름(필수), 이메일(형식), 휴대전화, 이름 번역, 프로필 이미지(저장된 사람만).
- 이미지: blob URL 로 보고, 닫거나 사람이 바뀌면 해제한다. 404 `FILE.NOT_FOUND` 는 사진 없음. 올리기는 업로드 다음 `PUT profile-image`. `FILE:CREATE` 가 없으면 버튼을 숨기고 이유를 보인다.
- 소속: 과거 포함, 시작일 내림차순, 사용 여부 필터. 추가·수정·종료·삭제·복구.
- 소속 폼: 회사 필수, 부서는 그 회사의 사용 중 부서. 회사를 바꾸면 부서를 비운다. 퇴직이면 종료일 필수. 부서 없이 부서장 불가. 현재 소속이 없으면 주 소속 체크를 끄고 안내한다. 있으면 기존 주 소속 해제 안내.
- 종료 대화상자: 종료일 기본 오늘, 재직 상태 기본 퇴직. 이미 종료된 소속에는 종료 버튼이 없다.
- 사용 중지 사람은 소속 추가·수정과 이미지 변경을 막는다.
- `?personPk=` 가 있으면 그 사람 Drawer 를 연다.

## 오류

| 코드 | 표시 |
| --- | --- |
| `ASSIGNMENT.DUPLICATE` | 부서 |
| `ASSIGNMENT.EMPLOYEE_NO_DUPLICATE` | 사원번호 |
| `ASSIGNMENT.DEPT_COMPANY_MISMATCH`, `DEPT_INACTIVE` | 부서 |
| `ASSIGNMENT.COMPANY_INACTIVE` | 회사 |
| `ASSIGNMENT.INVALID_DATE`, `RESIGNED_REQUIRES_END` | 종료일 |
| `ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT` | 부서장 |
| `fieldErrors`, `ORG.INVALID_CODE_VALUE` | 해당 칸. 칸이 없으면 알림 |
| `PERSON.HAS_ACTIVE_ASSIGNMENT`, `PERSON.HAS_ACCOUNT`, `ASSIGNMENT.INACTIVE` | 서버 메시지 |
| `FILE.EXTENSION_NOT_ALLOWED`, `MIME_MISMATCH`, `TOO_LARGE`, `NOT_IMAGE` | 서버 메시지 |

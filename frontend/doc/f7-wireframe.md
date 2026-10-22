# F7 회사 화면

기준: `frontend/doc/org-screens-wireframe.md` 1절·2절. 화면 구성은 그 문서와 같다.

## 목록

- 검색: `keyword`(코드·이름·약칭), 회사 유형(`COMPANY_TYPE`), 사용 여부(전체는 파라미터 생략, 사용 `Y`, 중지 `N`).
- 버튼: 조회, 초기화, 회사 등록(`CREATE`).
- 열: 코드, 회사명, 약칭, 유형(코드 이름), 상위 회사(`parentCompanyCode`), 국가(`nationCode`), 정렬, 사용 여부, 작업.
- 정렬: 코드, 회사명, 정렬. 서버 페이징, 기본 20건.
- 사용 중지 행은 흐리게 보이고 작업은 복구(`UPDATE`). 사용 중 행은 수정(`UPDATE`)·삭제(`DELETE`). 권한이 없으면 버튼을 숨긴다.

## Drawer

- 회사 코드: 등록만 입력. 수정은 읽기 전용. `^[A-Z0-9_-]{1,50}$`.
- 회사명: 필수.
- 약칭, 회사 유형(Select), 사업자등록번호(앞뒤 공백 제거, 하이픈 유지), 국가(사용 중 국가), 상위 회사(사용 중 회사 검색, 자기 자신 제외, 선택 해제), 정렬 순서(정수), 이름 번역 표.
- 빈 선택 값은 `null`. 코드 그룹에 값이 없으면 유형 Select 는 비활성이고 안내를 보인다.
- 수정은 상세 API 의 `translations` 를 채운다. 저장은 입력된 언어만 보낸다.
- 명세에 있으나 이 화면에 없는 항목(법인등록번호, 대표자, 주소, 전화, 이메일, 설명)은 입력칸을 만들지 않는다. 수정 저장 때는 상세에 있던 값을 그대로 보내 지우지 않는다.

## 오류

| 코드 | 표시 |
| --- | --- |
| `fieldErrors` | 해당 입력칸 |
| `COMPANY.DUPLICATE` | 회사 코드 칸. 서버 메시지 |
| `COMPANY.DUPLICATE_BUSINESS_NO` | 사업자등록번호 칸. 서버 메시지 |
| `COMPANY.PARENT_CYCLE` | 상위 회사 칸. 서버 메시지 |
| `COMPANY.HAS_CHILD_COMPANY`, `HAS_ACTIVE_DEPT`, `HAS_ACTIVE_ASSIGNMENT`, `LAST_OWN` | 삭제 거절 알림. 서버 메시지 |
| `COMPANY.PARENT_INACTIVE` | 복구 거절 알림. 서버 메시지 |
| 그 밖 | 서버 `errorMessage`. 없으면 로컬 문구 |

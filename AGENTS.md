# AGENTS.md

PMIS SaaS 솔루션. Spring Boot(백엔드) + React(Ant Design Pro, 프런트엔드). 멀티테넌트이며 **테넌트 1건 = DB 스키마 1개**(`t_<코드>`), 공용 스키마는 `pmis`.

## 역할
- 설계: Claude. ERD, 작업 지시서, 검토.
- 개발: Cursor AI. 지시서의 단계 하나만 수행하고 멈춘다.
- 검토·승인: 사용자.

## 문서 위치
- `docs/erd/`: ERD 정본(JSON). 개발 중 수정하지 않는다. 어긋나거나 모호하면 질문 목록에 올린다.
  - `erd-model-tenant`: 공용 스키마(테넌트 관리, 플랫폼 운영자)
  - `erd-model-i18n-common-code`, `erd-model-org-user`, `erd-model-file-team-auth-menu`: MDM 테넌트 스키마
  - `erd-model-pmis-core`: PMIS 1차(프로젝트·범위·일정·원가), 사용자 검토 대기
- `docs/prompts/`: Cursor 지시서. `backend-cursor-handoff-prompts.md`, `frontend-cursor-handoff-prompts.md`
- `docs/reviews/`: 설계 담당의 단계별 검토 기록

## 작업 방식
1. 사용자가 "지시서의 N단계를 수행"하라고 하면 해당 단계만 한다.
2. 끝나면 `doc/cursor-progress.md`에 `[N단계] 제목 - 날짜`, 변경 파일, 검증 결과, 질문/확인 필요, 가정한 사항을 추가한다.
3. 설계 담당이 검토한 뒤 다음 지시문을 쓴다. 다음 단계를 자동으로 진행하지 않는다.
4. 화면은 와이어프레임 수준으로 사용자와 합의한 뒤 구현한다.

## 지켜야 할 규칙
- DB 접속 정보(주소, 계정, 비밀번호), 토큰, 임시 비밀번호를 코드, 문서, 로그, 진행 기록, 채팅에 남기지 않는다. 로컬 값은 gitignore된 `config/application-local.yaml`과 `frontend/.env.local`에만 둔다.
- 사용자가 요청하지 않으면 git commit, branch, push를 하지 않는다.
- 새 라이브러리나 새 환경 전제는 만들기 전에 알린다.
- Docker는 사용할 수 없다. 개발 DB는 공유 원격 PostgreSQL이며 DB 이름은 `pmis`다.
- 통합 테스트는 `it_`/`t_it_` 접두 스키마만 만들고 정리한다. clean, drop, 실제 테넌트 변경 금지.
- 공용 스키마와 테넌트 스키마 사이에 FK를 두지 않는다. 감사 컬럼에는 FK를 두지 않는다.
- DB 공통: PK는 `BIGINT`+시퀀스(`*_pk`), 논리 삭제는 `use_yn='N'`, 부분 유니크는 `use_yn='Y'` 조건 포함.
- API: `/api/mdm/v1`, `/api/platform/v1`. 응답은 `{success,data,errorCode,errorMessage,showType,traceId,fieldErrors}`, 오류 코드는 문자열 `<DOMAIN>.<NAME>`.
- 프런트엔드 개발 서버 실행은 PowerShell에서 `npm.cmd`를 쓴다.

# AGENTS.md

PMIS SaaS 솔루션. Spring Boot(백엔드) + React(Ant Design Pro, 프런트엔드). 멀티테넌트이며 **테넌트 1건 = DB 스키마 1개**(`t_<코드>`), 공용 스키마는 `pmis`.

## 저장소 구조 (모노레포)
- `backend/common/`: MDM 서버(독립 실행). 문서는 `backend/common/doc/`
  - `doc/cursor-handoff-prompts.md`: 백엔드 작업 지시서
  - `doc/cursor-progress.md`: Cursor 작업 결과 기록
  - `doc/erd/`: ERD 정본(JSON). 개발 중 수정하지 않는다
  - `doc/api/`, `doc/auth-design.md`, `doc/multitenancy-design.md` 등 설계 문서
- `frontend/`: Ant Design Pro 기반 프런트엔드. 문서는 `frontend/doc/`
  - `doc/cursor-handoff-prompts.md`: 프런트엔드 작업 지시서(F0~)
  - `doc/cursor-progress.md`: Cursor 작업 결과 기록
- `docs/`: 저장소 공통 문서. `docs/WORKFLOW.md`(작업 흐름), `docs/reviews/`(설계 담당의 단계별 검토 기록)

## 역할
- 설계·검토: Claude. ERD, 지시서, 검토 기록 작성.
- 개발: Cursor AI. 지시서의 단계 하나만 수행하고 멈춘다.
- 승인: 사용자. PR을 확인하고 merge한다.

## 작업 방식 (자세한 흐름은 docs/WORKFLOW.md)
1. 사용자가 "지시서의 N단계를 수행"하라고 하면 해당 단계만 한다. 다음 단계를 자동으로 진행하지 않는다.
2. 작업은 `step/<be|fe>-<단계>` 브랜치(예: `step/be-8.7`, `step/fe-F4.1`)에서 한다. main에 직접 push하지 않는다.
3. 끝나면 해당 영역의 `doc/cursor-progress.md`에 `[N단계] 제목 - 날짜`, 변경 파일, 검증 결과, 질문/확인 필요, 가정한 사항을 추가하고 PR을 만든다.
4. 설계 담당이 PR을 검토하고 `docs/reviews/`에 기록한 뒤 다음 지시문을 쓴다.
5. 화면은 와이어프레임 수준으로 사용자와 합의한 뒤 구현한다.

## 지켜야 할 규칙
- DB 접속 정보(주소, 계정, 비밀번호), 토큰, 임시 비밀번호를 코드, 문서, 로그, 진행 기록, PR, 채팅에 남기지 않는다. 로컬 값은 gitignore된 `backend/common/config/application-local.yaml`과 `frontend/.env.local`에만 둔다. 커밋 전에 이 파일들이 추적되지 않는지 확인한다.
- 사용자가 요청한 브랜치 작업 외의 git 동작(main push, force push, 브랜치 삭제, 이력 재작성)은 하지 않는다.
- 새 라이브러리나 새 환경 전제는 만들기 전에 알린다.
- Docker는 사용할 수 없다. 개발 DB는 공유 원격 PostgreSQL이며 DB 이름은 `pmis`다.
- 통합 테스트는 `it_`/`t_it_` 접두 스키마만 만들고 정리한다. clean, drop, 실제 테넌트 변경 금지.
- 공용 스키마와 테넌트 스키마 사이에 FK를 두지 않는다. 감사 컬럼에는 FK를 두지 않는다.
- DB 공통: PK는 `BIGINT`+시퀀스(`*_pk`), 논리 삭제는 `use_yn='N'`, 부분 유니크는 `use_yn='Y'` 조건 포함.
- API: `/api/mdm/v1`, `/api/platform/v1`. 응답은 `{success,data,errorCode,errorMessage,showType,traceId,fieldErrors}`, 오류 코드는 문자열 `<DOMAIN>.<NAME>`.
- 프런트엔드 개발 서버 실행은 PowerShell에서 `npm.cmd`를 쓴다.

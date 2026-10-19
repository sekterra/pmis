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
- `docs/`: 저장소 공통 문서. `docs/WORKFLOW.md`(작업 흐름), `docs/reports/`(Cursor 보고서), `docs/reviews/`(설계 담당의 단계별 검토 기록)

## 역할
- 설계·검토: Claude. ERD, 지시서, 검토 기록 작성.
- 개발: Cursor AI. 지시서의 단계 하나만 수행하고 멈춘다.
- 검토 확인·승인: 사용자. Cursor에 지시서 수행과 push를 요청하고, 실제 서버 확인을 한다.

## 작업 방식 (자세한 흐름은 docs/WORKFLOW.md)
1. Claude가 지시서를 해당 영역의 `doc/cursor-handoff-prompts.md`(백엔드 `backend/common/doc/`, 프런트엔드 `frontend/doc/`)에 단계 섹션으로 추가한다. 사용자가 "이 파일의 N단계를 수행해줘"라고 하면 그 단계 하나만 하고 멈춘다. 다음 단계를 자동으로 진행하지 않는다. 지시서의 이전 단계 내용은 삭제·수정하지 않는다.
2. 작업은 현재 브랜치 `main`에서 바로 한다. 브랜치 생성, PR, merge는 하지 않는다. 시작 전 `main`이 아니면 `git switch main`으로 전환하고(이전 `step/...` 브랜치는 삭제하지 않는다, 미커밋 변경은 따라온다), `git pull --ff-only`가 되는지, Claude가 쓰는 파일(각 영역의 `doc/cursor-handoff-prompts.md`, `docs/reviews/` 안, `AGENTS.md`, `docs/WORKFLOW.md`, `.gitignore`) 밖에 미커밋 변경이 없는지 확인하고 어긋나면 멈추고 보고한다. 그 Claude 작성 파일의 미커밋 변경은 먼저 `docs: ...` 커밋으로 따로 커밋한다.
3. 끝나면 `[<단계>] 요약` 형식으로 로컬 커밋하고, 해당 영역의 `doc/cursor-progress.md`에 `[N단계] 제목 - 날짜`, 변경 파일, 검증 결과, 질문/확인 필요, 가정한 사항을 추가한다. 그리고 `docs/reports/<단계>.md` 보고서와 `docs/reports/<단계>.diff`(시작 커밋..HEAD, 커밋하지 않음)를 만든다.
4. **push는 사용자가 "push 해줘"라고 요청할 때만** 한다(`git push origin main`만, force 금지).
5. Claude가 보고서·diff를 읽고 검토해 `docs/reviews/`에 기록하고 다음 지시서를 쓴다.
6. 화면은 와이어프레임 수준으로 사용자와 합의한 뒤 구현한다.
7. 지시서 파일 맨 위의 "작업 방식" 절이 같은 규칙을 담고 있다. 옛 단계에 적힌 "커밋·브랜치·푸시 금지", "PR" 문구는 이 규칙이 대체한다.

## 지켜야 할 규칙
- DB 접속 정보(주소, 계정, 비밀번호), 토큰, 임시 비밀번호를 코드, 문서, 로그, 진행 기록, PR, 채팅에 남기지 않는다. 로컬 값은 gitignore된 `backend/common/config/application-local.yaml`과 `frontend/.env.local`에만 둔다. 커밋 전에 이 파일들이 추적되지 않는지 확인한다.
- 사용자가 요청하지 않은 push, force push, 브랜치 생성·삭제, 이력 재작성은 하지 않는다. `*.zip`, `*.diff`, `.env*` 로컬 설정은 커밋하지 않는다.
- 새 라이브러리나 새 환경 전제는 만들기 전에 알린다.
- Docker는 사용할 수 없다. 개발 DB는 공유 원격 PostgreSQL이며 DB 이름은 `pmis`다.
- 통합 테스트는 `it_`/`t_it_` 접두 스키마만 만들고 정리한다. clean, drop, 실제 테넌트 변경 금지.
- 공용 스키마와 테넌트 스키마 사이에 FK를 두지 않는다. 감사 컬럼에는 FK를 두지 않는다.
- DB 공통: PK는 `BIGINT`+시퀀스(`*_pk`), 논리 삭제는 `use_yn='N'`, 부분 유니크는 `use_yn='Y'` 조건 포함.
- API: `/api/mdm/v1`, `/api/platform/v1`. 응답은 `{success,data,errorCode,errorMessage,showType,traceId,fieldErrors}`, 오류 코드는 문자열 `<DOMAIN>.<NAME>`.
- 프런트엔드 개발 서버 실행은 PowerShell에서 `npm.cmd`를 쓴다.

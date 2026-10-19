# 작업 흐름 (로컬 중심, 수동 전달)

```
Claude: 지시서 작성 ─▶ 영역별 doc/cursor-handoff-prompts.md 에 단계 추가
사용자: Cursor에 "이 파일의 N단계를 수행해줘"
Cursor: main에서 작업 ─▶ 로컬 커밋 ─▶ docs/reports/<단계>.md 와 .diff 작성
Claude: 보고서·diff를 읽고 검토 ─▶ docs/reviews/ 기록 ─▶ 통과 또는 수정 지시서
사용자: 원할 때만 Cursor에 "push 해줘"
```

PR, 브랜치, 자동 리뷰(GitHub Actions)는 쓰지 않는다. 사용자가 단계마다 merge 하지 않는다.

## 1. 지시서 (Claude → Cursor)
- 위치: 백엔드 `backend/common/doc/cursor-handoff-prompts.md`, 프런트엔드 `frontend/doc/cursor-handoff-prompts.md`. 단계마다 섹션을 이어서 추가한다. 이전 단계 내용은 지우지 않는다.
- 각 지시서 파일 맨 위에 "작업 방식" 절(main 직접 커밋, push는 요청 시, 보고서 형식)이 있다.
- 사용자는 Cursor에 "이 파일의 N단계를 수행해줘"라고 요청한다.
- `docs/prompts/`는 쓰지 않는다(`be-8.7.md`는 옛 기록).

## 2. 개발 (Cursor)
- 현재 브랜치 `main` 에서 직접 작업한다. 브랜치 생성, PR, push 는 하지 않는다.
- 시작 전: `main` 인지, `git pull --ff-only` 가 되는지, Claude가 쓰는 파일(각 영역의 `doc/cursor-handoff-prompts.md`, `docs/reviews/` 안, `AGENTS.md`, `docs/WORKFLOW.md`, `.gitignore`) 밖의 미커밋 변경이 없는지 확인한다. 어긋나면 멈추고 보고한다. 그 Claude 작성 파일의 미커밋 변경은 `docs: ...` 커밋으로 먼저 따로 커밋한다.
- 단계 하나만 수행하고, 커밋 메시지는 `[<단계>] 요약` 형식이다.
- 끝나면 해당 영역의 `doc/cursor-progress.md` 에 결과를 추가하고, `docs/reports/<단계>.md` 보고서와 `docs/reports/<단계>.diff` (시작 커밋..HEAD, 커밋하지 않음)를 만든다.
- push 는 사용자가 요청할 때만 `git push origin main` 으로 한다. force push 금지.
- 접속 정보, 토큰, 임시 비밀번호는 코드·문서·보고서·커밋 어디에도 넣지 않는다.

## 3. 검토 (Claude)
- 사용자가 폴더를 연결해 둔 상태에서 Claude가 `docs/reports/<단계>.md`, `.diff`, 필요한 소스를 직접 읽는다. 사용자가 붙여 넣을 필요가 없다.
- 지시서 대비 충족 여부, 규칙 위반, 테스트 결과를 확인하고 `docs/reviews/YYYY-MM-DD-<단계>.md` 에 기록한다. 승인, 수정 요청, 사용자 결정 필요로 구분한다.
- 수정이 필요하면 수정 지시서를 새 파일로 쓴다. 승인되면 다음 지시서를 쓴다.
- Claude가 쓴 파일(지시서, `docs/reviews` 등)은 커밋되지 않은 상태로 폴더에 놓이고, Cursor가 다음 작업 시작 시 먼저 커밋한다.

## 4. 사용자가 직접 하는 것
- Cursor에 지시서 수행 요청, push 요청, 실제 서버 확인(각 `*-manual-check.md`), 공유 DB를 바꾸는 작업의 승인, 화면 와이어프레임 합의.

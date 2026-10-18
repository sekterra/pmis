# 작업 흐름 (GitHub 중심)

```
Claude: 지시문 작성 ──PR──▶ main ──pull──▶ Cursor: step 브랜치에서 개발 ──PR──▶ Claude: 검토
   ▲                                                                           │
   └──────────── 승인되면 다음 지시문, 문제가 있으면 수정 지시문 ◀─────────────┘
                              사용자: 각 PR을 확인하고 merge
```

## 1. 지시문 (Claude → Cursor)
- Claude가 `claude/prompt-<단계>` 브랜치에서 해당 영역의 `doc/cursor-handoff-prompts.md`에 단계를 추가하고 PR을 만든다.
- 사용자가 확인 후 merge한다. Cursor는 main을 pull하여 지시서를 읽는다.
- 사용자는 Cursor에 "지시서의 N단계를 수행해줘"라고만 말한다.

## 2. 개발 (Cursor)
- main에서 `step/<be|fe>-<단계>` 브랜치를 만든다.
- 단계 하나만 수행하고, 해당 영역의 `doc/cursor-progress.md`에 결과를 추가한다.
- 커밋 메시지는 `[<단계>] 요약` 형식으로 쓰고 PR을 만든다. PR 본문에는 progress의 해당 항목을 붙인다.
- 접속 정보, 토큰, 임시 비밀번호는 PR 어디에도 넣지 않는다.

## 3. 검토 (Claude)
- Claude가 PR의 diff와 `cursor-progress.md`를 읽고 지시서 대비 충족 여부, 규칙 위반, 테스트 결과를 확인한다.
- 결과는 PR 코멘트와 `docs/reviews/YYYY-MM-DD-<단계>.md`에 남긴다. 승인, 수정 요청, 사용자 결정 필요 항목으로 구분한다.
- 승인되면 사용자가 PR을 merge하고 Claude가 다음 지시문을 쓴다. 수정이 필요하면 수정 지시문을 같은 방식으로 추가한다.

## 4. 사용자가 직접 하는 것
- PR merge, 실제 서버 확인(각 `*-manual-check.md`), 공유 DB를 바꾸는 작업의 승인, 화면 와이어프레임 합의.

## 5. 브랜치 이름
- `main`: 승인된 상태만
- `step/be-<단계>`, `step/fe-<단계>`: Cursor 작업
- `claude/prompt-<단계>`, `claude/review-<단계>`: Claude 지시문·검토 기록

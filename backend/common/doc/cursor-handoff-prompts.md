# Cursor AI 작업 지시서 (backend/common)

이 파일은 Cursor AI가 읽고 수행하는 백엔드 작업 지시서이다.
사용자가 "이 파일의 N단계를 수행해줘"라고 요청하면 **해당 단계 하나만** 수행한다. 다음 단계는 사용자가 요청할 때 진행한다.

> 2026-10-19: 이 파일이 프런트엔드 지시서의 복사본으로 잘못 들어 있던 것을 바로잡았다. 1~8.7단계는 모두 완료·승인되었고 원문은 Claude 프로젝트 문서 `claude/cursor-handoff-prompts.md`에 있다. 결과 기록은 `doc/cursor-progress.md`, 설계는 `doc/auth-design.md`, `doc/multitenancy-design.md`, `doc/api/` 를 본다. 새 단계는 이 파일 아래에 이어서 추가된다.

## 공통 원칙 (모든 단계에 적용)
1. `.cursor/rules`의 규칙을 먼저 읽고 따른다. 특히 `00-project-overview.mdc`의 **개발 환경 제약**(Docker 사용 불가, 원격 개발 DB 공유)을 지킨다.
2. ERD(`doc/erd/*.json`)는 기준 문서이며 수정하지 않는다. ERD와 어긋나거나 모호한 부분은 임의로 결정하지 말고 질문 목록으로 정리한다.
3. 새 라이브러리나 새로운 환경 전제(설치 프로그램, 외부 서비스, 권한)가 필요하면 만들기 전에 사용자에게 먼저 알린다.
4. DB 접속 정보(주소, 계정, 비밀번호), 토큰, 임시 비밀번호를 코드·문서·로그·진행 기록·보고서·채팅에 남기지 않는다. 로컬 값은 gitignore된 `config/application-local.yaml`에만 둔다(`doc/local-setup.md`). 공유 개발 DB는 DB 이름 `pmis`를 확인한 뒤에만 쓴다. 통합 테스트는 `it_`/`t_it_` 접두 스키마만 만들고 정리하며, 실제 테넌트 행과 공용 스키마를 바꾸지 않는다(clean/drop 금지). 공유 DB를 바꾸는 마이그레이션 적용은 지시서가 명시한 단계에서만 한다.
5. 단계가 끝나면 `doc/cursor-progress.md`에 아래 형식으로 결과를 **추가**한다.

```
## [N단계] 단계 제목 - YYYY-MM-DD
- 변경/추가 파일: (목록)
- 검증 결과: (실행한 명령과 결과. 실행하지 못한 검증은 이유와 함께 명시)
- ERD 반영 내역: (해당 단계에서만. 표)
- 질문/확인 필요: (목록. 없으면 "없음")
- 가정한 사항: (목록. 없으면 "없음")
```

## 작업 방식 (2026-10-19 변경. 아래 옛 단계의 "커밋·브랜치·푸시 금지", "PR" 문구보다 우선한다)
1. 현재 브랜치 `main`에서 직접 작업하고 커밋한다. `step/...` 브랜치 생성, PR 생성, merge는 하지 않는다.
2. **push는 사용자가 "push 해줘"라고 요청할 때만** 한다(`git push origin main`만, force push·브랜치 삭제·이력 재작성 금지).
3. **시작 전 git 정리와 확인** (정리가 안 되거나 아래가 어긋나면 작업하지 말고 상황만 보고하고 멈춘다):
   - 현재 브랜치가 `main` 이 아니면(이전 단계의 `step/...` 브랜치가 남아 있는 경우가 많다) 먼저 `git fetch origin` 후 `git switch main` 으로 전환한다. 미커밋 변경은 그대로 따라온다. 전환이 충돌로 거절되면 `git stash push -u`, `git switch main`, 아래 pull, `git stash pop` 순서로 하고, 그래도 안 되면 멈춘다. 이전 `step/...` 브랜치는 삭제하지 않는다.
   - `git pull --ff-only` 성공(원격에 merge 된 이전 PR 이 있으면 여기서 반영된다).
   - `git status` 에서 Claude 작성 파일 밖의 미커밋 변경이 없다. Claude 작성 파일은 이 지시서 파일, `docs/reviews/` 안의 파일, `AGENTS.md`, `docs/WORKFLOW.md`, `.gitignore` 이다. 이 파일들의 미커밋 변경은 고치지 말고 시작 전에 `docs: Claude 지시서·규칙 갱신` 커밋으로 따로 커밋한다. 커밋 전에 `AGENTS.md`, `docs/WORKFLOW.md` 를 읽는다.
   - 시작 커밋 해시를 `git rev-parse --short HEAD` 로 기록한다.
4. **커밋**: 이 단계의 변경을 `[<단계>] 요약` 형식으로 커밋한다(여러 개로 나눠도 된다). 진행 기록과 보고서도 포함한다. `.env.local` 등 로컬 설정, zip, `*.diff` 는 커밋하지 않는다(`git status` 로 확인).
5. **보고서**: `docs/reports/<단계>.md` 를 쓰고(형식은 아래), `git diff <시작 커밋>..HEAD > docs/reports/<단계>.diff` 를 만든다. diff 는 `.gitignore` 의 `*.diff` 대상이라 커밋되지 않는 것이 정상이다. Claude가 이 두 파일을 읽고 검토한다.
6. 지시서 파일은 삭제하거나 수정하지 않는다. `docs/prompts/` 폴더는 쓰지 않는다(`be-8.7.md` 는 옛 기록으로 남김).
7. 보고서 형식:
   - a. 기본 정보: 시작 커밋, 이 단계 커밋 목록(`git log --oneline <시작>..HEAD`), 변경 파일(`git diff --stat <시작>..HEAD`)
   - b. 요구사항 체크리스트: 지시서 항목별 "충족/부분/미충족"과 근거(파일·테스트 이름)
   - c. 핵심 코드: 검토에 필요한 부분
   - d. 테스트·검증 결과: 명령별 결과와 개수
   - e. 가정, 질문, 알려진 한계, 지시에서 벗어난 점(없으면 "없음")
   마지막에 `git status` 로 의도하지 않은 파일이 커밋되거나 남지 않았는지 확인한 결과를 한 줄 쓴다.

---

## 완료된 단계 요약 (원문은 프로젝트 문서 참조, 새 작업의 전제)
| 단계 | 내용 |
| --- | --- |
| 1~1.6 | 개발 환경, 전용 DB `pmis`(ICU ko-KR)와 공용 스키마 `pmis` 스크립트 |
| 2~3.4 | 공용 V1(테넌트 5개 테이블), 테넌트 V1(다국어·공통코드 12개), 통합 테스트 정착(`it_` 접두, currval 금지) |
| 4~4.4 | 멀티테넌시 설계와 인프라(Host+JWT 식별, SCHEMA 멀티테넌시), 공용 V2(플랫폼 운영자), 프로비저닝·일괄 업그레이드 |
| 5~5.6 | 테넌트 V2(회사·부서·사람·사용자 9개), V3(파일·팀·권한·메뉴 10개) |
| 6~6.1 | Seed(`db/seed/tenant`, 멱등), 최초 관리자(임시 비밀번호, BCrypt), ACTIVE 전환, 재발급 |
| 7~7.1 | 공통 API 규약(페이징·정렬·오류), 공통코드 API, 다국어 API, 부분 유니크 V4/V5 |
| 8~8.4 | 인증 설계, 보안 인프라(JWT RS256, JWKS), 테넌트 사용자 인증, 플랫폼 인증·테넌트 관리 API(공용 V3), `menus/my`·공용 V4 |
| 8.5~8.7 | local 프로필, 플랫폼 API 보완·통합 테스트 격리, 메시지 목록 번역 요약 필드 |

---

## 9.1단계: 조직·사람 관리 API (회사, 부서, 사람, 사람 소속)

8.7까지 승인. 사용자 결정(2026-10-19): 백엔드 9단계를 9.1(회사·부서·사람), 9.2(사용자·역할), 9.3(파일·팀)으로 나눈다. 이번 단계는 테넌트 스키마 V2의 `company`, `dept`, `dept_history`, `person`, `person_assignment` 에 대한 관리 API이다. 사용자 계정(`user_account`), 역할·권한, 파일, 팀은 범위 밖이다. 이 단계만 수행하고 멈춘다.

````
기준 문서: doc/erd/erd-model-org-user-*.json(정본, 수정 금지), 테넌트 마이그레이션 V1~V6 (db/migration/tenant, 읽기), doc/api/mdm-api.md(공통 규약: 래퍼, 페이징·정렬, 오류 코드, 권한), doc/auth-design.md, 7단계 공통코드 API와 7.1단계 다국어 API의 코드(같은 패턴을 따른다. 패키지는 기능별로 org(회사·부서)와 person(사람·소속)을 새로 만들거나 기존 패키지 규칙에 맞춘다). ERD와 어긋나거나 모호한 부분은 임의로 정하지 말고 질문 목록에 올린다. DDL(V1~V6)은 원칙적으로 바꾸지 않는다. 꼭 필요하면 구현을 멈추고 질문한다(V7이 필요한 경우 어떤 제약인지와 이유를 적는다).

[결정 사항 - 먼저 doc/api/mdm-api.md 에 "조직·사람" 절 뼈대와 오류 코드 표를 쓰고 구현한다]
1. URL(복수 kebab-case, PK 경로): /api/mdm/v1/companies, /api/mdm/v1/departments, /api/mdm/v1/people, /api/mdm/v1/people/{personPk}/assignments, /api/mdm/v1/person-assignments/{assignmentPk}. 코드값 참조는 요청·응답에서 코드 문자열을 쓰고(예: nationCode, companyCode) 내부 PK 매핑은 서버가 한다. 응답에는 PK와 코드를 모두 준다. 공통 규약(래퍼, 페이징 page/size/sort, 정렬 화이트리스트, 400 COMMON.INVALID_SORT 등)은 7단계 그대로다.
2. 권한(@RequirePermission, 기능 코드는 Seed 의 permission_code 형식을 따른다): 회사 COMPANY, 부서 DEPT, 사람과 사람 소속 PERSON. 조회=READ, 등록=CREATE, 수정=UPDATE, 논리 삭제=DELETE, 복구는 7단계 공통코드 복구와 같은 기능 코드를 쓴다(기존 코드를 읽고 맞춘다). Seed 에 COMPANY, DEPT, PERSON 메뉴와 권한 행이 있는지 확인하고, 없거나 빠진 행이 있으면 db/seed/tenant 에 멱등으로 보강한다(기존 값 덮어쓰기 금지).
3. 코드값 검증: company_type_code(COMPANY_TYPE), dept_type_code(DEPT_TYPE), employment_type_code(EMPLOYMENT_TYPE), assignment_status_code(ASSIGNMENT_STATUS), position_code(POSITION), job_title_code(JOB_TITLE)는 요청에 값이 있으면 해당 공통코드 그룹의 "사용 중이고 유효기간 안인" 코드 값이어야 한다. 아니면 400 ORG.INVALID_CODE_VALUE 와 fieldErrors. POSITION, JOB_TITLE 은 Seed 값이 없고 테넌트가 화면에서 등록하므로 값이 없으면 항상 거절될 수 있다(요청에서 생략하면 된다). 공통코드 조회는 기존 서비스를 재사용하고 규칙을 복제하지 않는다.
4. 다국어 명칭: company_name, dept_name, person_name 의 언어별 표기는 i18n_entity_text(target_table_name, target_column_name, 대상 키)로 관리한다(V1 DDL과 ERD 설명을 읽고 컬럼 규칙을 확인한다). 등록·수정 요청의 translations 배열 [{supportLanguageCode, text}] 로 (대상, 컬럼, 언어) 기준 upsert 하고 요청에 없는 언어는 유지한다(7단계 공통코드 그룹과 같은 방식). 존재하지 않는 언어는 400 I18N.LANGUAGE_NOT_FOUND. 단건 조회 응답에만 translations 를 담고 목록 응답에는 담지 않는다. 이 화면용 번역 조회가 목록에서 필요해지는 경우는 이번 범위가 아니다.
5. 감사 사용자: 등록·수정 시 create_user_pk/update_user_pk(그리고 dept_history.change_user_pk)에 현재 로그인한 테넌트 사용자의 user_pk(JWT sub)를 남긴다. 지금 AuditorAware 가 아직 null 을 돌려주고 있으면 이번 단계에서 SecurityContext 의 검증된 테넌트 사용자 토큰의 sub 를 읽도록 구현한다(토큰이 없거나 플랫폼 토큰이면 null). 이미 구현돼 있으면 그대로 쓴다. 어느 쪽인지 보고서에 적는다.
6. 논리 삭제는 use_yn='N'. 삭제된 행을 다시 쓰려면 복구 API 를 쓴다. 코드 컬럼(company_code, dept_code)은 등록 후 변경 불가이고 DB 의 전체 유니크가 유지된다(삭제된 코드는 재등록 불가, 복구로만 되살림). 이 사실을 문서에 적는다.
7. 날짜는 DATE, "오늘"은 현재 테넌트의 timezone_id 기준(7.1단계에서 만든 한 곳의 처리를 재사용).

[회사 company]
- 목록(페이징): keyword(코드·이름·약칭 부분일치), companyTypeCode, useYn(생략 시 사용 중만. 사용 중지 포함은 useYn=ALL 같은 방식이 아니라 기존 7단계 관례를 따른다), 정렬 허용: companyCode, companyName, sortOrder, companyPk. 단건(번역 포함), 등록, 수정, 논리 삭제, 복구.
- 필드는 ERD(company)를 따른다. nation 은 nationCode 로 받아 nation_pk 로 매핑하고 존재하지 않거나 사용 중지된 국가는 400 I18N.NATION_NOT_FOUND 계열 오류가 아니라 fieldErrors(nationCode) 로 돌려준다(어떤 쪽이 기존 규약과 맞는지 코드를 보고 정하고 문서에 적는다).
- 규칙: company_code 형식은 대문자·숫자·밑줄·하이픈 1~50자(근거를 문서에 쓰고, ERD 설명과 다르면 질문), 중복 409 COMPANY.DUPLICATE. business_registration_no 는 NULL 제외, use_yn='Y' 범위에서 유일(V6, 중복 409 COMPANY.DUPLICATE_BUSINESS_NO; 하이픈 등 형식은 저장 전 숫자만 남기는 정규화는 하지 않고 입력 그대로 저장하되 앞뒤 공백만 제거). 상위 회사(parentCompanyCode): 자기 자신 금지, 순환 금지(400 COMPANY.PARENT_CYCLE), 사용 중인 회사만.
- 논리 삭제 거절(409 COMPANY.IN_USE, 사유별 하위 코드는 detail 이 아니라 errorMessage 로 구분하지 말고 오류 코드를 나눈다: COMPANY.HAS_CHILD_COMPANY, COMPANY.HAS_ACTIVE_DEPT, COMPANY.HAS_ACTIVE_ASSIGNMENT). 복구 시 상위 회사가 사용 중이어야 한다(아니면 409 COMPANY.PARENT_INACTIVE).
- 테넌트 가입 시 만들어진 OWN 회사(프로비저닝)도 같은 규칙으로 수정할 수 있다. 단 company_type_code='OWN' 인 마지막 사용 중 회사를 삭제하는 것은 409 COMPANY.LAST_OWN 으로 막는다.

[부서 dept, dept_history]
- 목록(페이징, 관리 화면용): companyCode 필터(필수 아님), keyword, deptTypeCode, useYn, parentDeptPk, 정렬 허용: deptCode, deptName, levelNo, sortOrder, deptPk.
- 트리 조회 GET /departments/tree?companyCode=(필수)&asOfDate=(선택)&includeClosed=: 페이징 없는 배열(코드성 목록 규약). 항목: deptPk, deptCode, deptName, parentDeptPk, levelNo, deptPath, deptTypeCode, sortOrder, validFromDate, validToDate, useYn. 정렬은 deptPath 기준으로 트리 순서가 되게 한다(같은 부모 안에서는 sortOrder, deptCode). asOfDate 가 있으면 valid_from_date <= asOfDate 이고 (valid_to_date IS NULL 또는 valid_to_date >= asOfDate)인 부서만 준다(폐지된 부서도 기간 안이면 포함. use_yn 은 보지 않는다). 없으면 오늘 기준 사용 중인 부서. 부모 이동 이력은 현재 구조만 반영한다(과거 구조 재현은 이번 범위가 아니며 한계로 문서에 적는다).
- 등록: company, 상위 부서(같은 회사, 사용 중), dept_code(회사 안 유일, 409 DEPT.DUPLICATE), 이름, 유형, 정렬, 유효 시작일(기본 오늘)·종료일(시작일 이하 금지: 종료일>=시작일 검증), 설명, translations. level_no 와 dept_path(예 /1/5/12)는 서버가 계산한다(INSERT 후 PK 로 경로를 채우는 방식. 같은 트랜잭션). dept_history 에 CREATE 행(effective_date=유효 시작일, after_dept_name, after_parent_dept_pk).
- 수정: dept_code, company 는 변경 불가. 이름이 바뀌면 RENAME 이력(before/after_dept_name), 상위 부서가 바뀌면 MOVE 이력(before/after_parent_dept_pk). 이름과 상위가 동시에 바뀌면 이력 행 2개. 이력의 effective_date 는 요청의 effectiveDate(선택, 기본 오늘), change_reason 은 요청의 changeReason(선택). 이동 시 자기 자신·하위 부서 아래로 이동 금지(400 DEPT.PARENT_CYCLE), 다른 회사 부서 아래로 금지(400 DEPT.PARENT_COMPANY_MISMATCH). 이동하면 이동한 부서와 모든 하위의 level_no 와 dept_path 를 같은 트랜잭션에서 갱신한다(하위 수가 많아도 행마다 쿼리를 내지 말고 경로 접두사 기준의 일괄 UPDATE 를 쓴다).
- 논리 삭제(폐지): 활성 하위 부서가 있으면 409 DEPT.HAS_CHILD, 현재 소속(end_date IS NULL, use_yn='Y')이 있으면 409 DEPT.HAS_ACTIVE_ASSIGNMENT. 통과하면 valid_to_date = 요청의 effectiveDate(기본 오늘), use_yn='N', CLOSE 이력. 복구: 상위 부서가 사용 중이어야 하고(409 DEPT.PARENT_INACTIVE), valid_to_date=NULL, use_yn='Y', REOPEN 이력.
- 이력 조회 GET /departments/{deptPk}/history (페이징, 최신순): change_type_code, effective_date, 전후 이름, 전후 상위 부서 PK, 사유, 변경 시각, 변경 사용자 PK. 이력은 수정·삭제 API 가 없다(append-only).
- 부서 이력은 DEPT_CHANGE_TYPE(CREATE/RENAME/MOVE/CLOSE/REOPEN) 공통코드 값을 쓴다. Seed 에 이 그룹이 없으면 멱등으로 보강한다.

[사람 person, 사람 소속 person_assignment]
- 사람 목록(페이징): keyword(이름·이메일·휴대전화·사번 부분일치. 사번은 현재 소속의 employee_no), companyCode, deptPk(현재 소속 기준), employmentTypeCode, assignmentStatusCode(주 소속 기준), useYn, 정렬 허용: personName, email, personPk. 각 항목에 "주 소속 요약"(현재 소속 중 primary_yn='Y' 한 건: companyCode, companyName, deptCode, deptName, positionCode, jobTitleCode, employeeNo, assignmentStatusCode, 없으면 null)을 포함한다. 요약은 페이지에 포함된 사람 PK 목록으로 한 번의 IN 조회(또는 조인)로 만든다. 행마다 쿼리를 내지 않는다(N+1 금지. 8.7단계와 같은 방식으로 쿼리 수가 페이지 크기와 무관함을 검증한다).
- 사람 단건(번역, 현재 소속 목록 포함), 등록, 수정, 논리 삭제, 복구. 필드는 ERD(person)를 따르되 profile_file_pk 는 이번 단계 요청에서 받지 않는다(파일 API 는 9.3. 응답에는 값을 그대로 준다). 이메일은 형식 검증(Bean Validation), 같은 이메일의 중복은 막지 않는다(동명이인·공용 메일 가능, 근거를 문서에 적는다).
- 사람 논리 삭제 거절: 현재 소속이 있으면 409 PERSON.HAS_ACTIVE_ASSIGNMENT, 연결된 사용 중인 user_account 가 있으면 409 PERSON.HAS_ACCOUNT (user_account 는 읽기 전용으로 확인만 하고 이 단계에서 만들거나 바꾸지 않는다). 복구 가능.
- 소속 API: GET /people/{personPk}/assignments (배열, 과거 소속 포함, 기본 정렬 start_date 내림차순; useYn 필터), POST /people/{personPk}/assignments, PUT /person-assignments/{assignmentPk}, POST /person-assignments/{assignmentPk}/end (본문 endDate(기본 오늘), assignmentStatusCode(기본 RESIGNED, 허용값은 코드 검증), changeReason 은 받지 않는다), DELETE /person-assignments/{assignmentPk}(잘못 입력한 소속의 논리 삭제), POST .../restore.
- 소속 규칙: company 필수(사용 중), dept 는 선택이지만 값이 있으면 같은 회사 소속이어야 한다(400 ASSIGNMENT.DEPT_COMPANY_MISMATCH. 사용 중인 부서만). start_date 필수, end_date >= start_date. employee_no 는 같은 회사 안에서 유일(NULL 제외, use_yn='Y', end_date 와 무관한 V2 의 부분 유니크를 확인해 같은 범위로 검사하고 위반은 409 ASSIGNMENT.DUPLICATE_EMPLOYEE_NO). dept_head_yn='Y' 는 dept 가 있을 때만(400). 같은 사람이 같은 회사·같은 부서(둘 다 NULL 포함)에 현재 소속이 둘 생기는 것은 409 ASSIGNMENT.DUPLICATE. 상태 RESIGNED 이면 end_date 가 있어야 한다(요청에 없으면 400).
- 주소속 규칙: 사람당 현재 소속(end_date IS NULL, use_yn='Y') 중 primary_yn='Y' 는 최대 1건(V2 부분 유니크). 사람의 첫 현재 소속은 primaryYn 요청값과 상관없이 'Y'로 한다. 이후 소속을 primaryYn='Y' 로 등록·수정하면 기존 주소속을 같은 트랜잭션에서 'N' 으로 내린다(409 로 거절하지 않는다). 주소속을 end 하면 다른 현재 소속이 남아 있어도 자동 승격하지 않는다(주소속 없음 상태를 허용하고 문서에 적는다). 소속 변경(부서 이동)은 기존 소속을 end 하고 새 소속을 등록하는 흐름으로 표현한다(이동 전용 API 는 만들지 않는다).
- 사번이 다른 회사에서 같아도 된다(UK 는 회사별).

[오류 코드와 문구] 위에서 정의한 모든 오류 코드(ORG.*, COMPANY.*, DEPT.*, PERSON.*, ASSIGNMENT.*)의 ko-KR/en-US 문구를 db/seed/tenant 의 error 네임스페이스 Seed 에 멱등으로 추가한다(기존 값 덮어쓰기 금지). 일괄 업그레이드가 기존 테넌트에도 추가하는 것을 테스트로 확인한다.

[문서] doc/api/mdm-api.md 에 위 엔드포인트의 요청·응답 예시, 필터·정렬 필드, 권한 표, 오류 코드 표, 한계(부서 구조의 과거 재현, 코드 재사용 불가)를 추가한다. FE 연동 시나리오에 "조직도 트리 로딩"과 "사람 등록 → 소속 등록" 순서를 쓴다.

[테스트]
- 단위/슬라이스(MockMvc, DB 없음): 오류 코드 매핑, 검증(날짜 순서, 코드 형식, 이메일), 권한(기본 프로필 403/토큰 없음 401), 정렬 화이트리스트, 트리 정렬 순서와 경로 계산 함수, 이동 시 하위 경로 재계산 함수, 주소속 승격 규칙.
- 통합(@Tag("integration"), 3.2단계 절차: 접속 정보 비밀 보호, 실행 전 DB 이름 pmis 확인, t_it_<랜덤> 스키마만, 테넌트는 프로비저닝 서비스로 ACTIVE, 테스트 전용 토큰 발급 헬퍼 사용): 
  (a) 엔티티 validate (V1~V6)
  (b) 회사: 등록·수정·삭제·복구, 국가 매핑, 사업자번호 부분 유니크(삭제 후 재등록 가능·사용 중 중복 409), 상위 회사 순환 거절, 삭제 거절 4종(하위 회사, 활성 부서, 활성 소속, 마지막 OWN)
  (c) 부서: 트리 생성과 level_no/dept_path, 이동 시 하위 경로 일괄 갱신, 순환·다른 회사 이동 거절, RENAME/MOVE/CLOSE/REOPEN 이력 행(전후 값, effective_date, 사용자 PK), asOfDate 조회에서 폐지된 부서 포함/제외, 삭제 거절 2종, 복구
  (d) 사람: 등록·수정·번역 upsert(요청에 없는 언어 유지), 목록의 주 소속 요약과 쿼리 수가 페이지 크기와 무관함, 삭제 거절 2종(활성 소속, 연결된 계정 — 계정 행은 테스트가 직접 INSERT)
  (e) 소속: 첫 소속의 자동 주소속, 새 주소속 등록 시 기존 주소속 강등, 부서-회사 불일치 거절, 사번 중복(회사별), 중복 소속 거절, RESIGNED 종료일 규칙, end 후 부서 폐지 가능, 삭제·복구
  (f) 권한: READ 만 있는 사용자가 쓰기 API 403, 권한 없는 사용자 403, 두 테넌트 데이터 분리
  (g) 감사 사용자 PK 가 토큰의 sub 로 남는다
  (h) 코드값 검증(유효/무효/기간 밖/사용 중지 코드)
  (i) 일괄 업그레이드가 새 오류 문구·Seed 를 기존 테넌트에 추가하되 바뀐 값은 보존
  테스트가 만든 모든 행과 스키마를 정리하고 pmis 업무 테이블 0행(실제 테넌트 행 불변)을 확인한다. 기존 통합 테스트 전부(현재 개수를 보고서에 적는다)도 통과해야 한다.
- 새 라이브러리는 추가하지 않는다. 공유 DB 에 마이그레이션을 적용하는 일은 이 단계에 없다(테넌트 Seed 는 일괄 업그레이드로 반영되지만, 실제 테넌트에 업그레이드를 실행하지 않는다).
- 질문이 있으면 해당 부분만 멈추고 진행 기록의 질문 목록에 올린다. 진행 기록은 [9.1단계] 형식으로 doc/cursor-progress.md 에 추가하고, 보고서는 파일 앞쪽 "작업 방식"의 형식으로 docs/reports/9.1.md 와 9.1.diff 를 만든다. 커밋 메시지는 [9.1] 요약. push 는 하지 않는다. 완료 후 멈춘다.
````

## 9.2단계: 사용자 계정·역할·권한 관리 API (+ 9.1 작은 보완)

9.1 승인(검토 기록: docs/reviews/2026-10-19-9.1.md). 사용자 결정(2026-10-19): 9.3 은 메뉴 관리, 9.4 는 파일·팀으로 나눈다. 이번 단계는 테넌트 스키마의 `user_account`, `user_password_history`, `role`, `user_role`, `permission`, `role_permission` 에 대한 관리 API 이다. 메뉴 CRUD, 파일, 팀은 범위 밖이다. 이 단계만 수행하고 멈춘다.

````
기준 문서: doc/erd/erd-model-org-user-*.json 의 사용자 영역과 doc/erd/erd-model-file-team-auth-menu-*.json 의 권한 영역(정본, 수정 금지), 테넌트 마이그레이션 V1~V6, doc/auth-design.md(8단계: 토큰, 권한 검사 합집합 규칙, 캐시 TTL 60초, 감사 기록), doc/api/mdm-api.md(공통 규약), 6.1단계 최초 관리자 생성·임시 비밀번호 재발급 코드(TenantAdminWriter 등), 8.2단계 인증 서비스(비밀번호 정책, 이력, refresh 폐기), 8.4단계 DbPermissionChecker 와 캐시, 9.1단계 org 패키지(같은 패턴: 컨트롤러-서비스-저장소, OrgExceptions 식 헬퍼, EntityTextService, CurrentTenantUser, 코드값 검증). ERD 와 어긋나거나 모호한 부분은 임의로 정하지 말고 질문 목록에 올린다. DDL(V1~V6)은 바꾸지 않는다. 꼭 필요하면 구현을 멈추고 질문한다.

[A. 9.1 작은 보완 - 먼저 처리한다. 모두 사용자 눈에 보이는 동작을 바꾸므로 mdm-api.md 에도 반영한다]
1. 부서 수정(PUT /departments/{deptPk}): 상위 부서가 바뀌지 않았으면(요청의 parentDeptCode 가 현재 상위와 같으면) 상위 부서의 사용 여부(DEPT.PARENT_INACTIVE)를 다시 검사하지 않는다. 현재 상위가 폐지된 부서의 이름만 고치는 요청이 막히지 않아야 한다. 상위를 새로 지정하거나 바꿀 때만 기존대로 사용 중·같은 회사·순환을 검사한다. 통합 테스트 1건 추가.
2. 소속 종료(POST /person-assignments/{assignmentPk}/end): 이미 종료된 소속(end_date 가 있음)에 다시 호출하면 409 ASSIGNMENT.ALREADY_ENDED. 논리 삭제된 소속(use_yn='N')도 409 ASSIGNMENT.INACTIVE 로 거절한다(복구 후 종료). ko/en 문구를 시드에 멱등 추가. 통합 테스트 1건 추가.
3. 부서 트리 조립(DeptService.tree): 행마다 전체 목록을 다시 훑는 부분을 deptPk 로 만든 Map 으로 바꾼다(결과 순서·내용은 그대로). 기존 테스트가 그대로 통과해야 한다.

[B. 공통 결정]
1. URL(복수 kebab-case, PK 경로): /api/mdm/v1/users, /api/mdm/v1/users/{userPk}/roles, /api/mdm/v1/roles, /api/mdm/v1/roles/{rolePk}/permissions, /api/mdm/v1/permissions. 사용자 식별은 PK 와 loginId 둘 다 응답에 넣는다. 요청에서 다른 엔티티를 가리킬 때는 코드(roleCode, menuCode, actionCode, permissionCode)나 personPk 를 쓴다.
2. 권한(@RequirePermission): 사용자 API 는 메뉴 코드 USER, 역할·사용자 역할·역할 권한 API 는 ROLE, 권한 목록·등록·삭제는 PERMISSION. 메뉴 코드의 정확한 형태(예: SYSTEM.USER 인지 USER 인지)는 db/seed/tenant/S05_role_menu_permission.sql 의 menu_code/permission_code 를 읽어 그대로 쓰고, 9.1 의 COMPANY/DEPT/PERSON 과 같은 방식으로 확인해 문서에 표로 적는다. 조회=READ, 등록=CREATE, 수정(상태 변경·비밀번호 초기화·역할 부여 포함)=UPDATE, 논리 삭제=DELETE, 복구=UPDATE. 시드에 없는 권한 행이 필요하면 멱등으로 보강하고 질문 목록에 올린다.
3. 목록은 7단계 규약(page/size/sort 화이트리스트, useYn 생략 시 사용 중지 포함). 응답 래퍼·오류 형식·fieldErrors 는 기존과 같다. 날짜의 "오늘"은 TenantClock.
4. 이름의 언어별 표기(role_name, permission_name)는 9.1 의 i18n_entity_text upsert 방식을 쓴다(target_table_name=role/permission). 단건 조회에서만 translations 를 내려준다.
5. 비밀번호 해시, 임시 비밀번호 평문, 토큰은 로그·예외 메시지·진행 기록·보고서·테스트 출력 어디에도 남기지 않는다. 임시 비밀번호가 담기는 응답에는 Cache-Control: no-store 를 붙인다.
6. 권한 캐시(8.4, 키=테넌트 스키마+user_pk, TTL 60초)와 계정 상태 캐시: 아래 변경이 커밋된 뒤(트랜잭션 후) 해당 항목을 비운다. 역할 권한 변경은 그 역할을 가진 모든 사용자의 캐시를, 사용자 역할 변경과 계정 상태 변경은 그 사용자의 캐시를 비운다. 이미 있는 캐시 구현에 무효화 메서드가 없으면 추가한다(새 라이브러리 금지).

[사용자 계정 user_account]
- 목록(페이징): keyword(loginId, 연결된 사람 이름), userStatusCode, accountTypeCode, roleCode(현재 유효한 역할 기준), useYn. 정렬 허용: loginId, userStatusCode, lastLoginDatetime, createDatetime, userPk. 응답 항목: userPk, loginId, accountTypeCode, userStatusCode, personPk, personName(이름만. 사람 번역은 목록에서 제외), lastLoginDatetime, mustChangePasswordYn, roleCodes(현재 유효한 역할 코드 배열), useYn. 역할 코드는 페이지의 사용자들을 IN 쿼리 한 번으로 모은다(N+1 금지, 9.1 사람 목록과 같은 방식으로 테스트).
- 단건: 위 항목 + supportLanguageCode, timezoneId, description, loginFailCount, lockedDatetime, passwordChangedDatetime, passwordExpireDatetime, 감사 컬럼, 역할 배열(roleCode, roleName, validFromDate, validToDate, 현재 유효 여부). password_hash 는 어떤 응답에도 넣지 않는다.
- 등록 POST /users: loginId(필수, 앞뒤 공백 제거 후 소문자로 정규화해 저장, 형식은 영문·숫자·`._@-` 3~100자, 이메일 형태 허용), accountTypeCode(ACCOUNT_TYPE 값, 기본 NORMAL), personPk(선택. 값이 있으면 사용 중인 사람이어야 하고 그 사람에게 사용 중인 다른 계정이 없어야 한다. 시스템 계정은 NULL 가능), supportLanguageCode(선택, 사용 중인 지원 언어), timezoneId(선택, IANA 유효성 검사), description, roleCodes(선택, 초기 역할 배열. 사용 중인 역할만. 유효기간 없음). 서버가 임시 비밀번호를 만들어 BCrypt 해시로 저장하고(6.1/8.2 의 생성기·인코더·정책 재사용) must_change_password_yn='Y', password_expire_datetime 은 SECURITY_POLICY 의 TEMP_PASSWORD_VALID_DAYS 로 계산, user_status_code='ACTIVE', 비밀번호 이력 INITIAL 1건. 응답에 temporaryPassword(평문, 이 응답에서만)와 만료 시각을 준다. 오류: 409 USER.DUPLICATE_LOGIN_ID(논리 삭제된 계정의 아이디도 포함. DB 전체 유니크 유지), 409 USER.PERSON_ALREADY_LINKED, 404 PERSON.NOT_FOUND, 400 COMMON.VALIDATION_FAILED(fieldErrors), 404 ROLE.NOT_FOUND, 400 ORG.INVALID_CODE_VALUE(accountTypeCode).
- 수정 PUT /users/{userPk}: personPk(연결 변경·해제. 규칙은 등록과 같고 자기 자신이 이미 가진 연결은 중복이 아님), supportLanguageCode, timezoneId, description. loginId, accountTypeCode, 상태, 비밀번호는 여기서 바꾸지 않는다. 논리 삭제된 계정 수정은 409 USER.INACTIVE.
- 상태 변경 PUT /users/{userPk}/status {userStatusCode}: ACTIVE, LOCKED, DORMANT 만 허용(WITHDRAWN 은 삭제 API 로만). LOCKED→ACTIVE 는 login_fail_count=0, locked_datetime=NULL 로 되돌린다. ACTIVE→LOCKED 는 locked_datetime=지금(관리자 잠금), LOCKED·DORMANT 로 바뀌면 그 사용자의 유효한 refresh 토큰을 모두 ADMIN 사유로 폐기한다. 같은 상태로의 변경은 409 USER.INVALID_STATUS_TRANSITION. 삭제된(WITHDRAWN) 계정은 409 USER.INACTIVE. 8.2 의 로그인 잠금 로직(자동 잠금 해제 시각 계산)이 관리자 잠금과 충돌하지 않는지 확인하고, 충돌하면(관리자가 건 잠금이 자동으로 풀리는 문제) 질문 목록에 올린다.
- 비밀번호 초기화 POST /users/{userPk}/password/reset: 새 임시 비밀번호 생성·저장, must_change_password_yn='Y', 만료 갱신, login_fail_count=0, locked_datetime=NULL(상태가 LOCKED 면 ACTIVE 로), 이력 RESET(changed_user_pk=관리자), refresh 토큰 전부 ADMIN 폐기, 계정 상태 캐시 무효화. 응답은 등록과 같은 임시 비밀번호 형태. 외부 인증 전용 계정(password_hash NULL)도 초기화로 비밀번호를 갖게 되는 것은 허용하지 않고 409 USER.NO_PASSWORD_ACCOUNT 로 거절한다(해당하는 계정이 없으면 이 오류 테스트는 단위로).
- 논리 삭제 DELETE /users/{userPk}: use_yn='N', user_status_code='WITHDRAWN', refresh 전부 ADMIN 폐기, 캐시 무효화. 복구 POST /users/{userPk}/restore: use_yn='Y', 상태 ACTIVE(login_fail_count=0), must_change_password_yn='Y' 로 해 첫 로그인에서 비밀번호를 바꾸게 한다. 복구 때 person 연결이 다른 사용 중 계정과 겹치면 409 USER.PERSON_ALREADY_LINKED.
- 자기 보호: 로그인한 본인에 대한 상태 변경(LOCKED·DORMANT), 삭제, 본인 역할 변경은 403 이 아니라 409 USER.SELF_OPERATION_FORBIDDEN. 비밀번호 초기화는 본인에게도 허용하지 않는다(본인은 비밀번호 변경 API 를 쓴다).
- 마지막 관리자 보호: TENANT_ADMIN 역할을 현재 유효하게 가진 사용 중·ACTIVE 계정이 이 사용자 하나뿐이면 그 사용자의 삭제, LOCKED·DORMANT 전환, TENANT_ADMIN 역할 제거를 409 USER.LAST_ADMIN 으로 거절한다.
- 사람 삭제(9.1)의 PERSON.HAS_ACCOUNT 규칙과 이번 USER.PERSON_ALREADY_LINKED 가 서로 일관되는지 테스트로 확인한다.

[사용자 역할 user_role]
- GET /users/{userPk}/roles: 부여 이력 전체(과거·미래·회수 포함. useYn 필터) 배열. 항목: userRolePk, roleCode, roleName, validFromDate, validToDate, 현재 유효 여부, useYn.
- PUT /users/{userPk}/roles: 요청 본문 [{roleCode, validFromDate?, validToDate?}] 로 "현재 부여 상태"를 통째로 교체한다(요청에 있는 역할은 부여·기간 수정, 요청에 없는 사용 중 부여는 use_yn='N' 으로 회수, 회수했던 행을 다시 부여하면 같은 행을 use_yn='Y' 로 되살린다. V5 의 (user_pk, role_pk) 유니크 때문에 새 행을 만들면 안 된다). 같은 roleCode 가 두 번 있으면 400. valid_from_date <= valid_to_date(위반 400 ROLE.INVALID_DATE). 사용 중이 아닌 역할은 404 ROLE.NOT_FOUND 가 아니라 400 ROLE.INACTIVE(fieldErrors). 응답은 교체 후의 GET 결과. 본인 역할 변경은 USER.SELF_OPERATION_FORBIDDEN, 마지막 관리자 보호는 USER.LAST_ADMIN. 변경 뒤 그 사용자의 권한 캐시 무효화.
- 감사: 별도 이력 테이블이 없으므로 애플리케이션 로그에 사용자 PK, 역할 코드, 변경 종류(grant/revoke/period)만 남긴다(auth-design.md 의 로그 규칙).

[역할 role]
- 목록(페이징): keyword(roleCode, roleName), useYn. 정렬 허용: roleCode, roleName, sortOrder, rolePk. 항목에 현재 유효한 부여 사용자 수(userCount)를 한 번의 집계로 포함한다.
- 단건(번역 포함), 등록, 수정, 논리 삭제, 복구. roleCode 형식 `^[A-Z][A-Z0-9_]{1,49}$`, 등록 후 변경 불가, 중복 409 ROLE.DUPLICATE(논리 삭제된 코드 포함, DB 전체 유니크 유지). 수정 대상: roleName, sortOrder, description, translations. 보호 역할: TENANT_ADMIN 은 삭제 불가 409 ROLE.PROTECTED (이름·설명 수정은 허용). 삭제 거절: 현재 부여(use_yn='Y', 기간 만료 여부와 무관)가 하나라도 있으면 409 ROLE.IN_USE. 복구는 UPDATE 권한.
- 역할 권한 GET /roles/{rolePk}/permissions: 사용 중인 모든 권한을 (메뉴 sortOrder, 권한 sortOrder) 순으로 내려주고 항목마다 granted(boolean)를 붙인다. 항목: permissionPk, permissionCode, menuCode, menuName(요청 언어, 8.4 의 메뉴 이름 해석 재사용), actionCode, permissionName, granted. 필터 granted=true 시 부여된 것만. 페이징 없는 배열(권한 매트릭스 화면용).
- PUT /roles/{rolePk}/permissions {permissionCodes:[...]}: 이 역할의 부여 상태를 통째로 교체한다(없던 것은 부여, 요청에 없는 것은 use_yn='N' 회수, 회수했던 행은 되살리기. V5 유니크). 규칙: 존재하지 않거나 사용 중이 아닌 permissionCode 는 404 PERMISSION.NOT_FOUND(fieldErrors 에 어떤 코드인지), READ 가 아닌 기능을 주려면 같은 메뉴의 READ 도 요청에 있어야 한다(400 ROLE.READ_REQUIRED, fieldErrors 에 메뉴 코드. 8단계 PermissionRules 재사용). 보호 역할 TENANT_ADMIN 의 권한 교체는 409 ROLE.PROTECTED. 변경 뒤 그 역할을 가진 모든 사용자의 권한 캐시 무효화. 응답은 교체 후의 GET 결과.

[권한 permission]
- GET /permissions: 목록(페이징), 필터 menuCode, actionCode, keyword, useYn. 정렬 허용: permissionCode, menuCode, actionCode, sortOrder, permissionPk.
- POST /permissions {menuCode, actionCode, permissionName, sortOrder?, description?, translations?}: 새 기능 권한 추가(예: 승인 APPROVE). 메뉴는 사용 중인 PAGE 메뉴여야 한다(GROUP 이면 400 PERMISSION.MENU_NOT_PAGE), actionCode 는 PERMISSION_ACTION 공통코드의 사용 중 값, permission_code 는 `메뉴코드:기능코드` 로 서버가 만든다(요청에서 받지 않는다), (menu, action) 중복 409 PERMISSION.DUPLICATE.
- PUT /permissions/{permissionPk}: permissionName, sortOrder, description, translations 만 수정. menu, action, permissionCode 는 변경 불가.
- DELETE /permissions/{permissionPk}(논리 삭제): READ 권한은 삭제 불가 409 PERMISSION.PROTECTED. 그 외 권한은 삭제하면서 그 권한이 부여된 role_permission 행도 use_yn='N' 으로 회수하고 모든 권한 캐시를 무효화한다(영향받은 역할 수는 응답이 아니라 로그에만). 복구는 사용 중인 PAGE 메뉴일 때만, 복구해도 역할 부여는 되살리지 않는다.
- 권한 API 는 메뉴 자체를 만들거나 바꾸지 않는다(9.3).

[오류 코드와 문구] 위에서 정의한 모든 오류 코드(USER.*, ROLE.*, PERMISSION.*, ASSIGNMENT.ALREADY_ENDED/INACTIVE)의 ko-KR/en-US 문구를 db/seed/tenant 의 error 네임스페이스 Seed 에 멱등(INSERT … WHERE NOT EXISTS, 테넌트가 고친 문구 보존)으로 추가한다. 이미 있는 코드(AUTH.*, ORG.INVALID_CODE_VALUE 등)는 중복하지 않는다.

[문서] doc/api/mdm-api.md 에 위 엔드포인트의 요청·응답 예시(임시 비밀번호는 예시 값으로만), 필터·정렬 필드, 권한 표(메뉴 코드 포함), 오류 코드 표, 캐시 무효화 시점(최대 60초 지연이 아니라 커밋 직후 비워짐)과 한계(역할 변경 이력 테이블 없음, 로그로만)를 쓴다. doc/auth-design.md 의 "역할, 권한 변경 API 가 생기면 캐시를 비운다" 부분과 "8.2 이후에 구현할 보완" 중 이번에 해결된 항목이 있으면 갱신한다.

[테스트]
- 단위/슬라이스(MockMvc, DB 없음): 오류 코드 매핑, 검증(loginId 형식·정규화, 날짜 순서, roleCode 형식, 중복 roleCode 요청), 권한(기본 프로필 403/토큰 없음 401), 정렬 화이트리스트, 임시 비밀번호가 담긴 응답의 Cache-Control: no-store, 임시 비밀번호/해시가 로그·예외·toString 에 없음, 역할 목록·사용자 목록의 IN 쿼리(N+1 없음).
- 통합(@Tag("integration"), 9.1 과 같은 절차: 접속 정보 비밀 보호, 실행 전 DB 이름 pmis 확인, t_it_<랜덤> 스키마만, 프로비저닝 서비스로 ACTIVE 테넌트, 테스트 JWT 헬퍼로 사용자별 토큰, 끝나면 정리):
  (a) 엔티티 validate (V1~V6)
  (b) 9.1 보완 3건 (부서 이름만 수정하는 요청이 폐지된 현재 상위 때문에 막히지 않음, 상위를 폐지 부서로 바꾸면 거절, 소속 종료 중복 거절)
  (c) 사용자 등록: 임시 비밀번호 BCrypt 일치(응답 값으로 실제 로그인 성공 → pwd_change_required), 이력 INITIAL, 만료일, loginId 소문자 정규화와 대소문자만 다른 중복 409, 논리 삭제된 loginId 재사용 409, 사람 연결 중복 409, 초기 역할 부여
  (d) 상태 변경: LOCKED 시 로그인 거절(401 AUTH.INVALID_CREDENTIALS 계열. 8.2 규칙 확인)과 refresh 폐기(ADMIN), ACTIVE 복구 시 실패 횟수 초기화, 같은 상태 409
  (e) 비밀번호 초기화: 새 임시 비밀번호로 로그인, 이전 비밀번호 거절, 이력 RESET, 잠금 해제, 기존 refresh 폐기. 본인 초기화 거절
  (f) 삭제·복구: WITHDRAWN, 로그인 거절, 복구 후 must_change_password
  (g) 자기 보호·마지막 관리자 보호(삭제, LOCKED, TENANT_ADMIN 제거 각각)
  (h) 사용자 역할 교체: 부여·기간 수정·회수·재부여(같은 행 되살림, 행 수가 늘지 않음), 중복 roleCode 400, 날짜 순서, 사용 중지 역할 거절
  (i) 역할: 등록·수정·번역 upsert·삭제·복구, 코드 형식·중복, TENANT_ADMIN 삭제·권한 교체 거절, 부여 중인 역할 삭제 거절, userCount 집계
  (j) 역할 권한: GET 의 granted 매트릭스, 교체(부여·회수·재부여), READ 없는 쓰기 권한 거절, 없는 permissionCode 404
  (k) 권한: 추가(APPROVE 등), GROUP 메뉴 거절, 중복, 수정 불가 필드, READ 삭제 거절, 삭제 시 role_permission 회수
  (l) 권한 효과와 캐시: 역할에서 권한을 빼면 그 역할 사용자의 다음 요청이 바로 403(60초를 기다리지 않음), 역할을 부여하면 바로 200, 권한 삭제 직후 반영
  (m) 권한 없는 사용자 403, 두 테넌트 데이터 분리(같은 loginId·roleCode 가 서로 간섭하지 않음)
  (n) 일괄 업그레이드가 새 오류 문구를 기존 테넌트에 추가하되 바뀐 값은 보존
  테스트가 만든 모든 행과 스키마를 정리하고 pmis 업무 테이블 0행(실제 테넌트 행 불변)을 확인한다. 기존 통합 테스트 전부(현재 36개, 보고서에 개수 기록)가 계속 통과해야 한다.
- 새 라이브러리는 추가하지 않는다. 공유 DB 에 마이그레이션을 적용하는 일은 이 단계에 없다.
- 질문이 있으면 해당 부분만 멈추고 진행 기록의 질문 목록에 올린다. 진행 기록은 [9.2단계] 형식으로 doc/cursor-progress.md 에 추가하고, 보고서는 파일 앞의 "작업 방식" 형식(docs/reports/9.2.md, 9.2.diff)으로 쓴다. 커밋은 `[9.2] 요약` 한 개, push 는 하지 않는다.
````

## 9.2.1단계: 접속 주소 컨텍스트 조회 (단일 로그인 화면 준비)

9.2 완료 보고 후 검토 전에 사용자가 요청한 변경이다(2026-10-19). 사용자 결정: 프런트를 한 앱·한 로그인 화면으로 합치고, 접속 주소(Host)로 플랫폼 콘솔인지 테넌트인지 구분한다. 이를 위해 프런트가 로그인 전에 "지금 이 주소가 플랫폼 콘솔인가, 어느 테넌트인가"를 물을 수 있는 공개 엔드포인트가 필요하다. 인증 구조(토큰 typ/aud, 쿠키 이름·Path, 플랫폼/테넌트 로그인 엔드포인트)는 바꾸지 않는다. 이 단계만 수행하고 멈춘다(9.2 검토는 이 단계 뒤에 한다).

````
기준 문서: doc/auth-design.md(테넌트 식별과 필터, 공개 경로), 8.1 TenantResolutionFilter/TenantRequestIdentifier, 8.3 플랫폼 인증, doc/api/mdm-api.md, 공용 스키마 tenant/tenant_domain. 모호하면 질문 목록에 올린다. 새 라이브러리 금지, DDL 변경 없음.

1. 엔드포인트: GET /api/mdm/v1/context (인증 불필요, 공개). 응답(ApiResponse 래퍼):
   - 플랫폼 콘솔 주소: { "kind": "PLATFORM" }
   - 테넌트 주소: { "kind": "TENANT", "tenantCode": "...", "tenantName": "..." }
   - 어느 쪽도 아니거나 테넌트가 사용 가능 상태가 아니면 기존 테넌트 식별 실패와 같은 오류(TENANT.NOT_FOUND 계열. 8.1/8.3 에서 쓰는 코드·HTTP 상태를 그대로 재사용하고 새 코드를 만들지 않는다). 테넌트가 ACTIVE 가 아닌 경우의 응답은 로그인 시도와 같은 규칙을 따른다(다르면 질문).
   - 이 외의 정보(도메인 목록, 상태 상세, 스키마 이름 등)는 응답에 넣지 않는다. tenantName 은 테넌트 기본 이름 하나뿐이다.
2. 판정 순서:
   ① dev 프로필이고 X-Tenant-Code 헤더가 있으면 그 테넌트(기존 규칙 그대로. 헤더가 가리키는 테넌트가 없으면 TENANT.NOT_FOUND).
   ② 요청 Host 의 호스트명(포트 제외, 소문자)이 설정 `mdm.platform.hosts`(쉼표 목록, 환경변수 MDM_PLATFORM_HOSTS)에 있으면 PLATFORM.
   ③ 그 외에는 기존 Host → tenant_domain 조회로 테넌트를 찾는다.
   설정 기본값: 비어 있음. dev 프로필 기본(application-dev 또는 기존 dev 설정 위치)은 `localhost,127.0.0.1` 로 둔다. 즉 로컬 개발에서 헤더가 없으면 플랫폼, 헤더가 있으면 테넌트다. dev 가 아닌데 목록이 비어 있어도 기동은 막지 않는다(플랫폼 콘솔을 안 쓰는 배포 가능). 호스트 값 형식 검증(공백·대문자·포트 포함 거절 또는 정규화)을 한다.
3. 필터·보안: 이 경로는 TenantResolutionFilter 의 테넌트 필수 처리에서 제외한다(플랫폼 주소에서는 테넌트가 없다). 공개 경로 표와 SecurityConfig 허용 목록에 추가한다. 응답에 Cache-Control: no-store. 다른 /api/mdm/** 의 동작은 바뀌지 않는다.
4. 플랫폼 API 의 호스트 제한(운영 보강): `mdm.platform.hosts` 가 비어 있지 않으면 `/api/platform/**` 요청의 Host 가 그 목록에 없을 때 거절한다(dev 프로필의 localhost 계열은 목록에 있으므로 영향 없음). 거절 응답은 존재를 드러내지 않도록 404(기존 not-found 형식). 목록이 비어 있으면 지금처럼 제한하지 않는다. 이 제한이 8.3 플랫폼 인증 테스트와 부트스트랩에 영향이 없는지 확인하고, 영향이 있으면 구현을 멈추고 질문한다.
5. 테스트:
   - 단위/슬라이스: 판정 순서 3단계(헤더, 플랫폼 호스트, 테넌트 Host), Host 정규화(대문자, 포트), 알 수 없는 Host 오류, 응답에 비공개 필드가 없음, no-store, 인증 없이 200.
   - 통합(@Tag("integration"), 9.1/9.2 와 같은 절차: pmis 확인, t_it_ 스키마, 프로비저닝 서비스로 ACTIVE 테넌트): 테넌트 도메인 Host 로 TENANT + 코드·이름 일치, 플랫폼 호스트 설정 시 PLATFORM, 플랫폼 호스트가 설정된 상태에서 목록에 없는 Host 의 /api/platform/** 는 404, 목록 안의 Host 는 기존처럼 로그인 가능(기존 플랫폼 통합 테스트 유지). 기존 통합 테스트 전부 통과, 개수를 보고서에 적는다.
6. 문서: doc/api/mdm-api.md 에 context 절(요청·응답·오류·판정 순서·설정 키), doc/auth-design.md 공개 경로 표와 "테넌트 식별" 절, doc/local-setup.md 에 MDM_PLATFORM_HOSTS 와 개발 시 헤더 유무로 플랫폼/테넌트가 갈린다는 설명을 추가한다.
7. 진행 기록은 [9.2.1단계] 형식으로 doc/cursor-progress.md 에 추가하고, 보고서는 "작업 방식" 형식(docs/reports/9.2.1.md, 9.2.1.diff)으로 쓴다. 커밋은 `[9.2.1] 요약` 한 개, push 는 하지 않는다.
````

## 9.3단계: 메뉴 관리 API (+ 9.2 관리자 잠금 보완)

9.2·9.2.1 승인(검토 기록: docs/reviews/2026-10-19-9.2.md, 9.2.1.md). 이번 단계는 테넌트 스키마 `menu`(와 메뉴가 만드는 `permission`)에 대한 관리 API 이다. 파일·팀은 9.4 로 미룬다. 이 단계만 수행하고 멈춘다.

````
기준 문서: doc/erd/erd-model-file-team-auth-menu-*.json 의 메뉴·권한 영역(정본, 수정 금지), 테넌트 마이그레이션 V1~V6, db/seed/tenant/S05_role_menu_permission.sql(시드된 메뉴 코드·경로 규칙), doc/auth-design.md, doc/api/mdm-api.md(menus/my 절 포함), 8.4 menu 패키지(MyMenuService, 메뉴 이름 해석 규칙), 9.2 access 패키지(PermissionAdminService, 캐시 무효화 AccessSessions·DbPermissionChecker), 7.1 다국어 메시지 관리 코드. ERD 와 어긋나거나 모호한 부분은 임의로 정하지 말고 질문 목록에 올린다. DDL 은 바꾸지 않는다.

[A. 9.2 보완 - 먼저 처리한다]
관리자 잠금이 자동으로 풀리는 문제를 고친다. 지금은 PUT /users/{userPk}/status 로 LOCKED 를 걸면 locked_datetime=지금 이라 LOCK_MINUTES 뒤 맞는 비밀번호 로그인에서 LoginLockRules 가 잠금을 푼다.
1. 관리자 잠금(status API 로 LOCKED)은 locked_datetime=NULL 로 둔다. 자동 해제 규칙(LoginLockRules)은 locked_datetime 이 있는 잠금(로그인 실패 초과로 걸린 자동 잠금)에만 적용하고, user_status_code='LOCKED' 이면서 locked_datetime 이 NULL 이면 로그인을 계속 거절한다(실패 이력은 FAIL_LOCKED). 8.2 의 로그인 코드를 읽고 가장 작은 변경으로 한다.
2. 관리자가 ACTIVE 로 되돌리면 지금처럼 login_fail_count=0, locked_datetime=NULL. 비밀번호 초기화(RESET)도 지금처럼 잠금을 푼다.
3. 사용자 단건 응답의 lockedDatetime 은 그대로 두고, 필요하면 lockType(AUTO | ADMIN | null) 같은 파생 필드를 추가해 화면이 구분할 수 있게 한다(추가 여부와 이름은 mdm-api.md 에 결정을 적는다).
4. 통합 테스트: 관리자 잠금 후 LOCK_MINUTES 가 지난 상황(locked_datetime 을 과거로 만들 필요 없이 NULL 이므로, 정책 값을 짧게 바꾸거나 시계를 쓰는 기존 테스트 방식 재사용)에서 올바른 비밀번호도 계속 거절, 자동 잠금은 기존대로 시간 경과 후 해제, 관리자 ACTIVE 복구 후 로그인 성공. 기존 8.2 잠금 테스트 전부 유지.

[B. 공통 결정]
1. URL: /api/mdm/v1/menus (관리 목록·단건·등록·수정·삭제·복구), /api/mdm/v1/menus/tree (관리용 전체 트리), /api/mdm/v1/menus/sort-orders (정렬 일괄 수정). 기존 GET /api/mdm/v1/menus/my 는 그대로이고 경로 충돌이 없게 한다(/menus/my 가 /menus/{menuPk} 로 해석되지 않도록 PK 변수는 숫자만 받는다).
2. 권한(@RequirePermission): 메뉴 코드 MENU (S05 의 실제 menu_code/permission_code 를 읽어 그대로 쓰고 표로 문서화). 조회 READ, 등록 CREATE, 수정·정렬·복구 UPDATE, 삭제 DELETE. 9.2 와 같은 방식.
3. 목록은 7단계 규약(page/size/sort 화이트리스트, useYn 생략 시 사용 중지 포함). 응답 래퍼·오류 형식·fieldErrors·감사 사용자는 기존과 같다.
4. 메뉴 이름의 언어별 표기: 8.4 에서 메뉴 이름은 네임스페이스 menu 의 message_key = menu_code 로 읽고, 없으면 menu.menu_name 을 쓴다. 이 읽기 경로와 쓰기 경로를 맞춘다. translations 요청은 i18n 메시지(네임스페이스 menu, 키=menu_code, 유형 MENU)에 언어별로 upsert 한다(7.1 메시지 관리 서비스를 재사용, 요청에 없는 언어는 유지, 단건 조회에서만 translations 반환). ERD 설명의 i18n_entity_text 방식과 달라지므로 이 결정과 이유를 문서와 질문 목록에 쓴다. 메시지 키 중복·네임스페이스 없음 같은 문제는 기존 I18N.* 오류를 따른다.
5. 변경 뒤 캐시: 메뉴가 바뀌면(등록 제외) 권한 캐시와 필요한 메뉴 이름 캐시를 커밋 후 비운다(9.2 방식). menus/my 가 변경 직후 반영되어야 한다.

[메뉴 menu 규칙]
- 목록(페이징): keyword(menuCode, menuName), solutionCode, menuTypeCode, parentMenuCode, visibleYn, useYn. 정렬 허용: menuCode, menuName, sortOrder, levelNo, menuPk. 항목: menuPk, parentMenuPk/Code, solutionCode, menuCode, menuName(기본 이름), menuTypeCode, routePath, iconName, levelNo, menuPath, sortOrder, visibleYn, useYn, 감사 컬럼.
- 트리 GET /menus/tree?solutionCode=&includeInactive=: 페이징 없는 배열(sortOrder, menuCode 순 선행 순회). 항목에 depth(levelNo), 자식 수, 연결된 권한 수(permissionCount)를 넣는다. 한 번의 조회로 조립한다(N+1 금지).
- 단건: 위 항목 + translations + 권한 목록 요약(permissionCode, actionCode, useYn).
- 등록 POST: solutionCode(필수, 형식 `^[A-Z][A-Z0-9_]{1,49}$`; i18n_namespace 의 solution_code 체계와 같은지 확인하고 존재 검증이 필요하면 질문), menuCode(필수, 등록 후 변경 불가, 전체 유니크 유지 → 409 MENU.DUPLICATE, 논리 삭제된 코드도 포함. 형식은 S05 의 기존 코드가 모두 통과하는 규칙으로 정하고 문서화), menuName, menuTypeCode(MENU_TYPE: GROUP/PAGE), parentMenuCode(선택. 값이 있으면 사용 중인 GROUP 메뉴여야 한다 → 400 MENU.PARENT_NOT_GROUP, 같은 솔루션이 아니어도 되는지는 ERD 설명을 읽고 정하되 모호하면 질문), routePath(PAGE 는 필수, `/` 로 시작하고 `//`·`..`·`\`·`://`·공백 금지, 사용 중인 다른 PAGE 와 중복이면 409 MENU.DUPLICATE_ROUTE; GROUP 은 null 가능), iconName(선택, antd 아이콘 이름 형식 `^[A-Za-z][A-Za-z0-9]*$`), sortOrder, visibleYn(기본 Y), description, translations. levelNo 와 menuPath 는 서버가 계산한다: 기존 시드의 menuPath 규칙(코드 경로 `/SYSTEM/I18N` 형태)을 따르고, ERD 설명의 PK 경로 예시와 다르면 그 차이를 질문 목록에 올린다. 최대 깊이 4(초과 시 400 MENU.TOO_DEEP).
- PAGE 를 등록하면 기본 기능 권한 5개(READ, CREATE, UPDATE, DELETE, EXPORT)를 같은 트랜잭션에서 만든다(permission_code = `메뉴코드:기능코드`, 이름과 정렬은 S05 가 기존 PAGE 메뉴에 만든 방식과 같게). 이미 같은 (menu, action) 행이 있으면(논리 삭제 포함) 되살리지 않고 건너뛰고 로그만 남긴다. GROUP 에는 권한을 만들지 않는다.
- 수정 PUT: menuName, iconName, sortOrder, visibleYn, description, translations, routePath(PAGE), parentMenuCode(이동). menuCode, solutionCode, menuTypeCode 는 변경 불가(타입 변경 시도는 400 COMMON.VALIDATION_FAILED fieldErrors). 이동 시: 새 상위는 사용 중 GROUP, 자기 자신·자손 아래로 이동 금지(409 MENU.PARENT_CYCLE), levelNo·menuPath 를 자손까지 한 문장으로 갱신하고(9.1 부서 이동과 같은 방식), 깊이 제한을 넘으면 400 MENU.TOO_DEEP. 상위가 바뀌지 않으면 상위의 사용 여부를 다시 검사하지 않는다(9.2 A절의 부서 수정과 같은 원칙).
- 정렬 일괄 수정 PUT /menus/sort-orders: [{menuCode, sortOrder}] 최대 200건, 한 트랜잭션, 없는 코드는 404 MENU.NOT_FOUND(전체 롤백).
- 논리 삭제 DELETE: 사용 중인 하위 메뉴가 있으면 409 MENU.HAS_CHILD. 시드 메뉴 보호: S05 가 만든 메뉴(시드 menu_code 목록을 코드 상수로 두지 말고 S05 파일/시드 정의에서 읽을 수 있으면 그것을, 어려우면 solutionCode='MDM' 인 메뉴를 보호 대상으로 하되 근거를 문서에 쓴다)는 삭제 불가 409 MENU.PROTECTED(이름·정렬·표시·번역 수정은 허용). 삭제 가능한 PAGE 를 삭제하면 그 메뉴의 permission 과 role_permission 행을 use_yn='N' 으로 회수한다(9.2 권한 삭제와 같은 방식). 복구 POST /menus/{menuPk}/restore: 상위가 사용 중이어야 하고(409 MENU.PARENT_INACTIVE), 권한 행은 되살리지 않는다(역할 화면에서 다시 부여).
- 보호 메뉴 판단이 모호하면 구현 전에 질문 목록에 올리고 가장 안전한 쪽(삭제 거절)으로 한다.

[오류 코드와 문구] 위에서 정의한 MENU.* 오류 코드의 ko-KR/en-US 문구를 db/seed/tenant 의 error 네임스페이스 Seed 에 멱등(INSERT … WHERE NOT EXISTS, 고친 문구 보존)으로 추가한다.

[문서] doc/api/mdm-api.md 에 위 엔드포인트의 요청·응답 예시, 필터·정렬 필드, 권한 표, 오류 코드 표, 메뉴 이름 번역 방식과 menus/my 반영 시점, 한계(프런트 화면 컴포넌트는 코드로 매핑되므로 새 PAGE 메뉴는 화면이 생기기 전까지 이동해도 404 화면이다 - 프런트 menuRoutes.ts 매핑 필요)를 쓴다. doc/auth-design.md 의 관리자 잠금 설명과 로그인 잠금 규칙을 A절에 맞게 갱신한다.

[테스트]
- 단위/슬라이스: 오류 코드 매핑, 검증(menuCode·routePath·iconName 형식, 깊이), 권한 401/403, 정렬 화이트리스트, /menus/my 와 /menus/{menuPk} 경로 충돌 없음, 트리 조립(순서, 자식 수, 고아 없음).
- 통합(@Tag("integration"), 9.1/9.2 와 같은 절차: pmis 확인, t_it_ 스키마, 프로비저닝 서비스로 ACTIVE 테넌트, 테스트 JWT, 끝나면 정리):
  (a) 엔티티 validate (V1~V6)
  (b) A절 3종(관리자 잠금 유지, 자동 잠금 해제 유지, 관리자 ACTIVE 복구)
  (c) 메뉴 등록: GROUP/PAGE, 레벨·경로 계산, PAGE 기본 권한 5개 생성, 중복 코드·중복 경로·GROUP 아닌 상위·깊이 초과 거절
  (d) 수정과 이동: 자손 경로·레벨 일괄 갱신, 순환 거절, 상위 폐지 상태에서 이름만 수정 가능, 타입·코드 변경 거절
  (e) 정렬 일괄 수정과 롤백
  (f) 삭제·복구: 하위 있음 거절, 시드(보호) 메뉴 거절, PAGE 삭제 시 permission·role_permission 회수와 해당 역할 사용자의 즉시 403, 복구 시 권한 비복구
  (g) 번역 upsert(요청에 없는 언어 유지)와 menus/my 의 메뉴 이름이 요청 언어로 바뀜
  (h) 새 PAGE 메뉴를 만들고 역할에 권한을 부여하면 해당 사용자의 menus/my 에 즉시 나타나고, 회수·삭제하면 즉시 사라짐(캐시 무효화)
  (i) 권한 없는 사용자 403, 두 테넌트 분리
  (j) 일괄 업그레이드가 새 오류 문구를 기존 테넌트에 추가하되 바뀐 값은 보존
  테스트가 만든 행과 스키마를 정리하고 pmis 업무 테이블 0행(실제 테넌트 행 불변)을 확인한다. 기존 통합 테스트 전부(현재 40개, 보고서에 개수 기록)가 통과해야 한다.
- 새 라이브러리는 추가하지 않는다. 공유 DB 에 마이그레이션을 적용하는 일은 이 단계에 없다.
- 진행 기록은 [9.3단계] 형식으로 doc/cursor-progress.md 에 추가하고, 보고서는 "작업 방식" 형식(docs/reports/9.3.md, 9.3.diff)으로 쓴다. 질문은 해당 부분만 멈추고 목록에 올린다. 커밋은 `[9.3] 요약` 한 개, push 는 하지 않는다.
````


## 9.4단계: 파일·팀 관리 API (+ 9.3 작은 보완)

9.3 승인(검토 기록: docs/reviews/2026-10-19-9.3.md). 이번 단계는 테넌트 스키마 V3 의 `file_storage`, `file`, `file_attachment`, `team`, `team_member` 에 대한 API 이고, 9.1 에서 미뤄 둔 사람 프로필 사진(`person.profile_file_pk`)을 연결한다. 이 단계만 수행하고 멈춘다. 이 단계가 끝나면 백엔드 MDM 관리 API 가 모두 갖춰진다.

````
기준 문서: doc/erd/erd-model-file-team-auth-menu-*.json 의 파일·팀 영역과 erd-model-org-user-*.json 의 person(정본, 수정 금지), 테넌트 마이그레이션 V1~V6, db/seed/tenant/S03·S04·S05, doc/auth-design.md, doc/api/mdm-api.md, 9.1 org 패키지·9.2 access 패키지·9.3 menu 패키지(같은 패턴: 컨트롤러-서비스-저장소, OrgExceptions 식 헬퍼, EntityTextService, CurrentTenantUser, 코드값 검증 OrgCodes, 캐시 무효화 AccessSessions). ERD 와 어긋나거나 모호한 부분은 임의로 정하지 말고 질문 목록에 올린다. DDL(V1~V6)은 바꾸지 않는다. 꼭 필요하면 구현을 멈추고 질문한다(예: person.profile_file_pk 의 FK 가 V3 에 없으면 V7 이 필요한지).

[A. 9.3 작은 보완 - 먼저 처리한다]
1. MenuAdminService.moveDescendants 와 assertDepth 가 `menu_path LIKE '<oldPath>/%'` 를 쓴다. 메뉴 코드는 `_` 를 허용하므로 `_` 가 LIKE 와일드카드로 작동해 다른 메뉴의 자손까지 바뀔 수 있다. 접두사 비교를 이스케이프된 LIKE(`ESCAPE`) 또는 `left(menu_path, n) = ?` 같은 정확 비교로 바꾸고, `_` 가 든 코드(예: `A_B` 와 `AXB` 두 가지)로 이동 시 다른 메뉴가 바뀌지 않는 회귀 테스트를 통합 테스트에 추가한다. 같은 패턴이 다른 곳(9.1 부서는 숫자 경로라 해당 없음, 그 외 LIKE 로 경로를 비교하는 곳)에 있는지 grep 해서 있으면 같이 고친다.
2. UserAccountService.response 의 탭 문자 섞임과 MenuAdminService.route 의 불필요한 삼항(`"GROUP".equals(type) ? route : route`)을 정리한다. 동작 변경 없음.

[B. 공통 결정]
1. URL(복수 kebab-case, PK 경로): /api/mdm/v1/file-storages, /api/mdm/v1/files, /api/mdm/v1/attachments, /api/mdm/v1/teams, /api/mdm/v1/teams/{teamPk}/members, /api/mdm/v1/team-members/{teamMemberPk}, /api/mdm/v1/people/{personPk}/profile-image. 목록은 7단계 규약(page/size/sort 화이트리스트, useYn 생략 시 사용 중지 포함), 래퍼·오류·fieldErrors·감사 사용자·논리 삭제·복구 관례는 9.1~9.3 과 같다. 코드 컬럼(storage_code, team_code)은 등록 후 변경 불가, 삭제한 코드는 재사용 불가(DB 전체 유니크 유지).
2. 권한(@RequirePermission): 새 메뉴·권한이 필요하다. S05 를 읽어 기존 SYSTEM 계열 메뉴 구조와 이름 규칙에 맞춰 다음 PAGE 메뉴와 기본 권한 5개(READ/CREATE/UPDATE/DELETE/EXPORT)를 멱등으로 추가한다: FILE_STORAGE(저장소 관리), FILE(파일 관리), TEAM(팀 관리). TENANT_ADMIN 에는 모두 부여하고 다른 역할(MANAGER, USER)에는 이 메뉴의 권한을 주지 않는다(8.4/9.2 의 기존 시드 결정과 같은 원칙. 다르게 해야 하면 질문). 일괄 업그레이드가 기존 테넌트에도 이 행을 추가하되 테넌트가 고친 값은 덮어쓰지 않는다. menu_code 가 9.3 의 MenuRules 형식에 맞는지 확인하고, 시드 메뉴 보호 목록(SeedMenuCodes)이 새 코드를 포함하는지 확인한다.
3. 코드값 검증은 OrgCodes 방식: storage_type_code(STORAGE_TYPE), file_status_code(FILE_STATUS), attachment_category_code(ATTACHMENT_CATEGORY, 값이 없는 그룹이면 값이 비어 있는 동안은 null 만 허용하고 임의 값은 400 ORG.INVALID_CODE_VALUE), team_type_code(TEAM_TYPE), team_role_code(TEAM_ROLE).
4. 이름의 언어별 표기(team_name)는 9.1 의 i18n_entity_text upsert 방식(target_table_name=team). 단건 조회에서만 translations 반환.
5. 비밀(secret)은 저장·응답·로그·문서 어디에도 남기지 않는다. file_storage.secret_ref 는 환경변수/Secret 이름 참조만 받는다(값처럼 보이는 문자열 거절 규칙은 만들지 말고, 형식 `^[A-Za-z0-9_.-]{1,200}$` 만 검사). connection_config 는 JSON 객체여야 하고(파싱 검증), 키 이름이 password/secret/token/key 로 끝나거나 포함하면 400 FILE.SECRET_IN_CONFIG.

[저장소 file_storage]
- 목록(페이징): keyword(코드·이름), storageTypeCode, useYn. 정렬: storageCode, storageName, createDatetime, storagePk. 단건. 등록·수정·논리 삭제·복구.
- 필드는 ERD 를 따른다. storage_type_code 는 STORAGE_TYPE 값이지만 구현된 어댑터는 LOCAL 하나뿐이다. LOCAL 외 유형을 등록하는 것은 허용하되(설정만) 그 저장소로 업로드/다운로드하면 501 FILE.STORAGE_TYPE_UNSUPPORTED. base_path: LOCAL 은 절대 경로이거나 설정 `mdm.file.local-root` 아래 상대 경로, `..` 포함 금지(400), 테넌트 간 분리를 위해 서버가 실제 저장 경로에 테넌트 코드를 넣는다(`<base>/<tenantCode>/...`). max_file_size_mb 는 1 이상, allowed_extensions 는 쉼표 구분 소문자 확장자 목록(형식 검증).
- default_yn='Y' 는 사용 중 저장소 중 1건(V3 부분 유니크). 새 기본 저장소를 지정하면 기존 기본을 같은 트랜잭션에서 N 으로 내린다. 기본 저장소를 삭제하려 하면 409 FILE.DEFAULT_STORAGE(다른 저장소를 먼저 기본으로 지정). 파일이 있는 저장소(file.storage_pk 참조, 삭제 상태 제외)를 삭제하면 409 FILE.STORAGE_IN_USE. 마지막 사용 중 저장소를 삭제하면 409.
- 기본 저장소가 하나도 없을 때 업로드: 설정 `mdm.file.local-root` 가 있으면 코드 DEFAULT, 유형 LOCAL, base_path 는 root 를 가리키는 상대 경로로 자동 생성한다(처음 업로드 때 지연 생성). root 설정이 없으면 409 FILE.NO_DEFAULT_STORAGE. 설정 기본값은 비워 두고(dev 프로필만 로컬 임시 폴더 하위 값을 두되 저장소 밖 경로 금지) application.yaml 과 doc/local-setup.md 에 설명한다.

[파일 file]
- 업로드 POST /files (multipart/form-data, 필드 `file` 하나, 선택 `storageCode`). 규칙: 크기 상한 = min(저장소 max_file_size_mb, 애플리케이션 상한 설정 `mdm.file.max-size-mb` 기본 50) 초과 시 413 FILE.TOO_LARGE(스프링 multipart 한도 초과도 같은 오류로 매핑). 확장자는 파일명에서 추출해 소문자로 비교하고 저장소 allowed_extensions 가 있으면 그것만, 없으면 애플리케이션 기본 정책(허용 목록 설정 `mdm.file.allowed-extensions`, 위험 확장자 exe, bat, cmd, com, sh, js, jar, msi, dll, scr, ps1, vbs 는 항상 거절) 적용, 위반 400 FILE.EXTENSION_NOT_ALLOWED. 이중 확장자(`a.php.png` 등)는 마지막 확장자 기준이되 위험 확장자가 중간에 있으면 거절한다. MIME: 새 라이브러리 없이 서버가 처음 몇 바이트(매직 바이트)로 흔한 형식(png, jpg, gif, webp, pdf, zip 계열)을 확인해 확장자와 모순되면 400 FILE.MIME_MISMATCH, 알 수 없는 형식은 확장자 기준으로 통과. 요청이 준 Content-Type 은 신뢰하지 않는다(DB 의 mime_type 은 서버가 판정한 값).
- 저장: 서버가 생성한 UUID 로 storage_key 를 만든다(`yyyy/MM/<uuid>` 형태, 사용자 입력 파일명을 경로에 쓰지 않는다). 원본 파일명은 경로 구분자·제어문자를 제거한 값으로 original_file_name 에 저장(300자 초과 시 줄임, 확장자 보존). 저장 중 SHA-256 을 계산해 checksum_sha256 에 넣는다. 처음 file_status_code='TEMP', upload_user_pk=현재 사용자, upload_datetime=지금. 디스크 쓰기는 임시 파일에 쓴 뒤 이동(원자적)하고, DB 저장이 실패하면 디스크 파일을 지운다. 응답: filePk, originalFileName, fileSize, mimeType, fileExtension, checksumSha256, fileStatusCode(storage_key, base_path 등 저장 위치는 절대 응답에 넣지 않는다).
- 업로드 권한: FILE:CREATE 가 아니라 "로그인한 사용자 누구나"는 위험하다. 이번 단계는 FILE:CREATE 를 요구하되, 첨부를 붙이는 화면(사람 프로필 등)에서 쓰는 일반 사용자도 있으므로 질문 목록에 "일반 사용자의 업로드 권한 정책"을 올리고 임시로 FILE:CREATE 로 한다.
- 다운로드 GET /files/{filePk}/download: 서버가 권한을 확인한 뒤 스트리밍한다. 허용: FILE:READ 보유자, 또는 TEMP 파일의 업로더 본인. 응답 헤더: Content-Type 은 저장된 mime_type(없으면 application/octet-stream), Content-Disposition: attachment 와 RFC 5987 인코딩된 안전한 파일명, X-Content-Type-Options: nosniff, Cache-Control: private, no-store. 대상 파일이 DELETED 이거나 use_yn='N' 이거나 디스크에 없으면 404 FILE.NOT_FOUND(디스크에 없는 경우는 로그에 파일 PK 만 남긴다). Range 요청은 지원하지 않는다(문서에 한계로 적는다). 첨부 대상별 접근(예: 사람 사진)은 아래 profile-image 와 attachments 에서 별도로 다룬다.
- 목록 GET /files(FILE:READ, 페이징): keyword(원본 파일명), fileStatusCode, uploadUserPk, 업로드 기간(from/to, 테넌트 날짜), useYn. 정렬: uploadDatetime, originalFileName, fileSize, filePk. 단건 GET /files/{filePk}: 메타데이터와 첨부 목록(어떤 대상에 붙어 있는지).
- 삭제 DELETE /files/{filePk}: 사용 중인 첨부(file_attachment.use_yn='Y')나 person.profile_file_pk 로 참조되는 동안은 409 FILE.IN_USE. 통과하면 use_yn='N', file_status_code='DELETED', delete_scheduled_datetime = 지금 + 설정 `mdm.file.delete-retention-days`(기본 7). 복구 POST /files/{filePk}/restore 는 디스크 파일이 아직 있고 물리 삭제 전일 때만(없으면 409 FILE.PURGED), 상태를 TEMP 가 아니라 이전 의미인 ACTIVE(첨부가 있었으면) 또는 TEMP 로 되돌리는 규칙을 정하고 문서에 적는다.
- 정리 POST /files/cleanup (FILE:DELETE): ① TEMP 이면서 upload_datetime 이 설정 `mdm.file.temp-ttl-hours`(기본 24) 이전인 파일 → 디스크 삭제 후 file_status_code='DELETED', use_yn='N', delete_scheduled_datetime=지금(행 유지), ② DELETED 이고 delete_scheduled_datetime 이 지난 파일 → 디스크 파일만 물리 삭제하고 행은 유지(ERD 설명: 물리 삭제 후에도 행은 유지). 디스크 삭제 실패는 건너뛰고 건수에 집계. 응답: 처리 건수들. 스케줄러(@Scheduled)는 이번 단계에 만들지 않는다(테넌트 순회 방식 결정이 필요하므로 질문 목록에 "정리 작업 자동 실행 방식"을 올린다).

[첨부 file_attachment]
- POST /attachments {fileId(=filePk), targetTableName, targetKey, attachmentCategoryCode?, sortOrder?, description?}: targetTableName 은 화이트리스트(코드 안의 작은 레지스트리 하나: person→PERSON, company→COMPANY, dept→DEPT, team→TEAM 의 메뉴 코드 매핑)만 허용하고 그 외는 400 FILE.TARGET_NOT_ALLOWED. 대상 행이 존재하고 사용 중이어야 한다(404 대상 NOT_FOUND 계열 오류를 재사용). 권한은 대상 메뉴의 UPDATE(person→PERSON:UPDATE 등)다 — 이를 위해 @RequirePermission 의 정적 선언이 아니라 PermissionChecker 를 서비스에서 직접 호출하는 방식을 쓴다(9.2 DbPermissionChecker 재사용). 파일은 TEMP 이고 업로더 본인이어야 하거나(다른 사람의 TEMP 파일 첨부 금지 → 403/409 중 기존 관례 선택) 이미 ACTIVE 여야 한다. 성공하면 file_status_code='ACTIVE'. 같은 (target_table_name, target_key, file_pk) 가 사용 중이면 409 FILE.ATTACHMENT_DUPLICATE(논리 삭제된 행은 되살린다. V3/V5 유니크 확인).
- GET /attachments?targetTableName=&targetKey=(배열): 권한은 대상 메뉴의 READ. 항목: attachmentPk, filePk, 파일 메타, 구분, 정렬, 설명. 첨부 파일 다운로드는 FILE:READ 가 아니라 "대상 READ 권한이 있는 사용자"도 허용하도록 GET /attachments/{attachmentPk}/download 를 따로 둔다(첨부에 연결된 파일만, 대상 메뉴의 READ 로 판단).
- DELETE /attachments/{attachmentPk}(대상 UPDATE): 첨부를 use_yn='N'. 그 파일에 다른 사용 중 첨부가 없고 profile 참조도 없으면 파일을 TEMP 로 되돌리지 말고 ACTIVE 로 두되 delete_scheduled 를 걸지 않는다(정리는 관리자가 파일 화면에서 한다 — 이 결정과 이유를 문서에 적는다).

[사람 프로필 사진]
- PUT /people/{personPk}/profile-image {fileId}: 권한 PERSON:UPDATE. 파일은 이미지 형식(png, jpg, gif, webp)만, TEMP 업로더 본인 또는 ACTIVE 여야 한다. person.profile_file_pk 를 설정하고 파일을 ACTIVE 로 올린다. 이전 사진 파일은 다른 참조가 없으면 TEMP 가 아니라 그대로 두고(삭제는 관리자 정리) profile 참조만 끊는다. DELETE /people/{personPk}/profile-image 로 해제. GET /people/{personPk}/profile-image: PERSON:READ 로 이미지 스트리밍(없으면 404 FILE.NOT_FOUND). 사람 단건/목록 응답에 hasProfileImage(boolean) 를 추가한다(9.1 응답 변경이므로 문서 갱신, 목록은 N+1 없이). PersonService 가 profile_file_pk 를 요청 본문에서 받지 않는 기존 규칙은 유지한다.
- person.profile_file_pk 의 FK 가 V3 에 이미 추가되어 있는지 확인한다. 없으면 구현을 멈추고 질문한다(V7 필요 여부).

[팀 team, team_member]
- 팀 목록(페이징): keyword(코드·이름), teamTypeCode, 활동 여부(active=true 면 종료일이 없거나 오늘 이후), useYn. 정렬: teamCode, teamName, sortOrder, startDate, teamPk. 항목에 현재 팀원 수(memberCount, 집계 한 번). 단건(번역 포함). 등록·수정·논리 삭제·복구.
- 필드는 ERD(team)를 따른다. team_code 형식 `^[A-Z0-9_-]{1,50}$`(근거를 문서에 쓰고 ERD 와 다르면 질문), 중복 409 TEAM.DUPLICATE. start_date <= end_date(위반 400 TEAM.INVALID_DATE). 팀은 회사에 종속되지 않는다.
- 팀 삭제 거절: 현재 팀원(end_date IS NULL, use_yn='Y')이 있으면 409 TEAM.HAS_ACTIVE_MEMBER. 팀을 종료(end_date 설정)하는 것은 수정 API 로 하고, end_date 가 오늘 이전이어도 현재 팀원이 있으면 막지 않는다(문서에 적는다). 복구는 UPDATE.
- 팀원 GET /teams/{teamPk}/members(배열, 과거 포함, 기본 start_date 내림차순, useYn/현재 여부 필터), POST /teams/{teamPk}/members {personPk, teamRoleCode, startDate, endDate?, description?}, PUT /team-members/{teamMemberPk}(역할·기간·설명), POST /team-members/{teamMemberPk}/end {endDate?(기본 오늘)}, DELETE /team-members/{teamMemberPk}(논리 삭제), POST .../restore. 규칙: 사람은 사용 중이어야 한다(PERSON.NOT_FOUND/PERSON.INACTIVE 계열 기존 코드 재사용, 없으면 새로 정의), 같은 팀에서 사람당 현재 소속은 1건(V3 부분 유니크, 위반 409 TEAM.MEMBER_DUPLICATE. 같은 사람이 다시 참여하면 이전 행이 종료된 뒤 새 행), start_date <= end_date(400 TEAM.INVALID_DATE), 이미 종료된 팀원을 다시 종료하면 409 TEAM.MEMBER_ALREADY_ENDED, 사용 중지된 팀원 종료는 409 TEAM.MEMBER_INACTIVE. 팀원 응답에 person 이름(번역 아님), 사람의 현재 주 소속 요약(회사·부서) 선택 포함은 하지 않는다(요약이 필요하면 사람 API 를 쓴다).
- 팀장(LEADER) 수 제한은 ERD 에 없어 구현하지 않는다. 질문 목록에 "팀당 LEADER 1명 제한 여부"를 올린다.
- 사람 삭제(9.1)에 팀원 확인을 추가할지는 질문 목록에 올리고 현재 규칙(PERSON.HAS_ACTIVE_ASSIGNMENT, PERSON.HAS_ACCOUNT)은 바꾸지 않는다.

[오류 코드와 문구] 위에서 정의한 FILE.*, TEAM.* 오류 코드(와 필요한 PERSON/ASSIGNMENT 보조 코드)의 ko-KR/en-US 문구를 db/seed/tenant 의 error 네임스페이스 Seed 에 멱등(INSERT … WHERE NOT EXISTS, 고친 문구 보존)으로 추가한다.

[문서] doc/api/mdm-api.md 에 위 엔드포인트의 요청·응답 예시(파일 응답에 저장 경로가 없음을 명시), 필터·정렬 필드, 권한 표(새 메뉴 코드 포함), 오류 코드 표, 업로드 정책(크기, 확장자, MIME, 임시 파일 수명), 다운로드 헤더, 첨부 대상 화이트리스트, 한계(Range 미지원, 정리 자동 실행 없음, LOCAL 외 어댑터 없음, 바이러스 검사 없음)를 쓴다. doc/local-setup.md 에 `mdm.file.local-root` 등 설정 키를 쓴다. doc/auth-design.md 공개 경로 표에 변동이 없음을 확인한다(모두 인증 필요).

[테스트]
- 단위/슬라이스(DB 없음): 확장자·위험 확장자·이중 확장자 판정, 매직 바이트 판정, 파일명 정제, storage_key 생성(사용자 입력 미사용), base_path 검증(`..`), connection_config 비밀 키 거절, 오류 코드 매핑, 권한(기본 프로필 403/토큰 없음 401), 정렬 화이트리스트, 다운로드 헤더(nosniff, no-store, 안전한 Content-Disposition), 임시 디렉터리를 쓰는 LOCAL 어댑터(원자적 저장, 실패 시 정리, 경로 이탈 시도 거절).
- 통합(@Tag("integration"), 9.1~9.3 과 같은 절차: 접속 정보 비밀 보호, 실행 전 DB 이름 pmis 확인, t_it_<랜덤> 스키마만, 프로비저닝 서비스로 ACTIVE 테넌트, 테스트 JWT, 파일은 임시 폴더(테스트 끝에 삭제)에 저장):
  (a) 엔티티 validate (V1~V6)와 새 시드(메뉴·권한·역할 부여·오류 문구)의 멱등과 일괄 업그레이드(고친 값 보존)
  (b) 9.3 보완: `_` 가 든 메뉴 코드 이동 회귀 테스트
  (c) 저장소: 등록·수정·삭제·복구, 기본 저장소 전환(기존 기본이 N 이 됨), 기본/사용 중 저장소 삭제 거절, 비밀 키 거절, 지연 생성되는 기본 저장소
  (d) 업로드/다운로드: 정상 업로드 후 다운로드 바이트 일치와 checksum 일치, 저장 경로가 응답에 없음, 크기 초과 413, 확장자 거절, MIME 불일치 거절, 다운로드 권한(FILE:READ 있음/없음, TEMP 업로더 본인), 삭제된 파일 404
  (e) 첨부: 사람에 첨부 → 파일 ACTIVE, 대상 READ 만 있는 사용자의 첨부 다운로드 허용·첨부 등록 거절, 화이트리스트 밖 대상 거절, 중복 첨부 거절, 삭제된 첨부 재부착(되살림), 첨부된 파일 삭제 거절
  (f) 프로필 사진: 설정·교체·해제·조회, 이미지 아닌 파일 거절, hasProfileImage 가 사람 목록/단건에 반영되고 목록 쿼리 수가 페이지 크기와 무관(목 또는 통계 검증, 9.1 방식), 사진이 설정된 파일의 삭제 거절
  (g) 정리: TEMP 만료 → DELETED 와 디스크 삭제, 보존 기간 지난 DELETED 의 물리 삭제(행 유지), 디스크 삭제 실패 집계
  (h) 팀: 등록·수정·번역 upsert·삭제·복구, 팀원 추가·역할 수정·종료·재참여(새 행)·중복 거절·삭제 거절(현재 팀원), memberCount 집계, 날짜 검증
  (i) 권한 없는 사용자 403, 두 테넌트 분리(같은 팀 코드·저장소 코드·파일 경로가 서로 간섭하지 않음, 다른 테넌트의 파일 PK 로 다운로드 불가)
  테스트가 만든 행·디스크 파일·스키마를 정리하고 pmis 업무 테이블 0행(실제 테넌트 행 불변)을 확인한다. 기존 통합 테스트 전부(현재 42개, 보고서에 개수 기록)가 통과해야 한다.
- 새 라이브러리는 추가하지 않는다(필요하면 구현 전에 질문). 공유 DB 에 마이그레이션을 적용하는 일은 이 단계에 없다.
- 질문이 있으면 해당 부분만 멈추고 진행 기록의 질문 목록에 올린다. 진행 기록은 [9.4단계] 형식으로 doc/cursor-progress.md 에 추가하고, 보고서는 "작업 방식" 형식(docs/reports/9.4.md, 9.4.diff)으로 쓴다. 커밋은 `[9.4] 요약` 한 개, push 는 하지 않는다.
````

## 9.5단계: 파일 보완 + 정리 스케줄러

9.4 승인(검토 기록: `docs/reviews/2026-10-19-9.4.md`). 이번 단계는 9.4 검토에서 나온 보완 5건과, 파일 정리를 자동으로 실행하는 스케줄러다. DDL 변경은 원칙적으로 하지 않는다. 필요해 보이면 구현 전에 보고서 질문(d절)에 적고, 가장 작은 변경으로 진행한다.

시작 전: `main` 으로 전환, `git pull --ff-only`, Claude 가 쓴 파일(지시서, `docs/reviews/*`)이 변경 상태면 먼저 `docs:` 커밋으로 올린다. 작업 방식은 이 파일 맨 위 "작업 방식" 블록을 따른다.

### A. 파일 보완 (9.4 검토)

1. **MIME 검사 강화** (`FileRules.assertMime`, `mimeType`)
   - 확장자가 png, jpg, jpeg, gif, webp, pdf 인데 `sniff` 결과가 null 이면 `FILE.MIME_MISMATCH` 로 거부한다. 지금은 null 이면 통과한다.
   - zip 계열(docx, xlsx 등)은 지금처럼 `PK` 판별을 쓰되, 판별 실패 시 거부한다.
   - txt, csv 등 판별이 불가능한 확장자는 기존대로 허용한다. 대신 NUL 바이트(0x00)가 앞 8KB 에 있으면 거부한다(텍스트가 아닌 파일 위장 방지).
   - 단위 테스트: `x.png` 에 일반 텍스트, `x.pdf` 에 PNG 바이트, `x.txt` 에 NUL 포함, 정상 파일 각각.
2. **검색어 와일드카드 이스케이프**
   - 공통 유틸(예: `com.tommy.common.global.query.LikeEscape.contains(String)`)을 만든다. `\`, `%`, `_` 를 이스케이프하고, 쿼리에 `escape '\'` 를 지정한다.
   - 적용 대상: `FileService.filter` 와 회사, 부서, 사람, 팀, 사용자, 공통코드, 다국어 등 `keyword` 로 LIKE 검색하는 모든 곳. 먼저 `grep` 으로 `builder.like`, `LIKE` 사용처를 모두 찾아 보고서 b절에 목록으로 적는다.
   - 9.3 에서 `left` 로 고친 메뉴 경로 비교는 건드리지 않는다.
   - 통합 테스트: 파일 이름에 `a_b`, `aXb`, `100%` 를 두고 `_`, `%` 검색이 문자 그대로만 찾는지.
3. **정리 집계 정확화** (`FileService.cleanup`)
   - 이미 디스크에서 지워진 DELETED 파일이 실행마다 `purged` 로 세어지지 않게 한다. 디스크 삭제에 성공한 순간 `deleteScheduledDatetime` 을 NULL 로 만들어 다음 실행의 대상에서 뺀다(DDL 변경 없음).
   - `StoredFile.markPurged(actor)` 같은 도메인 메서드로 두고, 복구(`restore`)는 이미 `FILE.PURGED` 로 막혀 있으므로 그 판단이 NULL 예약 시각과도 어긋나지 않는지 확인한다.
   - 같은 정리를 두 번 연속 실행했을 때 두 번째는 `expired=0, purged=0` 인 통합 테스트.
4. **고아 파일 방지** (`FileService.upload`)
   - 디스크 쓰기 뒤 트랜잭션이 롤백되면 디스크 파일을 지운다. `TransactionSynchronizationManager.registerSynchronization` 의 `afterCompletion(STATUS_ROLLED_BACK)` 에서 `deleteQuiet`. 현재의 `saveAndFlush` 실패 처리도 같은 경로로 합쳐 중복을 없앤다.
   - 단위 또는 슬라이스 테스트로 롤백 시 파일이 지워지는지.
5. **다운로드 스트리밍은 이번에 하지 않는다.** 대신 `doc/api/mdm-api.md` 의 파일 절에 "현재 전체를 메모리에 올림, 최대 크기 상한으로 제한" 이라고 한 줄 남긴다.

### B. 정리 스케줄러

목적: 임시 파일 만료와 삭제 보관기간 경과 파일의 디스크 삭제를, 관리자가 API 를 부르지 않아도 정해진 시간에 자동 실행한다. 기본은 꺼짐이다.

1. **설정** (`application.yaml`, 기본값은 꺼짐, `FileProperties` 에 묶는다)
   ```yaml
   mdm:
     file:
       cleanup:
         enabled: false          # 운영에서만 true
         cron: "0 0 3 * * *"     # 매일 03:00
         zone: Asia/Seoul
   ```
   기존 `temp-ttl-hours`, `delete-retention-days` 는 그대로 쓴다. 환경 변수로도 덮어쓸 수 있게 `MDM_FILE_CLEANUP_ENABLED` 등을 `application.yaml` 에 연결한다. `application-dev.yaml` 은 꺼짐을 유지한다.
2. **실행기** (`FileCleanupScheduler`, `@ConditionalOnProperty(mdm.file.cleanup.enabled=true)`)
   - `@Scheduled(cron = "${mdm.file.cleanup.cron}", zone = "${mdm.file.cleanup.zone}")`. `@EnableScheduling` 은 설정 클래스에 두되, 꺼짐일 때는 스케줄러 빈 자체가 만들어지지 않아야 한다.
   - `pmis.tenant` 에서 상태가 ACTIVE 인 테넌트를 모두 읽고, 테넌트마다 그 스키마 컨텍스트에서 정리를 실행한다. 컨텍스트 설정과 해제는 기존 테넌트 컨텍스트 방식을 그대로 쓴다(요청 필터가 쓰는 것과 같은 홀더). 반드시 `finally` 에서 해제한다.
   - 정리 로직은 `FileService.cleanup()` 을 그대로 재사용한다. 단, 이 메서드는 지금 `permissions.check("FILE","DELETE")` 를 하므로, 권한 확인이 없는 내부용 메서드(`cleanupInternal(actorPk)`)로 분리하고 API 용은 권한 확인 후 그것을 부르게 한다.
   - 실행자(`actor`)는 사람 사용자가 아니므로 감사 컬럼에는 시스템 계정이 필요하다. 시스템 사용자(`user_pk`)가 이미 있는지 확인하고 없으면 임시로 쓸 값과 이유를 보고서 d절 질문에 쓴다. 임의로 DDL 이나 시드를 만들지 않는다.
   - 테넌트 하나가 실패해도 예외를 잡아 `WARN` 로그(테넌트 코드, 예외 종류만. 메시지에 파일 경로가 있으면 넣지 않는다)를 남기고 다음 테넌트로 진행한다.
   - 실행이 끝나면 `INFO` 한 줄: 테넌트 수, 성공/실패 수, 만료·삭제·실패 파일 수의 합계.
   - 서버가 한 대라는 전제다. 같은 인스턴스에서 이전 실행이 끝나기 전에 다음 실행이 시작되지 않게 `AtomicBoolean` 으로 막는다. 여러 대 환경의 중복 방지(DB 락 등)는 이번 범위가 아니며, `doc/api/mdm-api.md` 나 `local-setup.md` 에 "단일 인스턴스 전제" 로 적는다.
3. **수동 API 유지**: `POST /api/mdm/v1/files/cleanup` 은 그대로 둔다. 응답 형식 변경 없음.
4. **보안**: 스케줄러는 HTTP 로 노출하지 않는다. 새 엔드포인트를 만들지 않는다.

### C. 테스트

- 단위: `LikeEscape`, `FileRules.assertMime` 신규 케이스, 정리 집계(두 번째 실행 0건).
- 통합 (`t_it_` 스키마 사용, 실제 `pmis.tenant` 행 불변 확인 유지):
  - 스케줄러 빈은 `enabled=false` 일 때 없고, `true` 일 때 있다(`ApplicationContextRunner` 또는 프로퍼티 오버라이드).
  - 스케줄러의 1회 실행 메서드를 직접 호출해, 테스트 테넌트 두 곳에서 만료 임시 파일이 정리되고, 한 테넌트가 실패해도 다른 테넌트가 정리되는지. 크론 시각을 기다리는 테스트는 만들지 않는다.
  - 업로드 롤백 시 디스크 파일 삭제.
- 기존 단위 81건, 통합 44건이 계속 통과해야 한다.

### D. 문서

- `doc/api/mdm-api.md`: 정리 절에 스케줄러 설명, 설정 키 표(`enabled`, `cron`, `zone`), 단일 인스턴스 전제, 메모리 사용 안내.
- `doc/local-setup.md`: 운영 환경변수 목록에 `MDM_FILE_CLEANUP_ENABLED`, `MDM_FILE_CLEANUP_CRON` 추가.
- `doc/cursor-progress.md` 에 9.5 항목 추가.

### E. 마무리

- 커밋: `[9.5] 파일 보완과 정리 스케줄러` (필요하면 둘로 나누어도 된다).
- 보고서: `docs/reports/9.5.md` 와 `9.5.diff` (`.diff` 는 기존대로 추적하지 않음). a–e 형식에 git 상태 줄 포함. b절에 LIKE 사용처 점검 목록을 넣는다.
- push 하지 않는다. 사용자가 "push 해줘" 라고 할 때만 한다.

## 9.6단계: 부서 경로 점검, 이력 부서 이름, 코드 시드, 조직 권한 현황

9.5 승인(검토 기록: `docs/reviews/2026-10-22-9.5.md`). 프런트 F8·F9 에서 나온 요청과 9.5 검토에서 넘긴 항목을 묶은 작은 단계다. 시드 값과 권한 현황 외에는 DDL 변경 없음. 이 단계만 수행하고 멈춘다.

시작 전: `main` 으로 전환, `git pull --ff-only`, Claude 가 쓴 파일(지시서, `docs/reviews/*`)이 변경 상태면 먼저 `docs:` 커밋으로 올린다. 작업 방식은 이 파일 맨 위 "작업 방식" 블록을 따른다.

### A. 부서 경로 LIKE 점검 (9.5 검토 항목)

- `DeptService.relocate` 의 `dept_path LIKE` 가 사용하는 값이 `/상위PK/.../자신PK` 형태(숫자와 슬래시뿐)인지 코드에서 확인한다. 부서 코드나 이름이 경로에 섞이는 곳이 없다면 `_`, `%` 문제가 없다.
- 확인 결과를 코드 주석 한 줄(왜 안전한가)과 `doc/api/mdm-api.md` 부서 경로 절 한 문장으로 남긴다.
- 회귀 테스트: 부서를 이동한 뒤 형제 부서의 경로가 바뀌지 않는지, PK 가 `1`, `10`, `100` 처럼 접두사가 겹칠 때(`/1/` 대 `/10/`) 하위만 갱신되는지 통합 테스트로 확인한다(접두사에 끝 슬래시가 포함되는지도 확인).
- 만약 경로에 숫자가 아닌 값이 섞이는 곳이 있으면 `LikeEscape` 로 이스케이프하거나 `left` 비교로 바꾸고 보고서에 이유를 쓴다.

### B. 부서 이력 응답에 상위 부서 이름 추가

- 현재 `GET /departments/{deptPk}/history` 는 변경 전후 상위 부서 PK 만 준다. 응답 항목에 `beforeParentDeptName`, `afterParentDeptName` 을 추가한다(기존 필드는 유지). 값은 해당 부서의 현재 이름이다(이력 시점의 이름이 아님). 요청 언어 이름이 아니라 `dept_name` 컬럼 값을 쓴다.
- 페이지의 PK 들을 모아 `IN` 한 번으로 조회한다(N+1 금지). 부서가 삭제(종료)된 경우에도 이름은 나온다. 대상이 없으면 null.
- 테스트: 이름 변경이 섞인 이력에서 이름이 채워지는지, 쿼리 수가 페이지 크기에 비례하지 않는지(기존 방식의 쿼리 수 측정 테스트가 있으면 같은 방식).
- `doc/api/mdm-api.md` 이력 절과 DTO 를 고친다.

### C. 직위·직책 코드 시드 값

- 공통코드 그룹 `POSITION`, `JOB_TITLE` 은 그룹만 있고 값이 없어 사람 화면의 직위·직책 선택이 비어 있다. 테넌트 시드(`S0x_...` 중 공통코드 시드를 확인해 같은 파일 또는 새 번호의 파일)에 기본 값을 추가한다. 멱등(`INSERT … WHERE NOT EXISTS`)이고 테넌트가 수정한 이름·정렬은 덮어쓰지 않는다. 기존 시드 파일을 바꿔도 이미 만들어진 테넌트에 반영되지 않는지 확인하고(시드 재실행 방식을 코드에서 확인), 반영 방식과 한계를 보고서에 쓴다.
- 기본 값(코드 → ko-KR / en-US 이름): POSITION: `STAFF` 사원/Staff, `ASSISTANT_MANAGER` 대리/Assistant Manager, `MANAGER` 과장/Manager, `DEPUTY_GENERAL_MANAGER` 차장/Deputy General Manager, `GENERAL_MANAGER` 부장/General Manager, `DIRECTOR` 이사/Director, `EXECUTIVE` 임원/Executive. JOB_TITLE: `MEMBER` 팀원/Member, `TEAM_LEAD` 팀장/Team Lead, `DEPT_HEAD` 부서장/Department Head, `DIVISION_HEAD` 본부장/Division Head, `CEO` 대표/CEO. 코드 값 길이·형식은 그룹 정책(`code_value_length` 등)과 기존 검증 규칙을 먼저 확인하고 맞지 않으면 정책에 맞게 줄이고 보고한다. 정렬 순서는 10 단위로.
- 이름은 공통코드 이름 번역 테이블(`i18n_entity_text` 의 공통코드 이름 대상)에 ko-KR 과 en-US 로 넣는다. 기존 공통코드 시드가 쓰는 방식을 그대로 따른다.
- 통합 테스트: 새 테넌트를 만들면 두 그룹에 위 값이 있고 `ORG.INVALID_CODE_VALUE` 검증이 통과하는 것(예: 사람 소속에 `MANAGER` 직위 저장), 시드를 두 번 실행해도 중복이 없고 수정한 이름이 유지되는 것.

### D. 조직 권한 현황 보고 (코드 변경 없음)

- 시드 S05 를 읽고(스테이징이 안 될 만큼 경로가 깊어 Cursor 가 직접 읽는다) `COMPANY`, `DEPT`, `PERSON`, `USER`, `ROLE`, `PERMISSION`, `FILE`, `FILE_STORAGE`, `TEAM`, `COMMON_CODE`, `I18N`, `MENU` 각 메뉴의 READ/CREATE/UPDATE/DELETE/EXPORT 가 `TENANT_ADMIN`, `MANAGER`, `USER` 중 어느 역할에 부여되어 있는지 표로 보고서 b절에 적는다. 값이 없으면 "없음".
- 이 표를 바탕으로 Claude 가 일반 사용자 기본 권한을 정할 수 있다. 이 단계에서 시드 권한을 바꾸지 않는다.

### E. 테스트

- 기존 단위 86건, 통합 47건이 계속 통과해야 한다. 새 통합 테스트는 `t_it_` 스키마와 실제 `pmis.tenant` 행 불변 확인 방식을 따른다.

### F. 문서·마무리

- `doc/api/mdm-api.md`, `doc/cursor-progress.md` 갱신.
- 커밋: `[9.6] 요약`. 보고서: `docs/reports/9.6.md` 와 `9.6.diff` (`.diff` 는 추적하지 않음). a–e 형식에 git 상태 줄 포함. d절 질문에 시드 반영 방식과 한계를 쓴다.
- push 하지 않는다. 사용자가 "push 해줘" 라고 할 때만 한다.

## 9.7단계: 개발 전용 관리자 계정 설정 명령

사용자 요청(2026-10-29): 로컬 화면 테스트를 위해 관리자 계정의 로그인 아이디와 비밀번호를 쉬운 값으로 정해 쓰고 싶다. 사용자가 정한 값은 `admin` / `password` 이다. 이 값은 비밀번호 정책(길이 12, 문자 종류 3)을 어기는 약한 값이므로, **개발 전용 명령**으로만 허용하고 운영 경로에서는 절대 동작하지 않게 한다. 이 단계만 수행하고 멈춘다.

시작 전: `main` 으로 전환, `git pull --ff-only`, Claude 가 쓴 파일(지시서, `docs/reviews/*`)이 변경 상태면 먼저 `docs:` 커밋으로 올린다. 작업 방식은 이 파일 맨 위 "작업 방식" 블록을 따른다.

### A. 원칙

- 로그인 아이디와 비밀번호 값은 **코드, 시드, 설정 파일, 문서, 테스트 코드, 로그, 보고서에 적지 않는다.** 사용자가 자기 PC 의 환경변수로만 준다(아래 B). 테스트는 무작위로 만든 값을 쓴다.
- 개발 프로필(`dev`, `local` 포함)에서만 실행된다. 운영에서 켜질 수 있는 설정 키를 만들지 않는다(기능 플래그 없음, 프로필로만 판단).
- 기존 비밀번호 정책 검증을 이 명령에서만 건너뛴다. 로그인 API, 비밀번호 변경 API, 사용자 등록 API 의 정책 검증은 건드리지 않는다.
- 웹 서버를 띄우지 않는 일회성 명령이다(기존 `--bootstrap-platform-admin` 과 같은 방식: 끝나면 프로세스 종료). 비밀번호 해시 알고리즘과 저장 방식은 기존 계정 생성과 같은 것을 재사용한다(BCrypt 등 기존 `PasswordEncoder`).

### B. 명령

`gradlew.bat bootRun --args="--dev-set-admin"` (기존 부트스트랩 명령 옆에 같은 구조로 만든다.)

환경변수(이 프로세스에만 둔다. 값은 문서에 쓰지 않는다):

| 이름 | 필수 | 의미 |
| --- | --- | --- |
| `MDM_DEV_ADMIN_TARGET` | 필수 | `platform` 또는 `tenant` |
| `MDM_DEV_ADMIN_TENANT_CODE` | target=tenant 일 때 선택 | 테넌트 코드. 비우면 `ACTIVE` 테넌트가 정확히 1개일 때 그 테넌트를 쓰고, 0개 또는 2개 이상이면 거절한다(후보 코드를 출력) |
| `MDM_DEV_ADMIN_LOGIN` | 필수 | 로그인 아이디 |
| `MDM_DEV_ADMIN_PASSWORD` | 필수 | 비밀번호 |

공통 검사(하나라도 어긋나면 아무것도 바꾸지 않고 이유를 한 줄로 출력하고 0이 아닌 코드로 종료):
 1. 활성 프로필에 `dev` 가 포함되어 있어야 한다. 아니면 거절.
 2. 데이터베이스 이름이 `pmis` 인지 확인한다(기존 부트스트랩과 같은 검사).
 3. 환경변수 누락이나 빈 값이면 거절. 로그인 아이디는 소문자로 바꿔 저장한다(사용자 계정 규칙: 영문·숫자·`._@-` 3~100자). 비밀번호는 비어 있지 않고 BCrypt 한도(72바이트) 이내인지만 본다.
 4. 출력에 비밀번호를 쓰지 않는다. 로그인 아이디는 출력해도 된다. 결과 한 줄(대상, target=tenant 이면 실제로 적용한 테넌트 코드, 만들었는지/바꿨는지)만 출력한다.

target=platform:
 - `platform_admin` 에 같은 로그인 아이디가 있으면 그 계정의 비밀번호 해시를 바꾸고, 없으면 새로 만든다(이름·이메일은 로그인 아이디로 채운다).
 - 계정 상태를 정상으로 되돌린다: 잠금 해제, 실패 횟수 0, 사용 여부 Y. `must_change_password_yn='N'`, 비밀번호 만료 시각은 충분히 먼 미래(예: 10년 뒤) 또는 null(스키마가 허용하는 쪽)로 둔다.
 - 기존 refresh 토큰이 있으면 철회한다.

target=tenant:
 - 테넌트 코드(생략 시 위 규칙의 유일한 ACTIVE 테넌트)로 `pmis.tenant` 를 찾고 상태가 ACTIVE 이며 스키마가 있어야 한다. 아니면 거절. 그 테넌트 스키마 컨텍스트에서 실행한다(요청 필터와 같은 방식. 끝나면 반드시 컨텍스트 해제).
 - `user_account` 에 같은 로그인 아이디가 있으면 비밀번호 해시를 바꾸고, 없으면 새로 만든다(계정 유형 NORMAL, 사람 연결 없음, 언어·시간대는 테넌트 기본값, 상태 ACTIVE).
 - 두 경우 모두 `TENANT_ADMIN` 역할이 현재 유효하게 부여되어 있게 한다(없으면 부여, 철회돼 있으면 되살림, 기간이 지났으면 종료일을 비움).
 - 상태 정상, 실패 횟수 0, 잠금 해제, 사용 여부 Y, `must_change_password_yn='N'`, 비밀번호 만료 먼 미래 또는 null, refresh 철회, 이력 테이블이 있으면 사유 `RESET` 같은 기존 코드로 한 줄.
 - 권한 캐시나 계정 상태 캐시는 별도 프로세스라 영향이 없다. 실행 중인 서버가 있다면 캐시 TTL(60초) 동안 이전 상태가 보일 수 있다는 점을 문서에 한 줄 적는다.
 - 계정 삭제·역할 제거 같은 파괴적인 동작은 하지 않는다.

### C. 테스트

- 단위: 환경변수 검증(누락, 빈 값, 로그인 아이디 규칙, 72바이트 초과), 프로필 검사(dev 가 없으면 거절). 모든 값은 무작위 문자열.
- 통합(`t_it_` 스키마, 실제 `pmis.tenant` 행 불변 확인 방식 유지): 
  - target=tenant 로 새 계정을 만들면 그 값으로 로그인 API 가 성공하고 비밀번호 변경 강제가 없다(`passwordChangeRequired=false`), 정책 위반인 짧은 비밀번호로도 만들어진다.
  - 같은 아이디가 이미 있으면 비밀번호만 바뀌고 계정 수가 늘지 않는다. 잠금 상태 계정이 정상으로 돌아오고 `TENANT_ADMIN` 역할이 부여된다.
  - 존재하지 않는 테넌트, 사용 중지 테넌트는 거절되고 아무것도 바뀌지 않는다.
  - target=platform 은 공유 `platform_admin` 을 건드리지 않도록 테스트에서는 기존 부트스트랩 통합 테스트와 같은 방식으로 격리한다(격리가 불가능하면 platform 은 단위 수준 검증만 하고 이유를 보고서에 쓴다).
  - 실행 후 출력과 로그에 사용한 비밀번호 문자열이 없다.
- 기존 단위 86건 이상, 통합 50건 이상이 계속 통과해야 한다.

### D. 문서

- `backend/common/doc/local-setup.md` 에 "개발용 관리자 계정 설정" 절을 추가한다. 환경변수 이름과 예시 명령(PowerShell 에서 `$env:이름='<여기에 입력>'` 형식의 자리표시자)만 쓰고 실제 값은 쓰지 않는다. 개발 전용이며 운영·공유 환경에서는 쓰지 말라는 경고, 실행 뒤 환경변수를 지우는 방법(`Remove-Item Env:...`)을 쓴다.
- `backend/common/doc/platform-admin-bootstrap.md` 끝에 이 명령으로 이어지는 링크 한 줄.
- `doc/cursor-progress.md` 갱신.

### E. 마무리

- 커밋: `[9.7] 요약`. 보고서: `docs/reports/9.7.md` 와 `9.7.diff` (`.diff` 는 추적하지 않음). a–e 형식에 git 상태 줄 포함. 보고서에 어떤 값도 적지 않는다.
- push 하지 않는다. 사용자가 "push 해줘" 라고 할 때만 한다.

## 9.8단계: 지원 접속 (플랫폼 운영자가 테넌트에 들어가 검증)

사용자 결정(2026-10-29): 권한은 테넌트 관리자 전체, 사유는 선택 입력(비면 접속 일시 + 담당자 로그인 ID), 모든 테넌트에서 항상 허용, 접속 기록 조회 가능, 계정 관련 기능도 허용. 설계는 `backend/common/doc/auth-design.md` 의 "지원 접속" 절이다(먼저 끝까지 읽는다). 이 단계만 수행하고 멈춘다. 프론트는 이 단계가 승인된 뒤 F12 에서 한다.

시작 전: `main` 으로 전환, `git pull --ff-only`, Claude 가 쓴 파일(지시서, `docs/reviews/*`, `auth-design.md`)이 변경 상태면 먼저 `docs:` 커밋으로 올린다. 작업 방식은 이 파일 맨 위 "작업 방식" 블록을 따른다.

### A. 사전 확인 (코드 수정 전에 보고서 초안에 적는다)

설계와 현재 코드가 어긋나는 곳을 찾는다. 작은 차이는 코드 쪽에 맞추고 보고서 "질문"에 적는다. 구조가 달라 설계를 못 따르는 경우에만 멈춘다. 확인할 것:
 1. `pmis.tenant` 의 키 컬럼과 `tenant_domain` 의 대표 도메인 컬럼 이름.
 2. `user_account` 의 NOT NULL 컬럼과 비밀번호 해시 컬럼 제약(시스템 사용자 행을 어떻게 넣을지), 상태·계정 유형 코드 값.
 3. `TENANT_ADMIN` 역할과 시드가 어느 스크립트(마이그레이션 또는 `S0x`)에서 들어가는지, 사용자 역할 부여 테이블 구조.
 4. 마지막 관리자 판정(`USER.LAST_ADMIN`)이 어떤 쿼리를 쓰는지, 사용자 목록·검색·사람 연결 선택·역할 보유자 수가 어디서 나오는지.
 5. 현재 Access 필터 체인 순서, `CurrentTenantUser` 가 `sub` 로 무엇을 읽는지, `DbPermissionChecker` 가 역할을 읽는 경로.
 6. 플랫폼 운영자 비활성화·삭제 API 가 있는지.

### B. DB

 1. 공용 스키마 새 마이그레이션(다음 번호): 설계의 두 테이블 `tenant_support_access`, `tenant_support_access_action`. 기본 키, `status` 체크(`ISSUED`/`ACTIVE`/`ENDED`/`EXPIRED`/`REVOKED`), 인덱스(테넌트+시작시각 내림차순, 운영자+시작시각, `ticket_hash` 유일, action 은 `support_access_pk`+발생시각). 기존 컬럼 이름 규칙과 시간 타입을 따른다. ERD 문서(`backend/common/doc/erd`)가 있으면 갱신한다.
 2. 테넌트 스키마 새 마이그레이션 또는 시드(A.3 결과에 맞춰 순서를 정한다. 반복 실행에 안전하게 `NOT EXISTS`): 지원 시스템 사용자 1행(로그인 ID `$platform-support`, 상태 ACTIVE, 사람 연결 없음, 비밀번호 해시는 어떤 입력으로도 일치하지 않는 값 또는 NULL 허용이면 NULL), `TENANT_ADMIN` 역할 부여. 메뉴 `SUPPORT_ACCESS`(접근 관리 그룹 아래, 이름 i18n 키 포함, 기존 메뉴 시드 방식 그대로)와 권한 READ 를 `TENANT_ADMIN` 에만.
 3. 기존 테넌트: `TenantUpgradeService.migrate` 를 실행하면 위 2가 적용되는지 통합 테스트로 확인한다(이미 마이그레이션 방식이면 별도 코드가 없을 수 있다. 시드가 필요하면 업그레이드 서비스가 시드를 다시 적용하는 경로를 쓴다). 새 테넌트 프로비저닝도 같은 결과가 되어야 한다.

### C. 플랫폼 API (`/api/platform/v1`, 플랫폼 토큰)

 1. `POST /tenants/{tenantCode}/support-access` 본문 `{reason?}`. 테넌트가 ACTIVE 이고 스키마가 있어야 한다(아니면 `TENANT.NOT_FOUND` 또는 `SUPPORT.TENANT_NOT_ACTIVE`). 사유는 trim 해 최대 200자(초과 `COMMON.VALIDATION_FAILED`), 비면 설계의 기본값(Asia/Seoul 시각 `yyyy-MM-dd HH:mm` + 운영자 로그인 ID)과 `reason_default_yn='Y'`. 티켓은 `SecureRandom` 256비트, URL-safe Base64, DB 에는 SHA-256 해시만. 수명 60초(`mdm.security.support-access.ticket-ttl-seconds`). 응답 `{supportAccessId, accessUrl, ticketExpireDatetime}` 에 `Cache-Control: no-store`. 티켓 원문은 응답 외 어디에도(로그, 예외, DB) 남기지 않는다.
 2. `accessUrl`: dev 프로필에서는 `{mdm.support-access.dev-base-url}/support-access?tenant={tenantCode}#ticket={ticket}` (기본 `http://localhost:8000`). 그 외에는 `{scheme}://{대표 도메인}/support-access#ticket={ticket}` (`mdm.support-access.scheme` 기본 https). 대표 도메인이 없으면 `SUPPORT.NO_DOMAIN`.
 3. `GET /support-accesses` 필터 tenantCode, platformLoginId(부분 일치. `LikeEscape` 사용), status, from, to(시작 시각 기준), 페이지네이션, 정렬 화이트리스트(`issuedDatetime` 기본 내림차순, `startedDatetime`, `tenantCode`). 응답 항목에서 `ticket_hash` 는 제외. 저장 상태가 ACTIVE 이고 만료 시각이 지났으면 응답 상태를 EXPIRED 로 계산해 준다(읽기 시 계산, 읽기 때 갱신하지 않는다).
 4. `GET /support-accesses/{id}/actions` 페이지네이션, 발생 시각 오름차순. 없으면 `SUPPORT.NOT_FOUND`.
 5. 플랫폼 운영자를 비활성화하는 경로가 있으면 그 운영자의 ISSUED/ACTIVE 행을 `REVOKED`(사유 코드 `OPERATOR_DISABLED`)로 바꾼다. 경로가 없으면 요청마다 운영자 활성 여부를 확인하는 B 쪽 검증(아래 D.4)에 맡기고 보고서에 적는다.

### D. 테넌트 API (`/api/mdm/v1`)

 1. `POST /auth/support-access/exchange` 본문 `{ticket}`. 공개 경로, `X-MDM-Request: 1` 필수(`AUTH.CSRF`), Host(또는 dev 헤더)로 정해진 테넌트와 행의 테넌트가 같아야 한다. 티켓 해시로 행을 찾고 상태 ISSUED, 만료 전이어야 한다. 실패는 사유를 구분하지 않고 `401 AUTH.SUPPORT_TICKET_INVALID`(없음, 만료, 재사용, 다른 테넌트 모두 같은 응답과 비슷한 처리 시간). 성공하면 행 상태를 한 번에(조건부 UPDATE 로 경쟁 방지) ACTIVE 로 바꾸고 `started_datetime`, `expire_datetime`(+30분, `mdm.security.support-access.ttl-minutes`), `start_ip`, `user_agent` 를 채운다. 응답 `{accessToken, expiresInSeconds, supportAccessId}`, Refresh 쿠키는 만들지 않는다, `Cache-Control: no-store`. 이 요청은 티켓 외 인증이 필요 없다.
 2. `POST /auth/support-access/end` (지원 접속 토큰 필요, 헤더 `X-MDM-Request: 1`). 행을 ENDED 로, 사유 `USER_ENDED`. 이미 끝났으면 멱등하게 성공. 지원 접속 토큰이 아니면 `403 AUTH.SUPPORT_FORBIDDEN`.
 3. `GET /support-accesses`, `GET /support-accesses/{id}/actions`: `@RequirePermission(SUPPORT_ACCESS, READ)`. 현재 테넌트의 행만 나오도록 서버에서 테넌트 코드 조건을 강제한다(요청 파라미터로 바꿀 수 없다). 다른 테넌트 id 는 `SUPPORT.NOT_FOUND`. 응답 항목: 접속 id, 운영자 로그인 ID, 사유, 기본 사유 여부, 상태, 발급·시작·만료·종료 시각, 종료 사유, 접속 IP, 쓰기 요청 건수. 이 API 는 pmis 공용 스키마를 읽는다(테넌트 컨텍스트와 별개 조회. 연결·스키마 전환이 꼬이지 않게 기존 플랫폼 서비스가 공용 테이블을 읽는 방식을 따른다).
 4. `GET /auth/me` 응답에 `support` 객체를 추가한다. 일반 토큰이면 `null`. 지원 접속 토큰이면 `{supportAccessId, operatorLoginId, expireDatetime}` 이고 사용자 표시 이름은 `플랫폼 지원 ({운영자 로그인 ID})`.

### E. 토큰과 요청 검증

 1. 발급: 기존 Access 발급기를 재사용해 `typ=tenant-user`, `aud=mdm-api`, `tenant`, `sub`=시스템 사용자 `user_pk`, `sup`=접속 id, `act`=운영자 `platform_admin_pk`, 수명 30분. `pwd_change_required` 는 항상 false.
 2. 매 요청 검증: `sup` 가 있는 토큰이면 (a) 행이 같은 테넌트의 ACTIVE 이고 만료 전인지, (b) 운영자가 활성(`use_yn='Y'`, 활성 상태)인지 본다. 짧은 캐시(60초 이내, 종료·폐기 때 즉시 삭제)를 둘 수 있다. 실패는 `401 AUTH.SUPPORT_ENDED`. 만료된 ACTIVE 행은 이 시점에 EXPIRED 로 갱신해도 된다.
 3. 지원 접속 토큰으로 막는 것: 자기 비밀번호 변경(`AUTH.SUPPORT_FORBIDDEN`), `/auth/refresh`(쿠키가 없으니 기존대로 실패하면 된다. 별도 처리 불필요), 지원 접속 재발급(플랫폼 API 라 `typ` 로 이미 막힘. 테스트로 확인). 나머지는 `TENANT_ADMIN` 과 같이 전체 허용이다(`DbPermissionChecker` 를 따로 우회하지 않고 시스템 사용자의 `TENANT_ADMIN` 역할로 통과시킨다).
 4. 쓰기 요청 기록 필터: `sup` 토큰의 POST/PUT/PATCH/DELETE 요청은 처리 전에 `tenant_support_access_action` 행을 별도 트랜잭션(REQUIRES_NEW)으로 넣고(경로는 쿼리 제외, `sensitive_yn` 은 비밀번호 초기화·재발급, 사용자 생성, 역할 부여·교체, 권한 변경, 메뉴 변경 경로), 처리 후 같은 행의 `status_code` 를 갱신한다. 처리 전 삽입이 실패하면 요청을 처리하지 않고 `503 SUPPORT.AUDIT_UNAVAILABLE` 로 거절한다(기록 없는 변경 금지). 요청 본문과 응답 본문은 저장하지 않는다. 종료 요청(`end`)은 기록 대상에서 뺀다.

### F. 시스템 사용자 보호

 1. 시스템 사용자는 사용자 목록, 검색, 연결할 사람 선택, 역할 보유자 수에 나오지 않는다. 상세·수정·삭제·복구·상태 변경·역할 교체·비밀번호 초기화는 `USER.SYSTEM_ACCOUNT`(409 또는 기존 규칙에 맞는 상태 코드)로 거절한다. 사용자 목록 총 건수와 `TENANT_ADMIN` 보유자 수에도 포함하지 않는다. **마지막 관리자 판정에서 시스템 사용자를 세지 않는다**(세면 관리자가 한 명도 없어도 통과하는 구멍이 생긴다. 테스트 필수).
 2. 로그인: 이 로그인 ID 로는 어떤 경로로도 로그인할 수 없다. 응답은 잘못된 자격 증명과 같은 오류, 같은 처리 시간 규칙을 쓴다. 로그인 실패 이력 기록도 일반 실패와 같다.
 3. `pwd_change_required`, 비밀번호 만료, 잠금 판정이 시스템 사용자에게 적용되지 않게 한다(요청 검증이 `sup` 경로로 처리).

### G. 테스트

 - 단위: 사유 기본값(비어 있음, 공백, 200자 초과), 티켓 해시, 만료 계산, 민감 경로 판정, 상태 전이.
 - 통합(`t_it_` 스키마, 실제 `pmis.tenant` 행 불변 방식 유지. 지원 접속 테이블 행은 테스트가 만들고 지운다):
   1. 플랫폼 로그인 → 발급 → 교환 → 지원 토큰으로 `GET /me`(support 채워짐), 사용자 등록(임시 비밀번호 발급), 역할 교체, 권한 확인 → 쓰기 요청 기록 행에 경로와 `sensitive_yn` 이 맞게 남음, 조회 요청은 남지 않음.
   2. 티켓 재사용, 만료(시계 조작), 다른 테넌트 Host, 변조 티켓 → 모두 같은 401.
   3. 종료 후 같은 토큰 → `AUTH.SUPPORT_ENDED`. 만료 후, 운영자 비활성화 후도 같음.
   4. 지원 토큰으로 자기 비밀번호 변경 거절, 플랫폼 API 호출 거절, 지원 재발급 불가.
   5. 시스템 사용자: 목록·검색·선택에 없음, 수정·삭제·재발급 거절, 로그인 불가, 마지막 관리자 판정에 포함되지 않음(테넌트 관리자가 1명일 때 그 관리자 삭제가 여전히 `USER.LAST_ADMIN`).
   6. 조회 권한: `TENANT_ADMIN` 은 자기 테넌트 기록만 보고, 다른 역할은 403, 다른 테넌트 기록 id 는 404. 플랫폼 목록 필터(테넌트, 운영자 부분 일치 + 와일드카드 문자, 상태, 기간)와 정렬 화이트리스트.
   7. 기록 삽입 실패 시뮬레이션(액션 테이블 잠금 또는 목) → 쓰기 요청 503, 데이터 변경 없음.
   8. `TenantUpgradeService.migrate` 로 기존 형태의 테넌트에 시스템 사용자·메뉴·권한이 생긴다.
   9. 감사 컬럼: 지원 접속 중 만든 데이터의 `created_by` 가 시스템 사용자 `user_pk`.
   10. 티켓·토큰·`Authorization` 값이 로그 출력에 없다(로그 캡처).
 - 기존 단위·통합 테스트는 계속 통과해야 한다.

### H. 문서

 - `backend/common/doc/api/mdm-api.md` 와 플랫폼 API 문서에 새 엔드포인트, 오류 코드(`AUTH.SUPPORT_TICKET_INVALID`, `AUTH.SUPPORT_ENDED`, `AUTH.SUPPORT_FORBIDDEN`, `USER.SYSTEM_ACCOUNT`, `SUPPORT.*`)를 추가한다. 설정 키(`mdm.security.support-access.ticket-ttl-seconds`, `.ttl-minutes`, `mdm.support-access.scheme`, `.dev-base-url`)를 `local-setup.md` 에 적는다.
 - `auth-design.md` 와 구현이 달라진 곳이 있으면 "지원 접속" 절 끝에 "구현 차이" 항목으로만 적는다(본문은 고치지 않는다).
 - 기존 테넌트에 적용하는 방법(`TenantUpgradeService.migrate` 실행 방법)을 `local-setup.md` 에 한 줄 이상 적는다.
 - `doc/cursor-progress.md` 갱신.

### I. 마무리

 - 커밋: `[9.8] 요약`. 보고서: `docs/reports/9.8.md` 와 `9.8.diff` (`.diff` 는 추적하지 않음). a–e 형식에 git 상태 줄 포함. 보고서에 티켓, 토큰, 비밀번호 값을 적지 않는다.
 - push 하지 않는다. 사용자가 "push 해줘" 라고 할 때만 한다.

## 9.9단계: 지원 접속 보완 (9.8 검토 반영)

`docs/reviews/2026-10-29-9.8.md` 의 보완 3건이다. 이 단계만 수행하고 멈춘다. 시작 전: `main` 으로 전환, `git pull --ff-only`, Claude 가 쓴 파일이 변경 상태면 먼저 `docs:` 커밋. 작업 방식은 맨 위 "작업 방식" 블록을 따른다.

 1. `SupportAccessService.requireSession` 의 60초 캐시: 의도를 정한다. 종료·폐기·운영자 비활성화·만료가 다른 경로(직접 DB 변경 포함)로 일어나도 즉시 반영되게 하려면 캐시를 없앤다(요청마다 1쿼리). 성능상 두고 싶으면 TTL 을 5초 이하로 줄이고 종료·폐기 코드 경로가 항상 캐시를 지우는지 테스트한다. 결정과 이유를 `auth-design.md` "구현 차이"와 보고서에 적는다. (지난 보고서는 "캐시를 두지 않았다"고 했으나 실제로는 있었다. 보고서와 코드가 어긋나지 않게 한다.)
 2. `SupportAuditStore.failNextInsert()` 같은 테스트용 훅을 운영 클래스에서 없앤다. 테스트 소스에서 `SupportAuditStore` 를 대체하는 테스트 전용 빈(`@TestConfiguration` 또는 `@MockitoSpyBean`)으로 같은 시나리오(감사 기록 실패 → 503, 데이터 변경 없음)를 검증한다.
 3. 만료된 ACTIVE 행 정리: 그 토큰으로 다시 요청이 오지 않으면 영원히 ACTIVE 로 저장된다. 다음 중 하나로 정한다(권장: 둘 다 최소로). (a) 접속 기록 조회(플랫폼·테넌트)가 만료된 ACTIVE 행을 읽기 전에 EXPIRED 로 갱신하는 한 번의 UPDATE(`expire_datetime < now`), 또는 (b) 새 티켓 교환 때 같은 테넌트의 만료 행을 정리. 이 UPDATE 는 `end_reason_code='EXPIRED'`, `ended_datetime=expire_datetime` 으로 채운다. 읽기 API 가 쓰기를 하게 되는 점을 보고서에 적는다.
 4. 마무리: 단위·통합 전부 통과를 확인하고, 커밋 `[9.9] 요약`, 보고서 `docs/reports/9.9.md` 와 `9.9.diff`(UTF-8). push 하지 않는다.

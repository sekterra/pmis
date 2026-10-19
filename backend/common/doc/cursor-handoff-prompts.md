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

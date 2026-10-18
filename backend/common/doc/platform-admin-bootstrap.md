# 플랫폼 운영자 부트스트랩

공유 데이터베이스 이름이 `pmis`이고 공용 스키마에 `platform_admin` 테이블이 있을 때 한 번만 실행한다.

```text
--bootstrap-platform-admin
```

필수 환경변수는 `MDM_BOOTSTRAP_ADMIN_LOGIN`이다. `MDM_BOOTSTRAP_ADMIN_NAME`과 `MDM_BOOTSTRAP_ADMIN_EMAIL`은 생략할 수 있다. 생략하면 로그인 아이디를 이름과 이메일에 넣는다.

동작:

- 웹 서버는 뜨지 않고 끝나면 프로세스가 종료된다.
- `platform_admin` 행이 0건일 때만 계정을 만든다. 이미 있으면 아무 행도 바꾸지 않고 0이 아닌 코드로 종료한다.
- 임시 비밀번호는 20자, 문자 4종류이며 표준 출력에 한 줄만 나온다. 로그 프레임워크는 쓰지 않는다. 데이터베이스에는 해시만 저장한다.
- `must_change_password_yn`은 `Y`다. `password_expire_datetime`은 지금으로부터 `mdm.security.platform.temp-password-valid-days`(기본 3일) 뒤다.

운영 시 표준 출력을 로그 파일로 연결하지 않는다. 출력된 비밀번호는 그 실행에서만 확인하고, 바로 플랫폼 로그인으로 비밀번호를 바꾼다. 채팅, 티켓, 로그에 평문을 붙이지 않는다.

## 개발 DB에서 첫 운영자를 만드는 순서

값은 적지 않는다. 이름만 이 프로세스에 설정한다.

1. 데이터베이스 이름이 `pmis`인지 조회만 한다. `SELECT current_database();` 결과가 `pmis`여야 한다.
2. 접속은 `config/application-local.yaml`의 `mdm.db.url`, `mdm.db.username`, `mdm.db.password` 또는 같은 값의 환경변수 `MDM_DB_URL`, `MDM_DB_USERNAME`, `MDM_DB_PASSWORD`로 준다. 환경변수가 있으면 파일보다 우선한다. 값은 적지 않는다.
3. 이 프로세스에만 다음 환경변수를 넣는다.
   - `MDM_BOOTSTRAP_ADMIN_LOGIN` (필수)
   - `MDM_BOOTSTRAP_ADMIN_NAME` (선택. 없으면 로그인 아이디)
   - `MDM_BOOTSTRAP_ADMIN_EMAIL` (선택. 없으면 로그인 아이디)
4. `backend/common`에서 웹 서버 없이 한 번 실행한다. `bootRun`은 프로필을 지정하지 않으면 `local`이다.

```text
gradlew.bat bootRun --args="--bootstrap-platform-admin"
```

5. 표준 출력 한 줄을 그 자리에서만 확인한다. 표준 출력을 로그 파일로 돌리지 않는다.
6. 그 비밀번호로 플랫폼에 로그인한 뒤 바로 비밀번호를 바꾼다.
7. `platform_admin` 행이 이미 있으면 계정은 만들지 않고 0이 아닌 코드로 끝난다. 다시 실행해도 기존 행은 바꾸지 않는다.

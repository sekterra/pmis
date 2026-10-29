# 로컬 기동

값은 이 문서에 적지 않는다. 키 이름과 `<여기에 입력>` 자리표시자만 사용한다.

## 1. 로컬 파일

작업 디렉터리는 `backend/common`이다. IDE에서 실행할 때도 같은 디렉터리를 작업 디렉터리로 둔다. 저장소 루트에서 실행하면 `config/application-local.yaml`을 찾지 못하고 환경변수만 사용한다. 파일 경로는 프로세스의 작업 디렉터리 기준 `config/application-local.yaml`이다.

```text
copy config\application-local.yaml.example config\application-local.yaml
```

`config/application-local.yaml`에 `mdm.db.url`, `mdm.db.username`, `mdm.db.password`를 채운다. URL 형식은 `jdbc:postgresql://<host>:<port>/pmis`이다. 이 파일은 git에 올리지 않는다. 공유 폴더나 클라우드 동기화 폴더에 두지 않는다.

`spring.flyway.enabled`는 예시에서 `false`다. 마이그레이션이 필요할 때만 `true`로 바꾼다. 바꾸면 기동 시 Flyway가 실행된다.

## 2. 실행

```text
gradlew.bat bootRun
```

`bootRun`은 프로필을 지정하지 않으면 `local`이다. `local`은 `dev` 설정을 포함한다. 포트 기본값은 8080이다. `--server.port` 또는 로컬 파일의 `server.port`로 바꿀 수 있다.

`MDM_PLATFORM_HOSTS`는 플랫폼 콘솔로 볼 호스트 목록이다. 쉼표로 구분하고 포트는 비교하지 않는다. `dev` 기본값은 `localhost,127.0.0.1`이다. 로컬에서 `X-Tenant-Code`가 없으면 `GET /api/mdm/v1/context`는 플랫폼이고, 헤더가 있으면 그 테넌트다. 목록이 비어 있으면 플랫폼 콘솔 제한 없이 기동한다.

## 3. 환경변수 우선

같은 값이 환경변수와 파일에 있으면 환경변수가 우선한다.

- `MDM_DB_URL` → `spring.datasource.url` (`mdm.db.url`보다 우선)
- `MDM_DB_USERNAME` → `spring.datasource.username`
- `MDM_DB_PASSWORD` → `spring.datasource.password`

## 4. 다른 프로필

`SPRING_PROFILES_ACTIVE`가 있으면 `bootRun`은 `local`을 강제하지 않는다. 인자로 준 프로필이 더 우선한다.

```powershell
$env:SPRING_PROFILES_ACTIVE = '<profile>'
.\gradlew.bat bootRun --args='--spring.profiles.active=<profile>'
```

jar를 프로필 없이 실행하면 `local`과 `dev`가 켜지지 않는다. `X-Tenant-Code`와 임시 서명 키는 `dev`가 포함된 프로필에서만 동작한다.

## 5. Windows PowerShell

```powershell
Set-Location backend\common
Copy-Item config\application-local.yaml.example config\application-local.yaml
.\gradlew.bat bootRun
```

값을 채운 뒤 실행한다. 명령 기록에 접속 값을 남기지 않는다.

## 6. 플랫폼 관리자 부트스트랩

같은 `config/application-local.yaml`을 쓴다. 데이터베이스 이름이 `pmis`인지 검사는 유지한다.

```text
gradlew.bat bootRun --args="--bootstrap-platform-admin"
```

로그인 아이디는 환경변수 `MDM_BOOTSTRAP_ADMIN_LOGIN`이다. 이름과 이메일은 `MDM_BOOTSTRAP_ADMIN_NAME`, `MDM_BOOTSTRAP_ADMIN_EMAIL`이며 생략할 수 있다. 임시 비밀번호는 그 실행의 표준 출력에만 나온다.

## 7. 개발용 관리자 계정 설정

개발 프로필에서만 동작하는 일회성 명령이다. 운영이나 공유 환경에서는 쓰지 않는다. 비밀번호 정책을 건너뛰므로 로컬 화면 확인용이다. 웹 서버는 뜨지 않고 끝나면 프로세스가 종료된다. 값은 이 문서에 적지 않는다.

`MDM_DEV_ADMIN_TARGET`은 `platform` 또는 `tenant`다. `tenant`일 때 `MDM_DEV_ADMIN_TENANT_CODE`는 비워도 된다. 비우면 사용 중(`ACTIVE`) 테넌트가 하나일 때만 그 테넌트를 쓰고, 없거나 둘 이상이면 후보 코드를 출력하고 끝난다. `MDM_DEV_ADMIN_LOGIN`과 `MDM_DEV_ADMIN_PASSWORD`는 항상 필요하다. 실행 중인 서버가 있으면 권한·계정 상태 캐시는 이 명령이 지우지 않는다. 캐시 TTL(60초) 동안 이전 상태가 보일 수 있다.

```powershell
$env:MDM_DEV_ADMIN_TARGET = '<여기에 입력>'
$env:MDM_DEV_ADMIN_TENANT_CODE = '<여기에 입력>'
$env:MDM_DEV_ADMIN_LOGIN = '<여기에 입력>'
$env:MDM_DEV_ADMIN_PASSWORD = '<여기에 입력>'
.\gradlew.bat bootRun --args="--dev-set-admin"
Remove-Item Env:MDM_DEV_ADMIN_TARGET
Remove-Item Env:MDM_DEV_ADMIN_TENANT_CODE
Remove-Item Env:MDM_DEV_ADMIN_LOGIN
Remove-Item Env:MDM_DEV_ADMIN_PASSWORD
```

## 8. 통합 테스트

통합 테스트는 로컬 파일을 쓰지 않는다. 환경변수 `MDM_TEST_DB_URL`, `MDM_TEST_DB_USERNAME`, `MDM_TEST_DB_PASSWORD`만 사용한다.

## 9. 파일 저장

`mdm.file.local-root`가 비어 있으면 기본 LOCAL 저장소를 자동으로 만들지 않는다. 업로드는 `409 FILE.NO_DEFAULT_STORAGE`다. `dev` 프로필은 작업 디렉터리의 `build/mdm-files`를 쓴다. 이 경로는 저장소 작업 디렉터리 안이다.

| 키 | 기본 | 의미 |
| --- | --- | --- |
| `mdm.file.local-root` | 비움. 환경변수 `MDM_FILE_LOCAL_ROOT` | LOCAL 상대 경로의 기준. 비밀 값이 아니다 |
| `mdm.file.max-size-mb` | 50. `MDM_FILE_MAX_SIZE_MB` | 애플리케이션 업로드 상한. 스프링 multipart 한도와 같다 |
| `mdm.file.allowed-extensions` | `png,jpg,jpeg,gif,webp,pdf,zip,txt,csv` | 저장소 목록이 없을 때의 확장자 |
| `mdm.file.delete-retention-days` | 7 | 논리 삭제 후 디스크를 지울 때까지의 일수 |
| `mdm.file.temp-ttl-hours` | 24 | TEMP 파일을 정리 대상으로 보는 시간 |
| `mdm.file.cleanup.enabled` | `false`. `MDM_FILE_CLEANUP_ENABLED` | 정리 스케줄러. 운영에서만 켠다. `dev`는 꺼 둔다 |
| `mdm.file.cleanup.cron` | `0 0 3 * * *`. `MDM_FILE_CLEANUP_CRON` | 스케줄러가 켜졌을 때의 실행 시각 |
| `mdm.file.cleanup.zone` | `Asia/Seoul`. `MDM_FILE_CLEANUP_ZONE` | 크론 시간대 |

정리 스케줄러는 단일 인스턴스 전제다. 서버를 여러 대 띄우면 같은 정리가 중복될 수 있다.

연결 설정과 `secret_ref`에는 비밀 값을 넣지 않는다. `secret_ref`는 외부 비밀 이름의 형식만 검사한다.

## 10. 지원 접속

플랫폼 운영자가 짧은 티켓으로 테넌트 토큰을 받는다. 티켓은 60초, 세션은 30분이다. 값은 비밀이 아니다.

| 키 | 기본 | 의미 |
| --- | --- | --- |
| `mdm.security.support-access.ticket-ttl-seconds` | 60. `MDM_SUPPORT_TICKET_TTL_SECONDS` | 일회용 티켓 유효 시간 |
| `mdm.security.support-access.ttl-minutes` | 30. `MDM_SUPPORT_ACCESS_TTL_MINUTES` | 교환 후 세션 시간. 연장과 refresh는 없다 |
| `mdm.support-access.scheme` | `https`. `MDM_SUPPORT_ACCESS_SCHEME` | `dev`가 아닐 때 대표 도메인 주소의 스킴 |
| `mdm.support-access.dev-base-url` | `http://localhost:8000`. `MDM_SUPPORT_ACCESS_DEV_BASE_URL` | `dev` 프로필의 프론트 주소 |

이미 만들어진 테넌트에는 시스템 사용자 `$platform-support`, 메뉴 `SUPPORT_ACCESS`, 그 READ 권한이 없다. `TenantUpgradeService`가 테넌트 마이그레이션 뒤에 시드를 다시 실행하므로, 플랫폼의 테넌트 업그레이드 API로 넣는다. 시드는 없는 행만 추가한다. 이 문서는 그 명령을 공유 DB에 실행하지 않는다.


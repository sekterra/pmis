# 프론트엔드 로컬 실행

비밀번호, 토큰, 계정은 설정 파일과 이 문서에 넣지 않는다. 개발 서버 주소와 테넌트 코드만 둔다.

## 1. 로컬 환경 파일

`frontend/.env.local.example`을 `frontend/.env.local`로 복사하고 `DEV_BACKEND_URL`을 채운다. 테넌트 앱에서 개발용 `X-Tenant-Code`가 필요하면 `DEV_TENANT_CODE`도 채운다.

Umi는 기동 시 `.env`를 읽은 다음 `.env.local`을 읽는다. 파일에 있는 키는 셸에 같은 이름이 있어도 파일 값으로 덮어쓴다. 파일에 없는 키만 셸 값이 남는다. `.env.local`은 git에 올리지 않는다.

## 2. 앱을 띄우기

Node는 22.18.0 이상이어야 한다. `node -v`로 확인한다. 23.0부터 23.5는 타입 제거가 기본이 아니므로 쓰지 않는다.

백엔드를 먼저 띄운 뒤 프론트를 띄운다. 개발 서버는 `npm run dev` 하나이고 기본 포트는 8000이다.

`DEV_TENANT_CODE`가 있으면 `/api/mdm`에 `X-Tenant-Code`가 붙고 화면은 그 테넌트다. 비우면 개발용 플랫폼 주소로 플랫폼 콘솔이 된다. 플랫폼 콘솔만 쓸 때는 비우고, 지원 접속 새 탭은 주소의 `?tenant=`로 테넌트를 정하며(교환·이후 `/api/mdm` 요청), `/api/platform`에는 이 헤더를 붙이지 않는다. `DEV_TENANT_CODE`와 다른 테넌트로 지원 접속할 때는 요청에 이미 있는 `X-Tenant-Code`를 프록시가 덮어쓰지 않는다. `PORT`를 셸이나 `.env.local`에 적으면 8000보다 우선한다. 파일에 `PORT`가 있으면 셸의 `PORT`보다 파일 값이 우선한다.

지정한 포트가 이미 쓰이고 있으면 개발 서버는 다른 포트로 넘어가지 않고 바로 끝난다. 안내 문구는 `이미 다른 개발 서버가 떠 있을 수 있습니다. 8000 포트를 확인하세요`이다.

`DEV_BACKEND_URL`이 없으면 시작 로그에 다음 한 줄이 나온다.

`백엔드 주소가 없어 /api 요청이 프록시되지 않습니다. frontend/.env.local에 DEV_BACKEND_URL을 설정하세요`

## 3. PowerShell에서 npm이 막힐 때

실행 정책 때문에 `npm`이 막히면 `npm.cmd`를 쓰거나, 현재 사용자만 바꾸려면 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`를 실행한다.

## 4. 백엔드를 먼저 띄운다

`backend/common`에서 `gradlew.bat bootRun`으로 띄운다. `SPRING_PROFILES_ACTIVE`가 비어 있으면 local 프로파일(dev 포함)을 쓴다. 접속 값은 백엔드 `config/application-local.yaml` 또는 환경 변수로만 두고, 이 문서에 옮기지 않는다.

## 5. 예전 개발 서버가 포트를 잡고 있을 때

8000을 쓰는 예전 `npm run dev` 프로세스를 먼저 끝낸다. 새 서버는 그 포트를 비운 뒤에만 시작한다.

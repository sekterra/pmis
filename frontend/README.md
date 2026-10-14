# PMIS 프론트엔드

테넌트 사용자 앱과 플랫폼 운영 콘솔을 하나의 Ant Design Pro 앱으로 둡니다. 기본 언어는 `ko-KR`입니다.

## 실행

```bash
npm install
npm run dev
```

로컬 실행은 `doc/local-setup.md`를 따른다. `.env.local.example`을 `.env.local`로 복사하고 값을 채운다. 비밀번호, 토큰, 계정은 넣지 않는다.

- `npm run dev`: 테넌트 사용자 앱, 기본 포트 8000
- `npm run dev:platform`: 플랫폼 운영 콘솔, 기본 포트 8002
- `DEV_BACKEND_URL`: 개발 서버가 `/api/mdm`, `/api/platform`을 넘길 주소. 없으면 프록시를 만들지 않고 시작 로그에 한 줄 안내가 나온다.
- `DEV_TENANT_CODE`: 있을 때만 개발 프록시가 `/api/mdm`에 `X-Tenant-Code`를 붙인다. `/api/platform`에는 붙이지 않는다.
- `PORT`: 적으면 8000/8002보다 우선한다. 파일에 있으면 셸 값보다 파일 값이 우선한다.
- `APP_MODE`: 스크립트가 넣는다. `dev`는 tenant, `dev:platform`은 platform이다.

PowerShell에서 `npm`이 실행 정책으로 막히면 `npm.cmd`를 쓰거나 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`를 실행한다. 백엔드는 프론트보다 먼저 `backend/common`에서 `gradlew.bat bootRun`으로 띄운다. 포트를 잡고 있는 예전 개발 서버는 먼저 끝낸다.

확인은 `npm run lint`, `npx antd lint ./src`, `npm run tsc`, `npm run test`, `npm run build`입니다.

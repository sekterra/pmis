# PMIS 프론트엔드

테넌트 사용자 앱과 플랫폼 운영 콘솔을 하나의 Ant Design Pro 앱으로 둡니다. 기본 언어는 `ko-KR`입니다.

## 실행

```bash
npm install
npm run dev
```

환경변수는 `.env.example`의 이름만 참고합니다. 값은 저장소에 넣지 않습니다.

- `DEV_BACKEND_URL`: 개발 서버가 `/api/mdm`, `/api/platform`을 넘길 주소. 없으면 프록시를 만들지 않습니다.
- `DEV_TENANT_CODE`: 있을 때만 개발 프록시가 `X-Tenant-Code` 헤더를 붙입니다. 운영 빌드에는 없습니다.
- `APP_MODE`: `tenant`(기본) 또는 `platform`. 라우트 분기는 이후 단계입니다.

확인은 `npm run lint`, `npx antd lint ./src`, `npm run tsc`, `npm run test`, `npm run build`입니다.

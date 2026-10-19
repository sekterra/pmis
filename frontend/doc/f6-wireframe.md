# F6 동작 정리

한 앱, 로그인 화면 `/user/login` 하나다. 접속 주소의 `GET /api/mdm/v1/context` 결과로 플랫폼 콘솔과 테넌트를 가른다.

- `kind=TENANT`: 제목은 `tenantName`. 없으면 로고만. 로그인은 테넌트 인증 API. 메뉴는 `GET /api/mdm/v1/menus/my`. 홈은 `/welcome`.
- `kind=PLATFORM`: 제목은 "플랫폼 콘솔". 로그인은 플랫폼 인증 API. 메뉴는 "테넌트 관리"(`/tenants`) 하나만. 홈은 `/tenants`. 서버 메뉴와 서버 번들은 부르지 않는다.
- 입력은 아이디, 비밀번호, 언어뿐이다. 테넌트 코드 입력란은 없다.
- context가 네트워크 오류나 5xx이면 "서버에 연결할 수 없습니다"와 다시 시도. `TENANT.NOT_FOUND` 계열이면 "알 수 없는 사이트입니다". 둘 다 로그인 폼을 보이지 않는다.
- 플랫폼 로그인으로 `/mdm/**`에 가면 403. 테넌트 로그인으로 `/tenants*`에 가면 403. 로그인 전 직접 접근은 `/user/login?redirect=`다.
- 비밀번호 변경은 `/user/password` 하나다.
- 로컬에서 `DEV_TENANT_CODE`가 있으면 테넌트, 비우면 플랫폼이다. 개발 서버 포트는 8000 하나다.

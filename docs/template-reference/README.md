# 템플릿 참고 원본 (읽기 전용)

이 폴더는 **참고 전용**이며 앱(`frontend/`)에 연결되지 않는다. Umi 라우트·빌드·테스트에 포함하지 않는다.

| 항목 | 값 |
| --- | --- |
| 출처 | https://github.com/ant-design/ant-design-pro |
| 태그 | `v6.0.3` (우리 `frontend/package.json` version `6.0.3`과 동일) |
| 커밋 | `adfd44085738ca953573a13322c1ba84aca8b9e3` |
| 복원 날짜 | 2026-10-29 |
| 방법 | 저장소 밖 임시 폴더에 `git clone --depth 1 --branch v6.0.3` 후 복사, 임시 복제 삭제 |

## 포함한 경로

- `src/pages/form`, `list`, `profile`, `account`, `result`, `dashboard`, `user`, `table-list`
- `src/services/ant-design-pro` (table-list·login이 import)
- `config/routes.ts`, `config/defaultSettings.ts`, `config/config.ts` (antd `configProvider` 포함)

공유 `src/components`(ArticleListContent 등)는 위 페이지가 `@/components/...`로 import하지 않아 복사하지 않았다. 페이지별 `components/`는 각 화면 폴더 안에 있다.

## 도구 제외

`docs/template-reference`는 `frontend/` 밖이다. `frontend`의 biome·tsc·vitest·`antd lint ./src` 대상에 들어가지 않는다. 별도 exclude 변경은 없다.

파일을 수정하지 말고 읽기만 한다.

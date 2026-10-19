# 백엔드 8.7단계 작업 보고서

## a. 기본 정보

- 브랜치: `step/be-8.7`
- PR: [#5](https://github.com/sekterra/pmis/pull/5)
- 보고서 작성 시점의 구현 커밋 (`git log --oneline origin/main..HEAD`, 이 보고서 커밋은 제외):
  - `611d57d` feat(mdm): 메시지 목록 응답에 번역 요약 필드 추가
  - `a3d2113` docs(prompts): 백엔드 8.7단계 프롬프트 추가
- 변경 파일 (`git diff --stat origin/main...HEAD`, 이 보고서 추가 전):

```
 backend/common/doc/api/mdm-api.md                  |  24 ++++
 backend/common/doc/cursor-progress.md              |  20 +++
 .../com/tommy/common/i18n/dto/MessageResponse.java |   7 +-
 .../i18n/repository/I18nMessageTextRepository.java |  10 ++
 .../common/i18n/repository/MessageTextSlice.java   |  13 ++
 .../common/i18n/service/I18nMessageService.java    |  49 ++++++-
 .../com/tommy/common/I18nApiIntegrationTest.java   | 113 +++++++++++++++
 .../i18n/service/I18nMessageListSummaryTest.java   | 155 +++++++++++++++++++++
 docs/prompts/be-8.7.md                             |  26 ++++
 9 files changed, 414 insertions(+), 3 deletions(-)
```

## b. 요구사항 체크리스트

기준은 `docs/prompts/be-8.7.md`다.

| 항목 | 판정 | 근거 |
| --- | --- | --- |
| 기존 응답 필드는 삭제·이름 변경하지 않고 필드만 추가 | 충족 | `MessageResponse` 레코드 끝에 필드 3개만 추가. 기존 8개 컴포넌트 이름과 순서는 그대로 |
| `translatedLanguageCodes`: 문구가 비어 있지 않은 활성 언어, 언어 정렬 순서, 없으면 빈 배열 | 충족 | `I18nMessageService.list`가 `SupportLanguageRepository.findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc("Y")` 순서를 쓰고, `listItem`이 공백이 아닌 문구만 `translated`에 넣음 |
| `missingLanguageCodes`: 활성 지원 언어 중 번역이 없는 언어 | 충족 | `I18nMessageService.listItem`이 행이 없거나 문구가 비어 있으면 `missing`에 넣음 |
| `translationStatusCodes`: 번역 상태 코드 중복 제거 | 충족 | `I18nMessageService.listItem`이 활성 언어 순으로 처음 나온 상태만 `statuses`에 넣음 |
| 목록의 `translations`는 비우고 본문은 단건 조회에만 | 충족 | `listItem`은 `List.of()`를 넣고, `response`는 `withTexts=true`일 때만 `I18nMessageTextRepository.findByMessageMessagePk` 결과를 넣음 |
| 페이지 PK로 번역을 한 번의 IN 조회. 행마다 쿼리 금지 | 충족 | `I18nMessageTextRepository.findSlicesByMessagePkIn` 한 번. `textsFor`가 메시지마다 조회하지 않음 |
| 비활성(`use_yn='N'`) 언어는 집계에서 제외 | 충족 | 활성 언어 목록에 없는 코드는 `listItem`의 세 배열에 들어가지 않음 |
| 기존 번역 상태 필터 유지. 누락 언어 필터는 추가하지 않음 | 충족 | `I18nMessageService.filter`의 `translationStatusCode` EXISTS 조건은 변경하지 않음. 컨트롤러 파라미터도 추가하지 않음 |
| 번들 조회와 단건 조회 응답, ETag는 변경하지 않음 | 충족 | `BundleService`와 `I18nQueryController`는 수정하지 않음. 단건은 `I18nMessageService.get` → `response`이고 요약 3필드는 null이라 JSON에서 빠짐 |
| 일부·전체·없음·비활성 언어 통합 테스트 | 충족 | `I18nApiIntegrationTest.messageListReportsTranslationCoverageAndSkipsInactiveLanguages` |
| 목록 쿼리 수가 페이지 크기와 무관한 테스트 | 부분 | `I18nMessageListSummaryTest.summarizesThePageInOneQueryAndIgnoresInactiveOrBlankLanguages`가 목 호출 횟수로 확인. 실제 SQL 로그나 페이지 크기를 바꾼 통합 테스트는 없음 |
| `doc/api/mdm-api.md` 목록 응답 예시와 필드 설명 | 충족 | `backend/common/doc/api/mdm-api.md` 다국어 관리 절 |
| `gradlew.bat test`, `integrationTest`, DB 이름 `pmis`, `it_` 잔존 확인, 실제 테넌트 행 불변 | 충족 | 아래 e. 종료 후 `it_`/`t_it_` 스키마 0, `pmis.tenant` 0행 |
| 새 라이브러리 없음 | 충족 | `build.gradle` 변경 없음 |
| `[8.7단계]` 진행 기록 | 충족 | `backend/common/doc/cursor-progress.md` |

## c. 핵심 코드

응답 DTO (`backend/common/src/main/java/com/tommy/common/i18n/dto/MessageResponse.java`):

```java
public record MessageResponse(Long messagePk, String namespaceCode, String messageKey, String messageTypeCode,
		String defaultMessage, String paramDescription, String useYn, List<MessageTextResponse> translations,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> translatedLanguageCodes,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> missingLanguageCodes,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> translationStatusCodes) {
}
```

번역 조회 (`I18nMessageTextRepository.findSlicesByMessagePkIn`):

```java
@Query("""
		select text.message.messagePk as messagePk, text.supportLanguageCode as supportLanguageCode,
				text.messageText as messageText, text.translationStatusCode as translationStatusCode
		from I18nMessageText text
		where text.message.messagePk in :messagePks
		""")
List<MessageTextSlice> findSlicesByMessagePkIn(@Param("messagePks") Collection<Long> messagePks);
```

조립 (`I18nMessageService.list`, `textsFor`, `listItem`):

```java
List<String> activeLanguages = languageRepository.findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc("Y").stream()
		.map(SupportLanguage::getSupportLanguageCode)
		.toList();
Map<Long, List<MessageTextSlice>> texts = textsFor(messages);
List<MessageResponse> items = messages.stream()
		.map(message -> listItem(message, activeLanguages, texts.getOrDefault(message.getMessagePk(), List.of())))
		.toList();

private Map<Long, List<MessageTextSlice>> textsFor(List<I18nMessage> messages) {
	if (messages.isEmpty()) {
		return Map.of();
	}
	List<Long> messagePks = messages.stream().map(I18nMessage::getMessagePk).toList();
	return textRepository.findSlicesByMessagePkIn(messagePks).stream()
			.collect(Collectors.groupingBy(MessageTextSlice::getMessagePk, LinkedHashMap::new, Collectors.toList()));
}
```

`listItem`은 활성 언어 순서로 비어 있지 않은 문구만 번역된 언어와 상태 코드에 넣고, 나머지는 누락 언어에 넣는다. 목록의 `translations`는 빈 배열이다.

## d. N+1 확인

`I18nMessageListSummaryTest.summarizesThePageInOneQueryAndIgnoresInactiveOrBlankLanguages`가 메시지 5건을 한 페이지로 넘겼다.

- `I18nMessageTextRepository.findSlicesByMessagePkIn(List.of(1L, 2L, 3L, 4L, 5L))`는 1회
- `findByMessageMessagePk`는 0회
- 활성 언어 조회 `findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc("Y")`는 1회

번역 조회 횟수가 행 수(5)가 아니라 1이다. 페이지가 비어 있으면 `textsFor`가 IN 조회를 하지 않는다. 이 확인은 저장소 목이며, 실제 SQL 개수를 센 것은 아니다. 목록 요청마다 메시지 페이지 조회 1번, 활성 언어 조회 1번, 번역 IN 조회 1번이 나가고, 그 번역 조회는 페이지 크기에 따라 늘지 않는다.

## e. 테스트

명령은 `backend/common`에서 실행했다.

- `gradlew.bat test --offline --no-daemon`: 55 통과, 실패 0, skip 0
- `gradlew.bat integrationTest --offline --no-daemon`: 31 통과, 실패 0, skip 0
- 실행 전 대상 DB 이름은 `pmis`였다. 종료 후 `it_`/`t_it_` 스키마 0개, `pmis.tenant` 0행
- 실행하지 못한 테스트는 없다

## f. 가정, 질문, 한계, 벗어난 점

가정:

- 문구가 공백만 있으면 번역이 없다. 그 상태 코드는 요약에 넣지 않는다. 이 경우는 단위 테스트에만 있다.
- `translationStatusCodes` 순서는 활성 언어의 `sort_order`, `support_language_code` 순으로 처음 나온 상태다. 명세의 예시는 값만 보여 주고 정렬 규칙은 없었다.
- 단건·등록·수정·복구는 요약 필드를 null로 두어 JSON에서 빠지게 했다. 목록만 세 배열을 채운다.

질문: 없음.

알려진 한계:

- 쿼리 수가 페이지 크기와 무관하다는 근거는 목 호출 횟수다. Hibernate 통계나 SQL 로그로 페이지 크기 20과 1을 비교하지는 않았다.
- 활성 언어 조회는 목록 요청마다 1번 더 있다. 행마다 늘어나지는 않는다.

지시에서 벗어난 점:

- PR 생성 당시 이 환경에 `gh`가 없어 Cursor가 PR을 열지 못했다. 사용자는 PR #5가 열려 있다고 알려 주었다.
- 공백 문구는 통합 테스트 4가지 사례에 포함하지 않았다. 명세의 통합 테스트 사례는 일부, 전체, 없음, 비활성 언어이고, 그 4가지는 `I18nApiIntegrationTest`에 있다.

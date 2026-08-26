# Galaxy Watch Voice Companion

Galaxy Watch 9(Wear OS 7)에서 음성으로 **폰의 앱을 실행**하고 **캘린더 일정을 만들고 수정**하기 위한
워치 앱 + 폰 컴패니언 앱입니다. 이 저장소는 Phase 0(스캐폴딩 + Watch↔Phone 통신 골격) 상태입니다.

## 모듈 구조

```
shared/protocol   순수 JVM. 워치와 폰이 공유하는 명령/결과 직렬화 프로토콜
watch/domain      순수 JVM. 워치 유스케이스와 포트(CommandTransport, IntentParser)
watch/data        Android 라이브러리. Wearable MessageClient 기반 transport 구현
watch/app         Wear OS 앱. Compose for Wear OS UI
phone/domain      순수 JVM. 폰 포트(AppLauncher, CalendarRepository)
phone/data        Android 라이브러리. WearableListenerService, 향후 PackageManager/CalendarContract
phone/app         폰 컴패니언 앱. Compose Material 3 UI
```

Android 프레임워크 의존성은 `*/data`, `*/app`에만 두고, `domain`과 `shared/protocol`은 순수 JVM으로
유지해 단위 테스트가 에뮬레이터 없이 돌아가도록 했습니다.

## 통신 프로토콜

명령은 `DataClient`가 아니라 `MessageClient`로 보냅니다. DataItem은 중복 제거되고 연결이 끊긴 동안
저장되었다가 나중에 재생될 수 있어, "앱 실행" 같은 일회성 명령에 부적합합니다.

| 경로 | 방향 | 용도 |
| --- | --- | --- |
| `/watchvoice/command` | 워치 → 폰 | `CommandEnvelope`(JSON) |
| `/watchvoice/result` | 폰 → 워치 | `CommandResult`(JSON) |
| capability `watchvoice_companion` | 폰이 광고 | 컴패니언 설치/도달 가능 여부 판별 |

봉투에는 `commandId`, `protocolVersion`, `sentAtEpochMs`가 들어가며, 폰은 프로토콜 버전이 다르거나
`CommandEnvelope.MAX_AGE_MS`(60초)보다 오래된 명령을 거부합니다.

`WatchCommand.LaunchApp`은 Android 패키지명이나 컴포넌트가 아니라 논리적인 `appKey`만 담습니다.
임의의 component/action/extras를 전송하지 않아 인텐트 스푸핑 여지를 없애기 위함입니다.

## 빌드

Android SDK(API 37, build-tools 37)가 필요하며 `local.properties`의 `sdk.dir`로 지정합니다.

```bash
./gradlew assembleDebug   # 워치/폰 디버그 APK
./gradlew test            # JVM 단위 테스트
./gradlew lint            # Android Lint
```

산출물:

- `watch/app/build/outputs/apk/debug/app-debug.apk`
- `phone/app/build/outputs/apk/debug/app-debug.apk`

워치 앱과 폰 앱은 `applicationId`를 공유합니다(Wear OS 페어링 규약).

## 현재 동작

폰 컴패니언을 설치한 뒤 워치 앱에서 "폰 연결 확인"을 누르면 `Ping`이 전송되고, 폰의
`CommandListenerService`가 `pong`으로 응답하여 연결 상태가 표시됩니다. 앱 실행과 캘린더 명령은
프로토콜에는 정의되어 있으나 아직 `UNSUPPORTED_COMMAND`를 반환합니다.

## 다음 단계

1. Phase 1 — 실기기 Watch↔Phone 통신 검증, 연결 끊김/컴패니언 미설치 처리
2. Phase 2 — 워치 음성 입력(STT), 한/영 명령 파싱, `PackageManager` 기반 앱 실행 + `SYSTEM_ALERT_WINDOW` 온보딩
3. Phase 3 — `CalendarContract` 기반 일정 생성/수정, 날짜 명시 이동, 다중 후보 되묻기
4. Phase 4 — 오류 처리, 로깅, 테스트 보강

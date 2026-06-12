# Multichat Java

Java Socket API와 멀티스레딩을 사용해 구현한 실시간 채팅 시스템입니다. 서버 하나에 여러 클라이언트가 동시에 접속해 메시지를 주고받을 수 있고, TCP 스트림에서 메시지 경계가 깨지는 문제를 피하기 위해 4-byte length-prefix 프로토콜을 사용합니다.

## 프로젝트 목적

여러 클라이언트가 동시에 접속하는 환경에서 메시지 송수신, 연결 종료, 로그 추적을 안정적으로 처리하는 흐름을 학습하기 위해 진행했습니다.

## 빠른 실행

필요한 것은 JDK 17 이상입니다.

```bash
./scripts/test.sh
```

서버 실행:

```bash
./scripts/run-server.sh 5000
```

다른 터미널에서 클라이언트 실행:

```bash
./scripts/run-client.sh 127.0.0.1 5000 gyumin
./scripts/run-client.sh 127.0.0.1 5000 guest
```

클라이언트에서 `/quit`을 입력하면 연결을 종료합니다.
`/users`를 입력하면 현재 접속 중인 사용자 목록을 확인할 수 있습니다.

## 핵심 주제

- Java Socket 기반 클라이언트/서버 통신
- Thread Pool 기반 클라이언트 처리
- JOIN, CHAT, LEAVE, SYSTEM 메시지 유형 구분
- 4-byte length-prefix 기반 수신 버퍼 처리
- `/users` 명령어를 통한 접속자 목록 조회
- 로그 기준 통일을 통한 디버깅

## 구조

```text
src/main/java/dev/lukemin/multichat/
  client/ChatClient.java
  server/ChatServer.java
  server/ClientSession.java
  protocol/Message.java
  protocol/MessageCodec.java
  protocol/MessageType.java
  util/Log.java
src/test/java/dev/lukemin/multichat/
  protocol/MessageCodecTest.java
scripts/
  compile.sh
  test.sh
  run-server.sh
  run-client.sh
```

## 문제 해결 포인트

- 긴 메시지가 여러 번에 나뉘어 수신될 때 데이터가 깨지는 문제를 length-prefix 프레이밍으로 해결했습니다.
- 스레드 ID, 타임스탬프, 이벤트 유형을 로그에 남겨 어느 연결에서 문제가 발생했는지 추적했습니다.
- 메시지 단위를 명확히 처리하기 위해 헤더와 페이로드 기준의 프로토콜을 구현했습니다.

## 프로토콜

각 메시지는 다음 순서로 전송됩니다.

```text
4-byte payload length
UTF-8 payload
```

payload에는 메시지 타입, sender, body가 들어갑니다. sender와 body는 줄바꿈이나 한글이 포함되어도 안전하게 처리되도록 Base64로 인코딩합니다.

이 방식은 TCP가 스트림 기반이라서 `send` 한 번과 `receive` 한 번이 1:1로 대응되지 않는다는 점을 전제로 합니다. `MessageCodec.read`는 지정된 길이만큼 바이트를 누적해서 완전한 메시지만 반환합니다.

## 면접 대비 문서

프로젝트 설명 흐름과 면접 답변 포인트는 [`docs/interview-notes.md`](./docs/interview-notes.md)에 정리했습니다.
폴더별 역할과 코드 흐름은 [`docs/code-walkthrough.md`](./docs/code-walkthrough.md)에 정리했습니다.

## 다시 한다면

- Java NIO 기반 비동기 소켓 처리 검토
- 클라이언트 재접속과 예외 복구 흐름 보강
- 귓속말, 채팅방 분리, 중복 닉네임 방지 기능 추가

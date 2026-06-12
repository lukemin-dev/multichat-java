# Multichat Java Code Walkthrough

이 문서는 `multichat-java` 코드를 처음 읽는 사람을 위한 공부용 설명입니다. 목표는 코드를 외우는 것이 아니라, "채팅 메시지가 어떤 순서로 이동하는지"를 따라갈 수 있게 되는 것입니다.

## 1. 폴더별 역할

```text
multichat-java/
  README.md
  docs/
  scripts/
  src/
```

### 루트 폴더

프로젝트의 가장 바깥 폴더입니다.

- `README.md`: 프로젝트 소개, 실행 방법, 핵심 개념
- `.gitignore`: `build/` 같은 임시 파일을 GitHub에 올리지 않도록 제외
- `build/`: 컴파일 후 생기는 결과물. 직접 수정하지 않아도 됩니다.

### docs

공부와 면접 설명을 위한 문서 폴더입니다.

- `interview-notes.md`: 면접에서 프로젝트를 설명하기 위한 답변 흐름
- `code-walkthrough.md`: 폴더와 코드 흐름을 이해하기 위한 설명

### scripts

긴 명령어를 짧게 실행하기 위한 스크립트 폴더입니다.

```text
compile.sh      Java 파일을 컴파일합니다.
test.sh         테스트를 실행합니다.
run-server.sh   채팅 서버를 실행합니다.
run-client.sh   채팅 클라이언트를 실행합니다.
```

예를 들어 서버를 실행할 때 매번 긴 `javac`, `java` 명령어를 치지 않고 아래처럼 실행할 수 있습니다.

```bash
./scripts/run-server.sh 5000
```

### src

실제 Java 코드가 들어있는 폴더입니다.

```text
src/main/java/   프로그램 본 코드
src/test/java/   테스트 코드
```

`main`은 실제 실행되는 코드, `test`는 코드가 의도대로 동작하는지 확인하는 코드입니다.

## 2. src/main/java 구조

```text
dev/lukemin/multichat/
  client/
  server/
  protocol/
  util/
```

### client

사용자가 실행하는 채팅 클라이언트 코드입니다.

- `ChatClient.java`: 터미널 입력을 읽고 서버로 메시지를 보냅니다. 서버에서 받은 메시지도 화면에 출력합니다.

쉽게 말하면 `client`는 채팅방에 들어온 사용자 프로그램입니다.

### server

채팅 서버 코드입니다.

- `ChatServer.java`: 서버 포트를 열고, 접속자를 받고, 전체 메시지를 브로드캐스트합니다.
- `ClientSession.java`: 접속자 한 명을 담당합니다. 한 사람이 보낸 메시지를 읽고 처리합니다.

쉽게 말하면 `ChatServer`는 채팅방 관리자이고, `ClientSession`은 접속자 한 명을 맡는 담당자입니다.

### protocol

메시지 규칙을 정의하는 코드입니다.

- `Message.java`: 메시지 한 개의 데이터 구조입니다.
- `MessageType.java`: `JOIN`, `CHAT`, `LEAVE`, `SYSTEM`, `ERROR` 같은 메시지 종류입니다.
- `MessageCodec.java`: 메시지를 네트워크로 보내기 위해 바이트로 바꾸고, 다시 메시지로 복원합니다.

여기가 이 프로젝트의 핵심입니다. TCP는 스트림 기반이라 메시지 경계를 자동으로 구분해주지 않습니다. 그래서 이 프로젝트는 메시지 앞에 "payload 길이 4바이트"를 먼저 붙여서 완전한 메시지만 읽도록 했습니다.

### util

여러 곳에서 같이 쓰는 작은 도구 코드입니다.

- `Log.java`: 시간, 스레드 이름, 이벤트 이름을 포함해서 로그를 출력합니다.

## 3. 전체 실행 흐름

채팅 서버와 클라이언트가 움직이는 순서는 아래와 같습니다.

```text
1. ChatServer가 포트를 엽니다.
2. ChatClient가 서버에 접속합니다.
3. ChatClient가 JOIN 메시지로 닉네임을 보냅니다.
4. ChatServer는 접속자마다 ClientSession을 만듭니다.
5. 사용자가 채팅을 입력합니다.
6. ChatClient가 CHAT 메시지로 서버에 보냅니다.
7. ClientSession이 메시지를 읽습니다.
8. ChatServer가 모든 ClientSession에게 메시지를 broadcast합니다.
9. 각 ChatClient가 메시지를 받아 화면에 출력합니다.
```

한 줄로 줄이면 이렇습니다.

```text
사용자 입력 -> ChatClient -> MessageCodec -> ClientSession -> ChatServer.broadcast -> 모든 ChatClient
```

## 4. 먼저 읽을 파일 순서

처음부터 모든 파일을 읽으려고 하면 헷갈립니다. 아래 순서대로 보는 것이 좋습니다.

### 1단계: ChatServer.java

먼저 서버가 어떻게 시작되는지 봅니다.

중요한 코드는 세 가지입니다.

```java
new ServerSocket(port)
```

서버 입구를 여는 코드입니다.

```java
serverSocket.accept()
```

클라이언트가 들어올 때까지 기다리는 코드입니다.

```java
clientPool.submit(new ClientSession(this, socket))
```

접속자 한 명을 `ClientSession`에게 맡기는 코드입니다.

### 2단계: ClientSession.java

접속자 한 명의 생명주기를 봅니다.

```text
JOIN 메시지 받기
-> 서버에 등록
-> 채팅 메시지 계속 읽기
-> /users 명령이면 접속자 목록 보내기
-> 일반 채팅이면 전체에게 broadcast
-> 나가면 서버에서 제거
```

`/users` 명령어도 여기서 처리합니다.

```java
if (message.body().trim().equalsIgnoreCase("/users")) {
    send(new Message(MessageType.SYSTEM, "server", "connected users: "
            + String.join(", ", server.connectedNicknames())));
    continue;
}
```

이 코드는 메시지 내용이 `/users`이면 전체 채팅으로 보내지 않고, 요청한 사람에게만 접속자 목록을 보내는 역할을 합니다.

### 3단계: ChatClient.java

클라이언트는 두 가지 일을 합니다.

```text
1. 사용자가 입력한 내용을 서버로 보냅니다.
2. 서버가 보내준 메시지를 화면에 출력합니다.
```

서버에서 메시지를 받는 작업은 별도 스레드에서 돌아갑니다. 그래야 사용자가 입력하는 동안에도 서버 메시지를 동시에 받을 수 있습니다.

### 4단계: Message.java / MessageType.java

메시지의 모양과 종류를 봅니다.

```text
type   메시지 종류
sender 보낸 사람
body   실제 내용
```

예를 들면 아래처럼 생각하면 됩니다.

```text
type: CHAT
sender: gyumin
body: hello
```

### 5단계: MessageCodec.java

가장 어렵지만 가장 중요한 파일입니다.

이 파일은 메시지를 네트워크로 보내기 위해 아래 순서로 처리합니다.

```text
Message 객체
-> 문자열 payload
-> UTF-8 bytes
-> 앞에 길이 4바이트 추가
-> OutputStream으로 전송
```

읽을 때는 반대로 처리합니다.

```text
InputStream에서 4바이트 읽기
-> payload 길이 계산
-> 그 길이만큼 정확히 읽기
-> payload를 Message 객체로 복원
```

이 방식이 필요한 이유는 TCP가 메시지 단위가 아니라 바이트 흐름 단위로 동작하기 때문입니다.

## 5. TCP 메시지 경계 문제

처음에는 이렇게 생각하기 쉽습니다.

```text
클라이언트가 "hello"를 한 번 보냄
서버도 "hello"를 한 번에 받음
```

하지만 TCP에서는 항상 그렇지 않습니다.

```text
보낸 쪽: hello world
받는 쪽: hel
받는 쪽: lo wo
받는 쪽: rld
```

또는 여러 메시지가 붙어서 올 수도 있습니다.

```text
보낸 쪽: hello
보낸 쪽: world
받는 쪽: helloworld
```

그래서 이 프로젝트는 각 메시지 앞에 길이를 붙입니다.

```text
[메시지 길이 4바이트][실제 메시지 내용]
```

그러면 서버는 먼저 4바이트를 읽고, "아, 이번 메시지는 35바이트구나"를 안 뒤에 정확히 35바이트를 읽습니다.

## 6. 공부하면서 직접 해볼 것

아래 순서로 직접 실행해보면 이해가 빨라집니다.

```bash
./scripts/test.sh
```

서버 실행:

```bash
./scripts/run-server.sh 5000
```

클라이언트 1:

```bash
./scripts/run-client.sh 127.0.0.1 5000 gyumin
```

클라이언트 2:

```bash
./scripts/run-client.sh 127.0.0.1 5000 guest
```

클라이언트에서 입력해볼 명령:

```text
hello
/users
/quit
```

## 7. 혼자 설명해보기

아래 질문에 답할 수 있으면 이 프로젝트의 큰 흐름은 잡은 것입니다.

1. `ChatServer`와 `ClientSession`은 역할이 어떻게 다른가?
2. 왜 클라이언트마다 별도 `ClientSession`이 필요한가?
3. `broadcast`는 어떤 일을 하는가?
4. `/users`는 왜 `ChatServer`가 아니라 `ClientSession`에서 먼저 감지하는가?
5. TCP에서 메시지 경계가 깨진다는 말은 무슨 뜻인가?
6. 왜 `MessageCodec`이 메시지 길이를 먼저 보내는가?
7. `MessageType`을 enum으로 만든 이유는 무엇인가?

## 8. 다음에 추가해볼 기능

코드를 더 이해하고 싶다면 작은 기능 하나씩 추가하는 것이 좋습니다.

- `/whisper 닉네임 메시지`: 특정 사용자에게만 메시지 보내기
- 중복 닉네임 거부
- 최대 접속자 수 제한
- 채팅 로그 파일 저장
- 채팅방 분리

처음에는 `/whisper`보다 "중복 닉네임 거부"가 더 쉽습니다. `ChatServer`가 이미 접속자 목록을 알고 있기 때문입니다.


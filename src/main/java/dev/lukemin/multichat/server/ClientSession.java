package dev.lukemin.multichat.server;

import dev.lukemin.multichat.protocol.Message;
import dev.lukemin.multichat.protocol.MessageCodec;
import dev.lukemin.multichat.protocol.MessageType;
import dev.lukemin.multichat.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

final class ClientSession implements Runnable {
    private final ChatServer server;
    private final Socket socket;
    private volatile String nickname = "anonymous";
    private volatile boolean registered;

    ClientSession(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    String nickname() {
        return nickname;
    }

    @Override
    public void run() {
        try (socket) {
            Message firstMessage = MessageCodec.read(socket.getInputStream());
            if (firstMessage == null || firstMessage.type() != MessageType.JOIN || firstMessage.sender().isBlank()) {
                send(new Message(MessageType.ERROR, "server", "JOIN message with a nickname is required"));
                return;
            }

            nickname = sanitizeNickname(firstMessage.sender());
            server.register(this);
            registered = true;
            Log.info("JOIN", nickname + " joined from " + socket.getRemoteSocketAddress());
            server.broadcastSystem(nickname + " joined the chat");

            Message message;
            while ((message = MessageCodec.read(socket.getInputStream())) != null) {
                if (message.type() == MessageType.LEAVE) {
                    break;
                }
                if (message.type() == MessageType.CHAT && !message.body().isBlank()) {
                    if (message.body().trim().equalsIgnoreCase("/users")) {
                        send(new Message(MessageType.SYSTEM, "server", "connected users: "
                                + String.join(", ", server.connectedNicknames())));
                        continue;
                    }
                    server.broadcast(new Message(MessageType.CHAT, nickname, message.body()));
                }
            }
        } catch (IOException exception) {
            Log.error("SESSION", "connection lost for " + nickname, exception);
        } finally {
            if (registered) {
                server.unregister(this);
                Log.info("LEAVE", nickname + " left");
                server.broadcastSystem(nickname + " left the chat");
            }
        }
    }

    synchronized void send(Message message) throws IOException {
        MessageCodec.write(socket.getOutputStream(), message);
    }

    private String sanitizeNickname(String rawNickname) {
        return Optional.ofNullable(rawNickname)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.replaceAll("\\s+", "_"))
                .map(value -> value.length() > 24 ? value.substring(0, 24) : value)
                .orElse("anonymous");
    }
}

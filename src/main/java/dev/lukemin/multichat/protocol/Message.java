package dev.lukemin.multichat.protocol;

import java.time.Instant;
import java.util.Objects;

public final class Message {
    private final MessageType type;
    private final String sender;
    private final String body;
    private final Instant createdAt;

    public Message(MessageType type, String sender, String body) {
        this(type, sender, body, Instant.now());
    }

    public Message(MessageType type, String sender, String body, Instant createdAt) {
        this.type = Objects.requireNonNull(type, "type");
        this.sender = sender == null ? "" : sender;
        this.body = body == null ? "" : body;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public MessageType type() {
        return type;
    }

    public String sender() {
        return sender;
    }

    public String body() {
        return body;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean isControlMessage() {
        return type == MessageType.JOIN || type == MessageType.LEAVE;
    }

    @Override
    public String toString() {
        if (sender.isBlank()) {
            return "[" + type + "] " + body;
        }
        return "[" + type + "] " + sender + ": " + body;
    }
}


package dev.lukemin.multichat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class MessageCodec {
    public static final int MAX_FRAME_SIZE = 64 * 1024;

    private MessageCodec() {
    }

    public static void write(OutputStream output, Message message) throws IOException {
        byte[] payload = encodePayload(message);
        if (payload.length > MAX_FRAME_SIZE) {
            throw new IOException("message is too large: " + payload.length + " bytes");
        }

        output.write(ByteBuffer.allocate(Integer.BYTES).putInt(payload.length).array());
        output.write(payload);
        output.flush();
    }

    public static Message read(InputStream input) throws IOException {
        byte[] header = readExactlyOrNull(input, Integer.BYTES);
        if (header == null) {
            return null;
        }

        int length = ByteBuffer.wrap(header).getInt();
        if (length < 0 || length > MAX_FRAME_SIZE) {
            throw new IOException("invalid frame length: " + length);
        }

        byte[] payload = readExactly(input, length);
        return decodePayload(payload);
    }

    private static byte[] encodePayload(Message message) {
        String payload = message.type().name()
                + "\n" + encodeField(message.sender())
                + "\n" + encodeField(message.body());
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    private static Message decodePayload(byte[] payload) throws IOException {
        String text = new String(payload, StandardCharsets.UTF_8);
        String[] fields = text.split("\n", 3);
        if (fields.length != 3) {
            throw new IOException("invalid message payload");
        }

        try {
            MessageType type = MessageType.valueOf(fields[0]);
            return new Message(type, decodeField(fields[1]), decodeField(fields[2]));
        } catch (IllegalArgumentException exception) {
            throw new IOException("unknown message type: " + fields[0], exception);
        }
    }

    private static String encodeField(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static byte[] readExactlyOrNull(InputStream input, int length) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(length);
        while (buffer.size() < length) {
            int next = input.read();
            if (next == -1) {
                if (buffer.size() == 0) {
                    return null;
                }
                throw new EOFException("stream ended in the middle of a frame header");
            }
            buffer.write(next);
        }
        return buffer.toByteArray();
    }

    private static byte[] readExactly(InputStream input, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(buffer, offset, length - offset);
            if (read == -1) {
                throw new EOFException("stream ended in the middle of a frame");
            }
            offset += read;
        }
        return buffer;
    }
}


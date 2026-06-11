package dev.lukemin.multichat.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class MessageCodecTest {
    private MessageCodecTest() {
    }

    public static void main(String[] args) throws IOException {
        roundTripKeepsFields();
        fragmentedFrameCanBeRead();
        oversizedFrameIsRejected();
        System.out.println("MessageCodecTest passed");
    }

    private static void roundTripKeepsFields() throws IOException {
        Message original = new Message(MessageType.CHAT, "gyumin", "hello\nsocket world");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MessageCodec.write(output, original);
        Message decoded = MessageCodec.read(new ByteArrayInputStream(output.toByteArray()));

        assertEquals(MessageType.CHAT, decoded.type(), "type");
        assertEquals("gyumin", decoded.sender(), "sender");
        assertEquals("hello\nsocket world", decoded.body(), "body");
    }

    private static void fragmentedFrameCanBeRead() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MessageCodec.write(output, new Message(MessageType.SYSTEM, "server", "fragmented payload"));

        ByteArrayInputStream slowInput = new ByteArrayInputStream(output.toByteArray()) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                return super.read(buffer, offset, Math.min(length, 2));
            }
        };

        Message decoded = MessageCodec.read(slowInput);
        assertEquals(MessageType.SYSTEM, decoded.type(), "fragmented type");
        assertEquals("fragmented payload", decoded.body(), "fragmented body");
    }

    private static void oversizedFrameIsRejected() {
        byte[] invalidHeader = new byte[] {0x00, 0x02, 0x00, 0x01};
        try {
            MessageCodec.read(new ByteArrayInputStream(invalidHeader));
            throw new AssertionError("expected oversized frame to fail");
        } catch (IOException expected) {
            if (!expected.getMessage().contains("invalid frame length")) {
                throw new AssertionError("unexpected exception: " + expected.getMessage());
            }
        }
    }

    private static void assertEquals(Object expected, Object actual, String name) {
        if (!expected.equals(actual)) {
            throw new AssertionError(name + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}


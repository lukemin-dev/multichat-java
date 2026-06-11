package dev.lukemin.multichat.client;

import dev.lukemin.multichat.protocol.Message;
import dev.lukemin.multichat.protocol.MessageCodec;
import dev.lukemin.multichat.protocol.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ChatClient {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 5000;

    private ChatClient() {
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        String nickname = args.length > 2 ? args[2] : System.getProperty("user.name", "guest");

        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            socket.setTcpNoDelay(true);
            MessageCodec.write(socket.getOutputStream(), new Message(MessageType.JOIN, nickname, ""));

            Thread receiver = new Thread(() -> receiveLoop(socket), "chat-client-receiver");
            receiver.setDaemon(true);
            receiver.start();

            System.out.println("Connected as " + nickname + ". Type /quit to leave.");
            String line;
            while ((line = console.readLine()) != null) {
                if (line.equalsIgnoreCase("/quit")) {
                    MessageCodec.write(socket.getOutputStream(), new Message(MessageType.LEAVE, nickname, "bye"));
                    break;
                }
                if (!line.isBlank()) {
                    MessageCodec.write(socket.getOutputStream(), new Message(MessageType.CHAT, nickname, line));
                }
            }
        }
    }

    private static void receiveLoop(Socket socket) {
        try {
            Message message;
            while ((message = MessageCodec.read(socket.getInputStream())) != null) {
                if (message.type() == MessageType.SYSTEM) {
                    System.out.println("* " + message.body());
                } else if (message.type() == MessageType.ERROR) {
                    System.out.println("! " + message.body());
                } else if (message.type() == MessageType.CHAT) {
                    System.out.println(message.sender() + ": " + message.body());
                }
            }
        } catch (IOException exception) {
            System.out.println("Disconnected from server.");
        }
    }
}


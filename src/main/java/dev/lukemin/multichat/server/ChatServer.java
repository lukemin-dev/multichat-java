package dev.lukemin.multichat.server;

import dev.lukemin.multichat.protocol.Message;
import dev.lukemin.multichat.protocol.MessageType;
import dev.lukemin.multichat.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChatServer {
    private static final int DEFAULT_PORT = 5000;

    private final int port;
    private final Set<ClientSession> sessions = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    public ChatServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new ChatServer(port).start();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Log.info("START", "chat server listening on port " + port);
            while (!clientPool.isShutdown()) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                clientPool.submit(new ClientSession(this, socket));
            }
        } finally {
            clientPool.shutdownNow();
        }
    }

    void register(ClientSession session) {
        sessions.add(session);
    }

    void unregister(ClientSession session) {
        sessions.remove(session);
    }

    void broadcastSystem(String body) {
        broadcast(new Message(MessageType.SYSTEM, "server", body));
    }

    List<String> connectedNicknames() {
        return sessions.stream()
                .map(ClientSession::nickname)
                .sorted()
                .toList();
    }

    void broadcast(Message message) {
        for (ClientSession session : sessions) {
            try {
                session.send(message);
            } catch (IOException exception) {
                Log.error("BROADCAST", "failed to send message to " + session.nickname(), exception);
                unregister(session);
            }
        }
    }
}

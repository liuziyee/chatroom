package com.dorohedoro.bio.server;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ChatServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String QUIT = "bye";

    private ExecutorService pool = Executors.newFixedThreadPool(5);
    private ServerSocket serverSocket;
    private Map<Integer, Writer> connectedClients = new HashMap<>();

    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port, writer);
            log.info("客户端[" + port + "]已连接");
        }
    }

    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                connectedClients.get(port).close();
                connectedClients.remove(port);
                log.info("客户端[" + port + "]已断开连接");
            }
        }
    }

    public void broadcast(Socket socket, String msg) throws IOException {
        for (Integer port : connectedClients.keySet()) {
            if (!port.equals(socket.getPort())) {
                Writer writer = connectedClients.get(port);
                writer.write(msg + "\n");
                writer.flush();
            }
        }
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public synchronized void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            log.info("服务器[" + DEFAULT_PORT + "]已启动");

            while (true) {
                Socket socket = serverSocket.accept();
                pool.execute(new ChatHandler(this, socket));
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            close(serverSocket);
        }
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}

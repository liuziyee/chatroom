package com.dorohedoro.bio.client;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;

@Slf4j
public class ChatClient {

    private final String DEFAULT_SERVER_IP = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 8080;
    private final String QUIT = "bye";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()) {
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()) {
            msg = reader.readLine();
        }
        return msg;
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void close() {
        try {
            if (writer != null) {
                writer.close();
                log.info("关闭客户端套接字");
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    public void start() {
        try {
            socket = new Socket(DEFAULT_SERVER_IP, DEFAULT_SERVER_PORT);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            new Thread(new KeyboardHandler(this)).start();

            String msg;
            while ((msg = receive()) != null) {
                log.info(msg);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        new ChatClient().start();
    }
}

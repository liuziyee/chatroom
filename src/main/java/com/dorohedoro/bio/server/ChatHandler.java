package com.dorohedoro.bio.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

@Slf4j
@RequiredArgsConstructor
public class ChatHandler implements Runnable {

    private final ChatServer chatServer;
    private final Socket socket;

    @Override
    public void run() {
        try {
            chatServer.addClient(socket);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            while ((msg = reader.readLine()) != null) {
                String broadcastMsg = "客户端[" + socket.getPort() + "]: " + msg;
                log.info(broadcastMsg);
                chatServer.broadcast(socket, broadcastMsg);
                if (chatServer.readyToQuit(msg)) {
                    break;
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                chatServer.removeClient(socket);
            } catch (Throwable e) {
                log.info(e.getMessage(), e);
            }
        }
    }
}

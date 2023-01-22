package com.dorohedoro.aio.callback;

import com.dorohedoro.aio.server.ChatServer;
import com.dorohedoro.common.IChatServer;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;

public class AcceptCallback implements CompletionHandler<AsynchronousSocketChannel, IChatServer> {

    private static final int SIZE = 1024;

    @Override
    public void completed(AsynchronousSocketChannel client, IChatServer attachment) {
        ChatServer chatServer = (ChatServer) attachment;
        AsynchronousServerSocketChannel server = chatServer.getServer();
        if (server.isOpen()) {
            server.accept(chatServer, this);
        }
        if (client != null && client.isOpen()) {
            ByteBuffer buffer = ByteBuffer.allocate(SIZE);
            ClientCallback callback = new ClientCallback(client);
            Map<String, Object> map = Map.of("buffer", buffer, "server", chatServer);
            client.read(buffer, map, callback);
            chatServer.addClient(callback);
        }
    }

    @Override
    public void failed(Throwable exc, IChatServer attachment) {}
}

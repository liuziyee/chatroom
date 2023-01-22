package com.dorohedoro.aio.callback;

import com.dorohedoro.aio.server.ChatServer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;

@Slf4j
@Data
@RequiredArgsConstructor
public class ClientCallback implements CompletionHandler<Integer, Map> {
    
    private final AsynchronousSocketChannel client;

    @Override
    public void completed(Integer res, Map attachment) {
        ByteBuffer buffer = (ByteBuffer) attachment.get("buffer");
        ChatServer chatServer = (ChatServer) attachment.get("server");
        if (buffer != null) {
            // 读操作完成
            if (res <= 0) {
                log.info(chatServer.getClientId(client) + "异常");
                chatServer.removeClient(this);
                return;
            }
            String msg = chatServer.bufferToStr(buffer);
            chatServer.broadcast(client, msg);
            if (chatServer.readyToQuit(msg)) {
                chatServer.removeClient(this);
            } else {
                client.read(buffer, attachment, this);
            }
        }
    }

    @Override
    public void failed(Throwable exc, Map attachment) {}
}

package com.dorohedoro.aio.client;

import com.dorohedoro.common.IChatClient;
import com.dorohedoro.common.KeyboardHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

@Slf4j
public class ChatClient implements IChatClient {

    private static final String DEFAULT_SERVER_IP = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final int SIZE = 1024;

    private AsynchronousSocketChannel client;
    private ByteBuffer readBuffer = ByteBuffer.allocate(SIZE);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(SIZE);

    @Override
    public void start() {
        try {
            client = AsynchronousSocketChannel.open();
            Future<Void> connectOps = client.connect(new InetSocketAddress(DEFAULT_SERVER_IP, DEFAULT_SERVER_PORT));
            connectOps.get();

            new Thread(new KeyboardHandler(this)).start();

            while (true) {
                Future<Integer> readOps = client.read(readBuffer);
                if (readOps.get() <= 0) {
                    log.info("服务端异常");
                    break;
                }
                String msg = bufferToStr(readBuffer);
                log.info(msg);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            close(client);
        }
    }

    public String bufferToStr(ByteBuffer buffer) {
        buffer.flip();
        String msg = new String(buffer.array(), buffer.position(), buffer.limit(), StandardCharsets.UTF_8);
        buffer.clear();
        return msg;
    }
    
    @SneakyThrows
    @Override
    public void send(String msg) {
        writeBuffer.clear();
        writeBuffer.put(Charset.forName("utf-8").encode(msg));
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            Future<Integer> writeOps = client.write(writeBuffer);
            writeOps.get();
        }

        if (readyToQuit(msg)) {
            close(client);
        }
    }

    public static void main(String[] args) {
        new ChatClient().start();
    }
}

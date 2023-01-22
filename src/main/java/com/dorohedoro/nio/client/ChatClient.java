package com.dorohedoro.nio.client;

import com.dorohedoro.common.IChatClient;
import com.dorohedoro.common.KeyboardHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
public class ChatClient implements IChatClient {

    private static final String DEFAULT_SERVER_IP = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final int SIZE = 1024;

    private SocketChannel client;
    private ByteBuffer readBuffer = ByteBuffer.allocate(SIZE);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(SIZE);
    private Selector selector;

    public void start() {
        try {
            client = SocketChannel.open();
            selector = Selector.open();
            log.debug("客户端通道置为非阻塞模式,向多路复用器注册一个CONNECT事件");
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_CONNECT);
            client.connect(new InetSocketAddress(DEFAULT_SERVER_IP, DEFAULT_SERVER_PORT));

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    handleEvent(key);
                }
                keys.clear();
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            close(selector);
        }
    }

    private void handleEvent(SelectionKey key) throws IOException {
        if (key.isConnectable()) {
            if (client.isConnectionPending()) {
                client.finishConnect();
                new Thread(new KeyboardHandler(this)).start();
            }
            log.debug("客户端通道向多路复用器注册一个READ事件");
            client.register(selector, SelectionKey.OP_READ);
        }
        if (key.isReadable()) {
            String msg = receive();
            if (msg == null || msg.isEmpty()) {
                log.info("服务端异常");
                close(selector);
                return;
            }
            log.info(msg);
        }
    }

    public String receive() throws IOException {
        readBuffer.clear();
        while(client.read(readBuffer) > 0);
        readBuffer.flip();
        return new String(readBuffer.array(), readBuffer.position(), readBuffer.limit(), StandardCharsets.UTF_8);
    }

    @Override
    public void send(String msg) throws IOException {
        writeBuffer.clear();
        writeBuffer.put(Charset.forName("utf-8").encode(msg));
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            client.write(writeBuffer);
        }

        if (readyToQuit(msg)) {
            close(selector);
        }
    }

    public static void main(String[] args) {
        new ChatClient().start();
    }
}

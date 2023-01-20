package com.dorohedoro.nio.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

@Slf4j
public class ChatServer {

    private final int DEFAULT_PORT = 8080;
    private final String QUIT = "bye";
    private final int SIZE = 1024;

    private ServerSocketChannel server;
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(SIZE);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(SIZE);

    private void start() {
        try {
            server = ServerSocketChannel.open();
            server.socket().bind(new InetSocketAddress(DEFAULT_PORT));

            selector = Selector.open();
            log.info("channel置为非阻塞模式,向selector注册一个ACCEPT事件");
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);
            log.info("服务器[" + DEFAULT_PORT + "]已启动");

            while (true) {
                log.info("阻塞直到至少有一个channel在注册的事件上就绪");
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    handleEvent(key);
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleEvent(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        }
        if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            String msg = receive(client);
            if (msg == null || msg.isEmpty()) {
                key.cancel();
                selector.wakeup();
                log.info("客户端[" + client.socket().getPort() + "]异常");
                return;
            }
            broadcast(client, msg);
            if (readyToQuit(msg)) {
                key.cancel();
                selector.wakeup();
                log.info("客户端[" + client.socket().getPort() + "]已断开连接");
            }
        }
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private String receive(SocketChannel client) throws IOException {
        log.info("写模式复位");
        log.info("读取通道的数据,写到缓冲区");
        log.info("写模式 => 读模式");
        readBuffer.clear();
        while (client.read(readBuffer) > 0);
        readBuffer.flip();
        return new String(readBuffer.array(), readBuffer.position(), readBuffer.limit(), "utf-8");
    }

    private void broadcast(SocketChannel client, String msg) throws IOException {
        msg = "客户端[" + client.socket().getPort() + "]: " + msg;
        log.info(msg);

        for (SelectionKey key : selector.keys()) {
            Channel channel = key.channel();
            if (channel instanceof ServerSocketChannel) {
                continue;
            }
            if (key.isValid() && !channel.equals(client)) {
                log.info("写模式复位");
                log.info("写数据到缓冲区");
                log.info("写模式 => 读模式");
                log.info("读取缓冲区的数据,写到通道");
                writeBuffer.clear();
                writeBuffer.put(Charset.forName("utf-8").encode(msg));
                writeBuffer.flip();
                while (writeBuffer.hasRemaining()) {
                    ((SocketChannel) channel).write(writeBuffer);
                }
            }
        }
    }
}

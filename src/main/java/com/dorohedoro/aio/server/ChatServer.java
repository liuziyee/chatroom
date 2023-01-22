package com.dorohedoro.aio.server;

import com.dorohedoro.aio.callback.AcceptCallback;
import com.dorohedoro.aio.callback.ClientCallback;
import com.dorohedoro.common.IChatServer;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Data
public class ChatServer implements IChatServer {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8080;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel server;
    private Set<ClientCallback> connectedClients = new HashSet<>();

    @Override
    public void start() {
        try {
            ExecutorService pool = Executors.newFixedThreadPool(5);
            channelGroup = AsynchronousChannelGroup.withThreadPool(pool);
            server = AsynchronousServerSocketChannel.open(channelGroup);
            server.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            log.info("服务器[" + DEFAULT_PORT + "]已启动");
            
            server.accept(this, new AcceptCallback());
            System.in.read();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            close(server);
        }
    }

    public synchronized void addClient(ClientCallback callback) {
        connectedClients.add(callback);
        log.info(getClientId(callback.getClient()) + "已连接");
    }


    public synchronized void removeClient(ClientCallback callback) {
        connectedClients.remove(callback);
        close(callback.getClient());
        log.info(getClientId(callback.getClient()) + "已断开连接");
    }

    public String bufferToStr(ByteBuffer buffer) {
        buffer.flip();
        String msg = new String(buffer.array(), buffer.position(), buffer.limit(), StandardCharsets.UTF_8);
        buffer.clear();
        return msg;
    }

    public void broadcast(AsynchronousSocketChannel client, String msg) {
        msg = getClientId(client) + msg;
        log.info(msg);

        for (ClientCallback callback : connectedClients) {
            if (!client.equals(callback.getClient())) {
                try {
                    ByteBuffer buffer = Charset.forName("utf-8").encode(msg);
                    callback.getClient().write(buffer, Map.of(), callback);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
    
    @SneakyThrows
    public String getClientId(AsynchronousSocketChannel client) {
        InetSocketAddress address = (InetSocketAddress) client.getRemoteAddress();
        return "客户端[" + address.getPort() + "]";
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}

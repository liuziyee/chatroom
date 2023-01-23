package com.dorohedoro.common;

import lombok.SneakyThrows;

import java.io.Closeable;
import java.io.IOException;

public interface IChatClient {

    String QUIT = "bye";

    void start();

    void send(String msg) throws IOException;
    
    default boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }
    
    @SneakyThrows
    default void close(Closeable closeable) {
        if (closeable != null) {
            closeable.close();
        }
    }
}

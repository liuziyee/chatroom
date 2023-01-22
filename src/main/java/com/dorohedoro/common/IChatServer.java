package com.dorohedoro.common;

import lombok.SneakyThrows;

import java.io.Closeable;

public interface IChatServer {

    String QUIT = "bye";

    void start();
    
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

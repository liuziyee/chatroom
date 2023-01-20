package com.dorohedoro.common;

import java.io.Closeable;
import java.io.IOException;

public interface IChatClient {

    String QUIT = "bye";

    String receive() throws IOException;
    
    void send(String msg) throws IOException;
    
    default boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }
    
    default void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
        }
    }
}

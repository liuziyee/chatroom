package com.dorohedoro.bio.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@RequiredArgsConstructor
public class KeyboardHandler implements Runnable {

    private final ChatClient chatClient;

    @Override
    public void run() {
        BufferedReader keyboardReader = null;
        try {
            keyboardReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = keyboardReader.readLine();
                chatClient.send(msg);
                if (chatClient.readyToQuit(msg)) {
                    break;
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (keyboardReader != null) {
                    keyboardReader.close();
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}

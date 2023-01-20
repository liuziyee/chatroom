package com.dorohedoro.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@RequiredArgsConstructor
public class KeyboardHandler implements Runnable {

    private final IChatClient chatClient;

    @Override
    public void run() {
        BufferedReader keyboardReader;
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
        }
    }
}

package com.duynguyen.utils;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class Utils {
    private static final Random rand = new Random();
    
    public static boolean availablePort(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    public static int nextInt(int max) {
        return rand.nextInt(max);
    }

    public static void setTimeout(Runnable runnable, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }
    
}

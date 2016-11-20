package com.example.cli;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class ThreadProfiler {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("PID: " + ManagementFactory.getRuntimeMXBean().getName() + "\n\n");

        new ThreadProfiler().run();
    }

    AtomicLong lastUsedMemory = new AtomicLong();

    public void run() {
        Runtime runtime = Runtime.getRuntime();
        lastUsedMemory.set(runtime.totalMemory() - runtime.freeMemory());
        IntStream.rangeClosed(1, 10000)
                .forEach(i -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(60 * 60 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    if (i % 200 == 0) {
                        System.out.println(i);
                    }
                });
    }
}
package com.example.cxfdemo.service;

import org.junit.Test;

public class AsyncBankTransferServiceTest {

    @Test
    public void testStartAsyncTransferTask() throws InterruptedException {
        AsyncBankTransferService service = new AsyncBankTransferService();

        service.startAsyncTransferTask();

        System.out.println("Main thread is waiting for background task...");
        Thread.sleep(3000);
        System.out.println("Test finished (Check console output to see retry log).");
    }
}

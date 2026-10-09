package com.flashexchange.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FlashSaleServiceTest {
    
    @Autowired 
    private FlashSaleService flashSaleService;

    @Test
    @DisplayName("Stress test: 1000 người tranh mua 100 sản phẩm - không được bán âm")
    void testConcurrentFlashSale() throws InterruptedException {
        String itemKey = "flashsale:stock:TICKET_VIP";
        int initialStock = 100;
        int totalRequests = 1000;

        // Khởi tạo 100 vé vào Redis
        flashSaleService.initStock(itemKey, initialStock);

        ExecutorService executor = Executors.newFixedThreadPool(50);

        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0;i < totalRequests;i++) {
            executor.submit(() -> {
                try {
                    startGun.await();
                    boolean success = flashSaleService.tryDeductStock(itemKey, 1);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            });
        }


        long startTime = System.currentTimeMillis();
        startGun.countDown();

        allDone.await();
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();

        long remainingStock = flashSaleService.getStock(itemKey);


        System.out.println("==================================================");
        System.out.println("TEST:");
        System.out.println("Excecution time: " + duration + " ms");
        System.out.println("Completed requests: " + successCount.get());
        System.out.println("Failed requests: " + failCount.get());
        System.out.println("Remaining stock: " + remainingStock);
        System.out.println("==================================================");

        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failCount.get()).isEqualTo(totalRequests - initialStock);
        assertThat(remainingStock).isEqualTo(0);
    }


    @Test
    @DisplayName("Test timeout: Quá 10s không thanh toán -> Tự động hoàn lại sản phẩm")
    void testOrderTimeout_RollbackStock() throws InterruptedException {
        String itemKey = "flashsale:stock:IPHONE_TIMEOUT";

        flashSaleService.initStock(itemKey, 1);
        assertThat(flashSaleService.getStock(itemKey)).isEqualTo(1);

        boolean success = flashSaleService.tryDeductStock(itemKey, 1);
        assertThat(success).isTrue();

        assertThat(flashSaleService.getStock(itemKey)).isEqualTo(0);
        System.out.println("Khách đã mua thành công, kho hiện tại: " + flashSaleService.getStock(itemKey));

        System.out.println("Đợi 12s để kiểm tra timeout...");
        Thread.sleep(12000);

        long restoredStock = flashSaleService.getStock(itemKey);
        System.out.println("Sau 12s, kho hiện tại: " + restoredStock);

        assertThat(restoredStock).isEqualTo(1);
    }

}

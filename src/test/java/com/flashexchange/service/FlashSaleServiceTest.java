package com.flashexchange.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.flashexchange.dto.DeductResult;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FlashSaleServiceTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

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
                    DeductResult success = flashSaleService.tryDeductStock(itemKey, 1);
                    if (success.success()) {
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

        DeductResult success = flashSaleService.tryDeductStock(itemKey, 1);
        assertThat(success.success()).isTrue();

        assertThat(flashSaleService.getStock(itemKey)).isEqualTo(0);
        System.out.println("Khách đã mua thành công, kho hiện tại: " + flashSaleService.getStock(itemKey));

        System.out.println("Đợi 12s để kiểm tra timeout...");
        Thread.sleep(12000);

        long restoredStock = flashSaleService.getStock(itemKey);
        System.out.println("Sau 12s, kho hiện tại: " + restoredStock);

        assertThat(restoredStock).isEqualTo(1);
    }

    @Test
    @DisplayName("The ultimate stress test: Buying -> Dont't pay -> Rollback -> Buy again")
    void testComprehensiveFlashsaleLifecycle() throws InterruptedException {
        String itemKey = "flashsale:stock:CONCERT_VIP";
        int initialStock = 50;
        int wave1Users = 500;

        flashSaleService.initStock(itemKey, initialStock);
        assertThat(flashSaleService.getStock(itemKey)).isEqualTo(initialStock);

        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch wave1Done = new CountDownLatch(wave1Users);

        List<String> successfulOrders = new CopyOnWriteArrayList<>();
        AtomicInteger wave1Failed = new AtomicInteger(0);

        for (int i = 0;i < wave1Users;i++) {
            pool.submit(() -> {
                try {
                    startGun.await();
                    String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
                    String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);

                    DeductResult success = flashSaleService.tryDeductStock(itemKey, 1);
                    if (success.success()) {
                        successfulOrders.add(success.orderId());
                        redisTemplate.opsForValue().set("order:status:" + orderId, "UNPAID");
                    } else {
                        wave1Failed.incrementAndGet();
                    }
                } catch(Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    wave1Done.countDown();
                }
            });
        }

        startGun.countDown();
        wave1Done.await();

        assertThat(successfulOrders).hasSize(50);
        assertThat(wave1Failed.get()).isEqualTo(wave1Users - initialStock);
        assertThat(flashSaleService.getStock(itemKey)).isEqualTo(0);
        System.out.println("Wave 1 completed: Successful orders = " + successfulOrders.size() + ", Failed orders = " + wave1Failed.get());
        
        successfulOrders.forEach(orderId -> {
            String status = flashSaleService.getOrderStatus(orderId);
            assertThat(status).isEqualTo("UNPAID");
        });

        for (int i = 0;i < 30;i++) {
            flashSaleService.payOrder(successfulOrders.get(i));
        }
        System.out.println("30 orders paid, 20 orders unpaid. Waiting for 12s to trigger rollback...");
        Thread.sleep(12000);

        long restoredStock = flashSaleService.getStock(itemKey);
        System.out.println("After rollback, stock = " + restoredStock);
        assertThat(restoredStock).isEqualTo(20);

        CountDownLatch wave3Gun = new CountDownLatch(1);
        CountDownLatch wave3Done = new CountDownLatch(100);
        AtomicInteger wave3Success = new AtomicInteger(0);

        for (int i = 0;i < 100;i++) {
            pool.submit(() -> {
                try {
                    wave3Gun.await();
                
                    if (flashSaleService.tryDeductStock(itemKey, 1).success()) {
                        wave3Success.incrementAndGet();
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    wave3Done.countDown();
                }
            });
        }

        wave3Gun.countDown();
        wave3Done.await();
        pool.shutdown();

        assertThat(wave3Success.get()).isEqualTo(20);
        assertThat(flashSaleService.getStock(itemKey)).isEqualTo(0);

        System.out.println("Wave 3 completed: Successful orders = " + wave3Success.get() + ", Remaining stock = " + flashSaleService.getStock(itemKey));
        System.out.println("Comprehensive flash sale lifecycle test completed successfully.");

    }   
}

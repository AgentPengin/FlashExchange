package com.flashexchange;

import com.flashexchange.engine.OrderBook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FlashExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashExchangeApplication.class, args);
    }

    /**
     * Khởi tạo đối tượng OrderBook duy nhất (Singleton) trong bộ nhớ RAM
     * cho cặp giao dịch FLASH-USDT. 
     * Spring Boot sẽ tự động tiêm (Inject) đối tượng này vào Controller.
     */
    @Bean
    public OrderBook defaultOrderBook() {
        return new OrderBook("FLASH-USDT");
    }
}

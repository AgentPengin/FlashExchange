package com.flashexchange.controller;

import com.flashexchange.dto.DeductResult;
import com.flashexchange.service.FlashSaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController 
@RequestMapping ("/api/v1/flashsale")
public class FlashSaleController {
    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    /** *
     * POST http://localhost:8080/api/v1/flashsale/buy?itemKey=FLASH_PASS
    */

    @PostMapping("/buy")
    public ResponseEntity<?> buyFlashSale(@RequestParam(defaultValue = "FLASH_PASS") String itemKey, 
                                         @RequestHeader(value = "X-User-Id", defaultValue = "user-1") String userId) {
        DeductResult result = flashSaleService.tryDeductStock(userId, itemKey, 1);

        if (result.success()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "SUCCESS",
                "message", "Order placed successfully! Move to payment.",
                "orderId", result.orderId()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "status", "FAILED",
                "message", "Flash sale item is sold out or insufficient stock."
            ));
        }
    }

}

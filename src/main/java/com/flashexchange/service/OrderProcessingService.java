package com.flashexchange.service;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.flashexchange.dto.FlashSaleOrderMessage;

@Service
public class OrderProcessingService {

    private final JdbcTemplate jdbcTemplate;

    public OrderProcessingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void createOrUpdateWallet(String userId, long balance) {
        String sql = """
            INSERT INTO wallets (user_id, balance) 
            VALUES (?, ?) 
            ON CONFLICT (user_id) DO UPDATE SET balance = EXCLUDED.balance;
        """;
        jdbcTemplate.update(sql, userId, balance); 
    }

    public long getWalletBalance(String userId) {
        String sql = "SELECT balance FROM wallets WHERE user_id = ?";
        Long balance = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return balance != null ? balance : 0L;
    }


    @Transactional 
    public boolean processOrder(FlashSaleOrderMessage message, long itemPrice) {
        String checkSql = "SELECT COUNT(*) FROM orders WHERE order_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, message.orderId());
        if (count != null && count > 0) {
            System.out.println("Order: " + message.orderId() + " already exists. Skipping processing.");
            return false;
        }
        long totalAmount = itemPrice * message.quantity();

        String deductSql = "UPDATE wallets SET balance = balance - ? WHERE user_id = ? AND balance >= ?";
        int rowAffected = jdbcTemplate.update(deductSql, totalAmount, message.userId(), totalAmount);
        if (rowAffected == 0 ) {
            throw new IllegalStateException("Insufficient balance for user: " + message.userId());
        }

        long newBalance = getWalletBalance(message.userId());

        String ledgerSql = "INSERT INTO ledger_entries (order_id, user_id, amount, balance_after) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(ledgerSql, message.orderId(), message.userId(), -totalAmount, newBalance);

        String orderSql = "INSERT INTO orders (order_id, user_id, item_key, quantity, amount, status) VALUES (? , ?, ?, ?, ?, 'PAID')";
        jdbcTemplate.update(orderSql, message.orderId(), message.userId(), message.itemKey(), message.quantity(), totalAmount);

        System.out.println("Substracted " + totalAmount + " from user: " + message.userId() + ". New balance: " + newBalance + "$");
        return true;
    }
}

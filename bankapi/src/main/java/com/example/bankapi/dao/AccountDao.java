package com.example.bankapi.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public class AccountDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public BigDecimal getBalance(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT balance FROM accounts WHERE id = ?",
                    BigDecimal.class, userId);
        } catch (Exception e) {
            return BigDecimal.valueOf(-1);  // Ошибка
        }
    }

    public int putMoney(Long userId, BigDecimal amount) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE accounts SET balance = balance + ? WHERE id = ?",
                    amount, userId);
            return updated > 0 ? 1 : 0;  // Успех или ошибка
        } catch (Exception e) {
            return 0;
        }
    }

    public int takeMoney(Long userId, BigDecimal amount) {
        try {
            BigDecimal balance = getBalance(userId);
            if (balance.compareTo(amount) < 0) {
                return 0;  // Недостаточно средств
            }
            int updated = jdbcTemplate.update(
                    "UPDATE accounts SET balance = balance - ? WHERE id = ?",
                    amount, userId);
            return updated > 0 ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}

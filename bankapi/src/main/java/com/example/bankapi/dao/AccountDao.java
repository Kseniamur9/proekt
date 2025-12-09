package com.example.bankapi.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AccountDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    private static final Map<Integer, String> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put(1, "пополнение счета");
        TYPE_MAP.put(2, "снятие со счета");

    }

    public BigDecimal getBalance(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT balance FROM users WHERE user_id = ?",
                    BigDecimal.class, userId);
        } catch (Exception e) {
            return BigDecimal.valueOf(-1);
        }
    }

    @Transactional
    public int putMoney(Long userId, BigDecimal amount) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE users SET balance = balance + ? WHERE user_id = ?",
                    amount, userId);
            if (updated > 0) {
                jdbcTemplate.update(
                        "INSERT INTO operations (user_id, operation_type, amount) VALUES (?, 1, ?)",  // 1 = пополнение
                        userId, amount.longValue());  // Преобразование в long для BIGINT
                return 1;
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Error in putMoney", e);
        }
    }

    @Transactional
    public int takeMoney(Long userId, BigDecimal amount) {
        try {
            BigDecimal balance = getBalance(userId);
            if (balance.compareTo(amount) < 0 || balance.compareTo(BigDecimal.valueOf(-1)) == 0) {
                return 0;
            }
            int updated = jdbcTemplate.update(
                    "UPDATE users SET balance = balance - ? WHERE user_id = ?",
                    amount, userId);
            if (updated > 0) {
                jdbcTemplate.update(
                        "INSERT INTO operations (user_id, operation_type, amount) VALUES (?, 2, ?)",  // 2 = снятие
                        userId, amount.longValue());
                return 1;
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Error in takeMoney", e);
        }
    }

    public List<Map<String, Object>> getOperationList(Long userId, String startDate, String endDate) {
        String sql = "SELECT operation_date AS date, operation_type AS type, amount " +
                "FROM operations WHERE user_id = ?";
        Object[] params = {userId};
        if (startDate != null && !startDate.isEmpty()) {
            sql += " AND operation_date >= ?";
            params = append(params, Timestamp.valueOf(startDate + " 00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql += " AND operation_date <= ?";
            params = append(params, Timestamp.valueOf(endDate + " 23:59:59"));
        }
        sql += " ORDER BY operation_date DESC";

        return jdbcTemplate.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> op = new HashMap<>();
                op.put("date", rs.getTimestamp("date").toString());
                int typeInt = rs.getInt("type");
                op.put("type", TYPE_MAP.getOrDefault(typeInt, "Неизвестный тип"));  // Маппинг int -> строка
                op.put("amount", BigDecimal.valueOf(rs.getLong("amount")));  // Из long в BigDecimal
                return op;
            }
        });
    }

    private Object[] append(Object[] arr, Object element) {
        Object[] newArr = new Object[arr.length + 1];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        newArr[arr.length] = element;
        return newArr;
    }
}
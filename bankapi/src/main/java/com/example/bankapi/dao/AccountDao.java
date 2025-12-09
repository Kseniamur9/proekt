package com.example.bankapi.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;


@Repository
public class AccountDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Map<Integer, String> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put(1, "пополнение счета");
        TYPE_MAP.put(2, "снятие со счета");
        TYPE_MAP.put(3, "перевод (out)");
        TYPE_MAP.put(4, "перевод (in)");
    }

    public BigDecimal getBalance(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT balance FROM users WHERE user_id = ?",
                    BigDecimal.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return BigDecimal.valueOf(-1);
        } catch (Exception e) {
            return BigDecimal.valueOf(-1);
        }
    }

    @Transactional
    public int putMoney(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return 0;
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE users SET balance = balance + ? WHERE user_id = ?",
                    amount, userId);
            if (updated > 0) {
                jdbcTemplate.update(
                        "INSERT INTO operations (user_id, operation_type, amount, counterparty_id) VALUES (?, 1, ?, NULL)",
                        userId, amount);
                return 1;
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Error in putMoney", e);
        }
    }

    @Transactional
    public int takeMoney(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return 0;
        try {
            BigDecimal balance = jdbcTemplate.queryForObject(
                    "SELECT balance FROM users WHERE user_id = ? FOR UPDATE",
                    BigDecimal.class, userId);
            if (balance == null) return 0;
            if (balance.compareTo(amount) < 0) return 0;

            int updated = jdbcTemplate.update(
                    "UPDATE users SET balance = balance - ? WHERE user_id = ?",
                    amount, userId);
            if (updated > 0) {
                jdbcTemplate.update(
                        "INSERT INTO operations (user_id, operation_type, amount, counterparty_id) VALUES (?, 2, ?, NULL)",
                        userId, amount);
                return 1;
            }
            return 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Error in takeMoney", e);
        }
    }

    @Transactional
    public int transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (fromUserId == null || toUserId == null || amount == null) return 0;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return 0;
        if (fromUserId.equals(toUserId)) return 0;

        try {
            Long first = fromUserId < toUserId ? fromUserId : toUserId;
            Long second = fromUserId < toUserId ? toUserId : fromUserId;

            String sel = "SELECT user_id, balance FROM users WHERE user_id IN (?, ?) FOR UPDATE";
            List<UserBalance> list = jdbcTemplate.query(sel, new Object[]{first, second}, (rs, rn) ->
                    new UserBalance(rs.getLong("user_id"), rs.getBigDecimal("balance"))
            );

            Map<Long, BigDecimal> map = new HashMap<>();
            for (UserBalance ub : list) map.put(ub.userId, ub.balance);

            if (!map.containsKey(fromUserId) || !map.containsKey(toUserId)) return 0;

            BigDecimal fromBalance = map.get(fromUserId);
            if (fromBalance.compareTo(amount) < 0) return 0;

            int d1 = jdbcTemplate.update("UPDATE users SET balance = balance - ? WHERE user_id = ?", amount, fromUserId);
            int d2 = jdbcTemplate.update("UPDATE users SET balance = balance + ? WHERE user_id = ?", amount, toUserId);
            if (d1 <= 0 || d2 <= 0) throw new RuntimeException("Failed to update balances");

            jdbcTemplate.update(
                    "INSERT INTO operations (user_id, operation_type, amount, counterparty_id) VALUES (?, 3, ?, ?)",
                    fromUserId, amount, toUserId);
            jdbcTemplate.update(
                    "INSERT INTO operations (user_id, operation_type, amount, counterparty_id) VALUES (?, 4, ?, ?)",
                    toUserId, amount, fromUserId);

            return 1;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Error in transferMoney", e);
        }
    }

    public List<Map<String, Object>> getOperationList(Long userId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT operation_date AS date, operation_type AS type, amount, counterparty_id " +
                        "FROM operations WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startDate != null && !startDate.isEmpty()) {
            try {
                LocalDate sd = LocalDate.parse(startDate);
                sql.append(" AND operation_date >= ?");
                params.add(Timestamp.valueOf(sd.atStartOfDay()));
            } catch (DateTimeParseException ex) {
                // ignore invalid format
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                LocalDate ed = LocalDate.parse(endDate);
                sql.append(" AND operation_date <= ?");
                params.add(Timestamp.valueOf(ed.atTime(23,59,59)));
            } catch (DateTimeParseException ex) {
                // ignore
            }
        }

        sql.append(" ORDER BY operation_date DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> op = new HashMap<>();
                Timestamp ts = rs.getTimestamp("date");
                op.put("date", ts != null ? ts.toString() : null);
                int typeInt = rs.getInt("type");
                op.put("type", TYPE_MAP.getOrDefault(typeInt, "Неизвестный тип"));
                op.put("amount", rs.getBigDecimal("amount"));
                Long cp = rs.getObject("counterparty_id") == null ? null : rs.getLong("counterparty_id");
                op.put("counterpartyId", cp);
                return op;
            }
        });
    }

    private static class UserBalance {
        Long userId;
        BigDecimal balance;
        UserBalance(Long userId, BigDecimal balance) {
            this.userId = userId;
            this.balance = balance;
        }
    }
}

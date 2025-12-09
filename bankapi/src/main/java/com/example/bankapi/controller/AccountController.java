package com.example.bankapi.controller;

import com.example.bankapi.dao.AccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountDao accountDao;

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@RequestParam Long id) {
        BigDecimal balance = accountDao.getBalance(id);
        Map<String, Object> response = new HashMap<>();
        if (balance.compareTo(BigDecimal.valueOf(-1)) == 0) {
            response.put("value", -1);
            response.put("error", "User not found or error");
        } else {
            response.put("value", balance);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/put")
    public ResponseEntity<Map<String, Object>> putMoney(@RequestParam Long id, @RequestParam BigDecimal amount) {
        int result = accountDao.putMoney(id, amount);
        Map<String, Object> response = new HashMap<>();
        response.put("value", result);
        if (result == 0) {
            response.put("error", "Error adding money");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/take")
    public ResponseEntity<Map<String, Object>> takeMoney(@RequestParam Long id, @RequestParam BigDecimal amount) {
        int result = accountDao.takeMoney(id, amount);
        Map<String, Object> response = new HashMap<>();
        response.put("value", result);
        if (result == 0) {
            response.put("error", "Insufficient funds or error");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operationList")
    public ResponseEntity<List<Map<String, Object>>> getOperationList(
            @RequestParam Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> operations = accountDao.getOperationList(id, startDate, endDate);
        return ResponseEntity.ok(operations);
    }
}
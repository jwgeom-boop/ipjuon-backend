package com.ipjuon.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    // 은행별 계정: username -> {password, 은행명}
    private static final Map<String, String[]> BANK_ACCOUNTS = Map.of(
        "shinhan",  new String[]{"shinhan2024!",  "신한은행"},
        "hana",     new String[]{"hana2024!",     "하나은행"},
        "kb",       new String[]{"kb2024!",       "KB국민은행"},
        "woori",    new String[]{"woori2024!",    "우리은행"},
        "nh",       new String[]{"nh2024!",       "NH농협은행"},
        "ibk",      new String[]{"ibk2024!",      "IBK기업은행"},
        "busan",    new String[]{"busan2024!",    "부산은행"},
        "daegu",    new String[]{"daegu2024!",    "대구은행"}
    );

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            return Map.of("success", true, "role", "admin", "token", "admin-authenticated");
        }

        String[] bankInfo = BANK_ACCOUNTS.get(username);
        if (bankInfo != null && bankInfo[0].equals(password)) {
            return Map.of("success", true, "role", "bank",
                    "bank_name", bankInfo[1], "token", "bank-authenticated");
        }

        return Map.of("success", false, "message", "아이디 또는 비밀번호가 올바르지 않습니다");
    }
}

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

    @Value("${bank.username}")
    private String bankUsername;

    @Value("${bank.password}")
    private String bankPassword;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            return Map.of("success", true, "role", "admin", "token", "admin-authenticated");
        }
        if (bankUsername.equals(username) && bankPassword.equals(password)) {
            return Map.of("success", true, "role", "bank", "token", "bank-authenticated");
        }
        return Map.of("success", false, "message", "아이디 또는 비밀번호가 올바르지 않습니다");
    }
}

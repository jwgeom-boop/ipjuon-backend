package com.ipjuon.backend.auth;

import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private final VendorRepository vendorRepository;

    public AuthController(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 관리자 로그인
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            return Map.of("success", true, "role", "admin", "token", "admin-authenticated");
        }

        // 은행 계정 로그인 (vendor_accounts 테이블 조회)
        Optional<Vendor> vendorOpt = vendorRepository.findByLoginId(username);
        if (vendorOpt.isPresent()) {
            Vendor vendor = vendorOpt.get();
            if ("은행".equals(vendor.getVendorType())
                    && password.equals(vendor.getPassword())
                    && "active".equals(vendor.getStatus())) {
                return Map.of(
                    "success", true,
                    "role", "bank",
                    "bank_name", vendor.getVendorName(),
                    "token", "bank-authenticated"
                );
            }
        }

        return Map.of("success", false, "message", "아이디 또는 비밀번호가 올바르지 않습니다");
    }
}

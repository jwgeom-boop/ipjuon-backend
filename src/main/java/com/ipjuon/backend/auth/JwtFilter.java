package com.ipjuon.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS preflight 요청은 인증 불필요
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getServletPath();
        // POST /api/invite는 현장 앱에서 호출 (인증 불필요), GET은 관리자용이라 인증 필요
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/invite".equals(path)) return true;
        return path.startsWith("/api/auth/") || path.startsWith("/api/consultation");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            sendUnauthorized(response, "인증이 필요합니다");
            return;
        }

        String token = header.substring(7);
        if (!jwtUtil.isValid(token)) {
            sendUnauthorized(response, "유효하지 않은 토큰입니다");
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}

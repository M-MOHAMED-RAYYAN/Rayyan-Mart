package com.shopping.system.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.system.dto.ApiError;
import com.shopping.system.service.SessionService;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final String PUBLIC_LOGIN = "/api/auth/login";
    private static final String PUBLIC_REGISTER = "/api/auth/register";
    private static final String PUBLIC_HEALTH = "/api/v1/health";
    private static final String PUBLIC_CHAT = "/api/chat";

    private final ObjectMapper objectMapper;

    public AuthFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || PUBLIC_LOGIN.equals(path)
                || PUBLIC_REGISTER.equals(path)
                || PUBLIC_HEALTH.equals(path)
                || PUBLIC_CHAT.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/api/")) {
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute(SessionService.USER_ID_ATTR) == null) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized", "Please log in to continue");
                return;
            }

            if (path.startsWith("/api/admin/")) {
                Object role = session.getAttribute(SessionService.ROLE_ATTR);
                if (!"ADMIN".equals(role)) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN,
                            "Forbidden", "Admin access required");
                    return;
                }
            }

            if (path.startsWith("/api/seller/")) {
                Object role = session.getAttribute(SessionService.ROLE_ATTR);
                if (!"SELLER".equals(role)) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN,
                            "Forbidden", "Seller access required");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status,
                            String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ApiError apiError = new ApiError(LocalDateTime.now(), status, error, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }
}

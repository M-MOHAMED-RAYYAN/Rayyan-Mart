package com.shopping.system.service;

import com.shopping.system.dto.LoginRequest;
import com.shopping.system.dto.LoginResponse;
import com.shopping.system.dto.RegisterRequest;
import com.shopping.system.dto.UserResponse;
import com.shopping.system.exception.DuplicateEmailException;
import com.shopping.system.exception.UnauthorizedException;
import com.shopping.system.model.Cart;
import com.shopping.system.model.Role;
import com.shopping.system.model.User;
import com.shopping.system.repository.CartRepository;
import com.shopping.system.repository.UserRepository;
import javax.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository,
                       CartRepository cartRepository,
                       PasswordEncoder passwordEncoder,
                       SessionService sessionService) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(resolveRole(request.role()));
        user = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        return UserResponse.from(user);
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        sessionService.login(session, user);
        return new LoginResponse(UserResponse.from(user), user.getRole().name(), "Login successful");
    }

    public void logout(HttpSession session) {
        sessionService.logout(session);
    }

    public UserResponse me(HttpSession session) {
        return UserResponse.from(sessionService.getCurrentUser(session));
    }

    private Role resolveRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return Role.CUSTOMER;
        }
        String normalized = requestedRole.trim().toUpperCase();
        if ("SELLER".equals(normalized)) {
            return Role.SELLER;
        }
        if ("CUSTOMER".equals(normalized)) {
            return Role.CUSTOMER;
        }
        // Never allow self-registration as ADMIN
        throw new IllegalArgumentException("Invalid role. Allowed roles: CUSTOMER, SELLER");
    }
}

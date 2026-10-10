package com.shopping.system.service;

import com.shopping.system.exception.ForbiddenException;
import com.shopping.system.exception.UnauthorizedException;
import com.shopping.system.model.Role;
import com.shopping.system.model.User;
import com.shopping.system.repository.UserRepository;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionService {

    public static final String USER_ID_ATTR = "userId";
    public static final String ROLE_ATTR = "role";

    private final UserRepository userRepository;

    public SessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void login(HttpSession session, User user) {
        session.setAttribute(USER_ID_ATTR, user.getId());
        session.setAttribute(ROLE_ATTR, user.getRole().name());
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(USER_ID_ATTR) != null;
    }

    public Long getCurrentUserId(HttpSession session) {
        if (session == null || session.getAttribute(USER_ID_ATTR) == null) {
            throw new UnauthorizedException("Please log in to continue");
        }
        return (Long) session.getAttribute(USER_ID_ATTR);
    }

    public User getCurrentUser(HttpSession session) {
        Long userId = getCurrentUserId(session);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User account no longer exists"));
    }

    public User getCurrentAdmin(HttpSession session) {
        User user = getCurrentUser(session);
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
        return user;
    }

    public User getCurrentSeller(HttpSession session) {
        User user = getCurrentUser(session);
        if (user.getRole() != Role.SELLER) {
            throw new ForbiddenException("Seller access required");
        }
        return user;
    }
}

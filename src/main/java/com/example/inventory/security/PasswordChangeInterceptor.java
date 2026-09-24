package com.example.inventory.security;

import com.example.inventory.entity.User;
import com.example.inventory.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class PasswordChangeInterceptor implements HandlerInterceptor {

    /** Paths that must remain accessible while a password change is pending. */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/account/change-password",
            "/logout",
            "/login",
            "/error"
    );

    private final UserRepository userRepository;

    public PasswordChangeInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();

        // Always allow static resources and permit-listed paths
        if (path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/fonts/")
                || ALLOWED_PATHS.contains(path)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || !user.isMustChangePassword()) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/account/change-password");
        return false;
    }
}
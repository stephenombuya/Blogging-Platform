package com.blogplatform.util;

import com.blogplatform.exception.AccessDeniedException;
import com.blogplatform.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtil {

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    }

    public boolean isAdmin() {
        return getCurrentUser()
                .map(u -> u.getRole() == User.Role.ROLE_ADMIN)
                .orElse(false);
    }

    public boolean isModerator() {
        return getCurrentUser()
                .map(u -> u.getRole() == User.Role.ROLE_MODERATOR || u.getRole() == User.Role.ROLE_ADMIN)
                .orElse(false);
    }

    public boolean isCurrentUser(Long userId) {
        return getCurrentUser()
                .map(u -> u.getId().equals(userId))
                .orElse(false);
    }

    public boolean isOwnerOrAdmin(Long ownerId) {
        return getCurrentUser()
                .map(u -> u.getId().equals(ownerId) || u.getRole() == User.Role.ROLE_ADMIN)
                .orElse(false);
    }
}

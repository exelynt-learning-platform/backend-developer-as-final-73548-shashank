package com.booking.resourcebooking.security;

import com.booking.resourcebooking.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Central place to pull the authenticated {@link User} out of the SecurityContext.
 * This is the ONLY source of truth for "who is making this request" — reservation
 * ownership must never be taken from request bodies.
 */
@Component
public class CurrentUserProvider {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return user;
    }

    public boolean isAdmin() {
        return getCurrentUser().getRole().name().equals("ADMIN");
    }
}

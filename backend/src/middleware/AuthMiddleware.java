package middleware;

import com.sun.net.httpserver.*;
import models.User;
import repositories.UserRepository;
import utils.JsonUtil;
import utils.SessionManager;

import java.io.IOException;

/**
 * AuthMiddleware — checks session authentication and role authorization.
 * Wraps a handler to enforce access control before the actual request processing.
 * Demonstrates inheritance (extends from a base concept) and the Decorator pattern.
 */
public class AuthMiddleware {

    private static final UserRepository userRepo = new UserRepository();

    /**
     * Wrap a handler to require authentication.
     * The authenticated User is stored as an attribute on the exchange.
     */
    public static HttpHandler requireAuth(HttpHandler handler) {
        return exchange -> {
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                JsonUtil.sendError(exchange, 401, "Authentication required.");
                return;
            }
            // Store user in exchange attributes for downstream use
            exchange.setAttribute("user", user);
            handler.handle(exchange);
        };
    }

    /**
     * Wrap a handler to require a specific role.
     */
    public static HttpHandler requireRole(HttpHandler handler, String... allowedRoles) {
        return exchange -> {
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                JsonUtil.sendError(exchange, 401, "Authentication required.");
                return;
            }

            // Check if user has any of the allowed roles
            boolean authorized = false;
            for (String role : allowedRoles) {
                if (user.hasRole(role)) {
                    authorized = true;
                    break; // using break (loop control — Sem III)
                }
            }

            if (!authorized) {
                JsonUtil.sendError(exchange, 403, "You do not have permission to access this resource.");
                return;
            }

            exchange.setAttribute("user", user);
            handler.handle(exchange);
        };
    }

    /**
     * Extract the authenticated user from the session cookie.
     * Returns null if not authenticated.
     */
    public static User getAuthenticatedUser(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        String token = SessionManager.extractToken(cookieHeader);

        if (token == null) return null;

        int userId = SessionManager.getUserId(token);
        if (userId < 0) return null;

        try {
            User user = userRepo.findById(userId);
            if (user == null || !user.isActive() || !user.isApproved()) return null;
            return user;
        } catch (Exception e) {
            System.err.println("[Auth] Error looking up user: " + e.getMessage());
            return null;
        }
    }
}

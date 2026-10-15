package utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager — manages user sessions with secure tokens.
 * Sessions are stored both in-memory (for fast lookup) and in the database (for persistence).
 * Extension beyond Sem III: session management is essential for web security.
 */
public class SessionManager {

    // In-memory session cache: sessionToken -> userId
    private static final ConcurrentHashMap<String, Integer> sessions = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();

    // Session duration: 24 hours in milliseconds
    public static final long SESSION_DURATION_MS = 24 * 60 * 60 * 1000;

    /**
     * Create a new session for a user.
     * @param userId the user's database ID
     * @return the session token
     */
    public static String createSession(int userId) {
        // Generate a cryptographically secure random token
        byte[] tokenBytes = new byte[64];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        sessions.put(token, userId);
        return token;
    }

    /**
     * Get the user ID associated with a session token.
     * @param token the session token
     * @return the user ID, or -1 if the session is invalid
     */
    public static int getUserId(String token) {
        if (token == null || token.isEmpty()) return -1;
        Integer userId = sessions.get(token);
        return userId != null ? userId : -1;
    }

    /**
     * Invalidate (destroy) a session.
     * @param token the session token
     */
    public static void destroySession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /**
     * Check if a session token is valid.
     * @param token the session token
     * @return true if valid
     */
    public static boolean isValid(String token) {
        return token != null && sessions.containsKey(token);
    }

    /**
     * Extract session token from the Cookie header.
     * @param cookieHeader the raw Cookie header value
     * @return the session token, or null
     */
    public static String extractToken(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith("SESSION=")) {
                return trimmed.substring("SESSION=".length());
            }
        }
        return null;
    }

    /**
     * Build the Set-Cookie header value for a session.
     * @param token the session token
     * @return the Set-Cookie header value
     */
    public static String buildSessionCookie(String token) {
        long maxAge = SESSION_DURATION_MS / 1000;
        return "SESSION=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + maxAge;
    }

    /**
     * Build a Set-Cookie header that clears the session.
     * @return the Set-Cookie header value
     */
    public static String buildClearCookie() {
        return "SESSION=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0";
    }
}

package handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.User;
import repositories.UserRepository;
import repositories.GeneralRepository;
import utils.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AuthHandler — handles Google OAuth sign-in, session management, and logout.
 * Extension beyond Sem III: OAuth and JWT verification.
 */
public class AuthHandler implements HttpHandler {

    private final UserRepository userRepo = new UserRepository();
    private final GeneralRepository generalRepo = new GeneralRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/api/auth/google") && "POST".equals(method)) {
                handleGoogleSignIn(exchange);
            } else if (path.equals("/api/auth/dev-login") && "POST".equals(method)) {
                handleDevLogin(exchange);
            } else if (path.equals("/api/auth/me") && "GET".equals(method)) {
                handleGetMe(exchange);
            } else if (path.equals("/api/auth/logout") && "POST".equals(method)) {
                handleLogout(exchange);
            } else if (path.equals("/api/config") && "GET".equals(method)) {
                handleGetConfig(exchange);
            } else {
                JsonUtil.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            System.err.println("[Auth] Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    /** Handle Google sign-in: verify token, create/update user, create session */
    private void handleGoogleSignIn(HttpExchange exchange) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String credential = JsonUtil.getString(body, "credential", "");

        if (credential.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Missing credential");
            return;
        }

        // Verify the Google ID token using Google's tokeninfo endpoint
        // This is the simplest approach suitable for a student project
        Map<String, String> tokenInfo = verifyGoogleToken(credential);

        if (tokenInfo == null || tokenInfo.get("email") == null) {
            JsonUtil.sendError(exchange, 401, "Invalid Google token");
            return;
        }

        String email = tokenInfo.get("email");
        String name = tokenInfo.getOrDefault("name", email.split("@")[0]);
        String picture = tokenInfo.getOrDefault("picture", "");
        String googleId = tokenInfo.getOrDefault("sub", "");

        // Check if user exists
        User user = userRepo.findByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            // Create new user — default role FACULTY, not approved
            user = new User();
            user.setGoogleId(googleId);
            user.setEmail(email);
            user.setName(name);
            user.setProfilePicture(picture);
            user.setRole("FACULTY");
            user.setActive(true);

            // First user becomes admin (auto-approved)
            int totalUsers = userRepo.countAll();
            if (totalUsers == 0) {
                user.setRole("ADMIN");
                user.setApproved(true);
            } else {
                user.setApproved(false);
            }

            user = userRepo.create(user);

            // Audit log
            generalRepo.addAuditLog(user.getId(), "USER_REGISTERED", "USER", user.getId(),
                    "New user registered via Google: " + email, getClientIP(exchange));

            if (!user.isApproved()) {
                response.put("status", "pending_approval");
                response.put("message", "Account created. Waiting for admin approval.");
                JsonUtil.sendJson(exchange, 200, response);
                return;
            }
        } else {
            // Update profile from Google
            userRepo.updateGoogleProfile(user.getId(), name, picture, googleId);
            user.setName(name);
            user.setProfilePicture(picture);
        }

        // Check if user is active and approved
        if (!user.isActive()) {
            response.put("status", "inactive");
            JsonUtil.sendJson(exchange, 200, response);
            return;
        }
        if (!user.isApproved()) {
            response.put("status", "pending_approval");
            JsonUtil.sendJson(exchange, 200, response);
            return;
        }

        // Initialize leave balances if needed
        if (user.getDesignation() != null && !user.getDesignation().isEmpty()) {
            generalRepo.initializeBalances(user.getId(), user.getDesignation(), DateUtil.currentAcademicYear());
        }

        // Create session
        String sessionToken = SessionManager.createSession(user.getId());
        exchange.getResponseHeaders().add("Set-Cookie", SessionManager.buildSessionCookie(sessionToken));

        // Audit log
        generalRepo.addAuditLog(user.getId(), "USER_LOGIN", "USER", user.getId(),
                "User signed in via Google", getClientIP(exchange));

        // Build response
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole());
        userMap.put("profile_picture", user.getProfilePicture());
        userMap.put("department_name", user.getDepartmentName());
        userMap.put("designation", user.getDesignation());
        userMap.put("is_approved", user.isApproved());

        response.put("status", "success");
        response.put("user", userMap);
        JsonUtil.sendJson(exchange, 200, response);
    }

    /**
     * Demo / Dev login for evaluation, testing, and viva presentations.
     */
    private void handleDevLogin(HttpExchange exchange) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String email = JsonUtil.getString(body, "email", "").trim();
        String role = JsonUtil.getString(body, "role", "").trim();

        User user = null;
        if (!email.isEmpty()) {
            user = userRepo.findByEmail(email);
        } else if (!role.isEmpty()) {
            List<User> list = userRepo.findAll(role);
            if (!list.isEmpty()) user = list.get(0);
        }

        if (user == null) {
            user = new User();
            user.setEmail(email.isEmpty() ? "admin@institution.edu" : email);
            user.setName("Administrator");
            user.setRole(role.isEmpty() ? "ADMIN" : role);
            user.setDesignation("Professor");
            user.setActive(true);
            user.setApproved(true);
            user = userRepo.create(user);
        }

        if (user.getDesignation() != null && !user.getDesignation().isEmpty()) {
            generalRepo.initializeBalances(user.getId(), user.getDesignation(), DateUtil.currentAcademicYear());
        }

        String sessionToken = SessionManager.createSession(user.getId());
        exchange.getResponseHeaders().add("Set-Cookie", SessionManager.buildSessionCookie(sessionToken));

        generalRepo.addAuditLog(user.getId(), "USER_DEV_LOGIN", "USER", user.getId(),
                "User signed in via dev-login: " + user.getEmail(), getClientIP(exchange));

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole());
        userMap.put("profile_picture", user.getProfilePicture());
        userMap.put("department_name", user.getDepartmentName());
        userMap.put("designation", user.getDesignation());
        userMap.put("is_approved", user.isApproved());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("user", userMap);
        JsonUtil.sendJson(exchange, 200, response);
    }

    /** Get current authenticated user info */
    private void handleGetMe(HttpExchange exchange) throws Exception {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        String token = SessionManager.extractToken(cookieHeader);

        if (token == null) {
            JsonUtil.sendError(exchange, 401, "Not authenticated");
            return;
        }

        int userId = SessionManager.getUserId(token);
        if (userId < 0) {
            JsonUtil.sendError(exchange, 401, "Invalid session");
            return;
        }

        User user = userRepo.findById(userId);
        if (user == null) {
            JsonUtil.sendError(exchange, 401, "User not found");
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole());
        userMap.put("profile_picture", user.getProfilePicture());
        userMap.put("department_id", user.getDepartmentId());
        userMap.put("department_name", user.getDepartmentName());
        userMap.put("designation", user.getDesignation());
        userMap.put("approver_id", user.getApproverId());
        userMap.put("is_approved", user.isApproved());
        userMap.put("is_active", user.isActive());

        JsonUtil.sendJson(exchange, 200, userMap);
    }

    /** Handle logout — destroy session */
    private void handleLogout(HttpExchange exchange) throws IOException {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        String token = SessionManager.extractToken(cookieHeader);
        SessionManager.destroySession(token);

        exchange.getResponseHeaders().add("Set-Cookie", SessionManager.buildClearCookie());
        JsonUtil.sendJson(exchange, 200, Map.of("status", "logged_out"));
    }

    /** Return app config (Google Client ID) */
    private void handleGetConfig(HttpExchange exchange) throws IOException {
        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        Map<String, Object> config = new HashMap<>();
        config.put("googleClientId", clientId != null ? clientId : "");
        JsonUtil.sendJson(exchange, 200, config);
    }

    /**
     * Verify a Google ID token using Google's tokeninfo endpoint.
     * In production, you would verify the JWT locally using Google's public keys.
     */
    private Map<String, String> verifyGoogleToken(String idToken) {
        try {
            URL url = URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8)).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("[Auth] Google token verification failed: HTTP " + status);
                return null;
            }

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            // Verify audience matches our client ID
            String expectedClientId = System.getenv("GOOGLE_CLIENT_ID");
            String audience = json.has("aud") ? json.get("aud").getAsString() : "";
            if (expectedClientId != null && !expectedClientId.isEmpty() && !audience.equals(expectedClientId)) {
                System.err.println("[Auth] Token audience mismatch: " + audience);
                return null;
            }

            // Extract claims
            Map<String, String> claims = new HashMap<>();
            claims.put("sub", json.has("sub") ? json.get("sub").getAsString() : "");
            claims.put("email", json.has("email") ? json.get("email").getAsString() : "");
            claims.put("name", json.has("name") ? json.get("name").getAsString() : "");
            claims.put("picture", json.has("picture") ? json.get("picture").getAsString() : "");
            claims.put("email_verified", json.has("email_verified") ? json.get("email_verified").getAsString() : "false");

            return claims;

        } catch (Exception e) {
            System.err.println("[Auth] Token verification error: " + e.getMessage());
            return null;
        }
    }

    /** Get client IP address */
    private String getClientIP(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null) return forwarded.split(",")[0].trim();
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }
}

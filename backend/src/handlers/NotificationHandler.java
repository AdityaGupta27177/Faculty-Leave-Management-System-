package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.User;
import repositories.GeneralRepository;
import utils.JsonUtil;

import java.io.IOException;
import java.util.*;

/**
 * NotificationHandler — manages user notifications and read receipts.
 */
public class NotificationHandler implements HttpHandler {

    private final GeneralRepository generalRepo = new GeneralRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        User user = (User) exchange.getAttribute("user");

        try {
            if (path.equals("/api/notifications") && "GET".equals(method)) {
                List<Map<String, Object>> notifs = generalRepo.getNotifications(user.getId());
                JsonUtil.sendJson(exchange, 200, notifs);
            } else if (path.matches("/api/notifications/\\d+/read") && "PUT".equals(method)) {
                String[] parts = path.split("/");
                int notifId = Integer.parseInt(parts[3]);
                generalRepo.markNotificationRead(notifId, user.getId());
                JsonUtil.sendJson(exchange, 200, Map.of("message", "Marked as read"));
            } else {
                JsonUtil.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            System.err.println("[NotificationHandler] Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}

package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Department;
import models.LeaveType;
import repositories.GeneralRepository;
import utils.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * CommonHandler — handles public and shared reference endpoints:
 * /api/departments, /api/leave-types, and /api/holidays.
 */
public class CommonHandler implements HttpHandler {

    private final GeneralRepository generalRepo = new GeneralRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (!"GET".equals(method)) {
            JsonUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            if (path.equals("/api/departments")) {
                List<Department> departments = generalRepo.findAllDepartments();
                JsonUtil.sendJson(exchange, 200, departments);
            } else if (path.equals("/api/leave-types")) {
                List<LeaveType> leaveTypes = generalRepo.findAllLeaveTypes();
                JsonUtil.sendJson(exchange, 200, leaveTypes);
            } else if (path.equals("/api/holidays")) {
                List<Map<String, Object>> holidays = generalRepo.findAllHolidays();
                JsonUtil.sendJson(exchange, 200, holidays);
            } else {
                JsonUtil.sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            System.err.println("[CommonHandler] Error handling " + path + ": " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}

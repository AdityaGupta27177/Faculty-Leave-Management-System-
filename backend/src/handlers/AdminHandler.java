package handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.*;
import repositories.*;
import utils.*;

import java.io.IOException;
import java.util.*;

/**
 * AdminHandler — handles all administrative operations:
 * User management, department configuration, leave policy management,
 * holiday management, system reports, and audit logs.
 */
public class AdminHandler implements HttpHandler {

    private final UserRepository userRepo = new UserRepository();
    private final GeneralRepository generalRepo = new GeneralRepository();
    private final LeaveRepository leaveRepo = new LeaveRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        User admin = (User) exchange.getAttribute("user");

        try {
            // Stats & Overview
            if (path.equals("/api/admin/stats") && "GET".equals(method)) {
                handleStats(exchange);
            } else if (path.equals("/api/admin/applications") && "GET".equals(method)) {
                handleApplications(exchange);
            }
            // User Management
            else if (path.equals("/api/admin/users") && "GET".equals(method)) {
                handleGetUsers(exchange);
            } else if (path.equals("/api/admin/users") && "POST".equals(method)) {
                handleCreateUser(exchange, admin);
            } else if (path.matches("/api/admin/users/\\d+") && "GET".equals(method)) {
                handleGetUserById(exchange);
            } else if (path.matches("/api/admin/users/\\d+") && "PUT".equals(method)) {
                handleUpdateUser(exchange, admin);
            }
            // Department Management
            else if (path.equals("/api/admin/departments") && "POST".equals(method)) {
                handleCreateDepartment(exchange, admin);
            } else if (path.matches("/api/admin/departments/\\d+") && "PUT".equals(method)) {
                handleUpdateDepartment(exchange, admin);
            }
            // Leave Type Management
            else if (path.equals("/api/admin/leave-types") && "POST".equals(method)) {
                handleCreateLeaveType(exchange, admin);
            } else if (path.matches("/api/admin/leave-types/\\d+") && "PUT".equals(method)) {
                handleUpdateLeaveType(exchange, admin);
            }
            // Policy Management
            else if (path.equals("/api/admin/policies") && "GET".equals(method)) {
                handleGetPolicies(exchange);
            } else if (path.equals("/api/admin/policies") && "POST".equals(method)) {
                handleCreatePolicy(exchange, admin);
            } else if (path.matches("/api/admin/policies/\\d+") && "PUT".equals(method)) {
                handleUpdatePolicy(exchange, admin);
            }
            // Holiday Management
            else if (path.equals("/api/admin/holidays") && "POST".equals(method)) {
                handleCreateHoliday(exchange, admin);
            } else if (path.matches("/api/admin/holidays/\\d+") && "DELETE".equals(method)) {
                handleDeleteHoliday(exchange, admin);
            }
            // Reports & Logs
            else if (path.equals("/api/admin/reports") && "GET".equals(method)) {
                handleReports(exchange);
            } else if (path.equals("/api/admin/audit-logs") && "GET".equals(method)) {
                handleAuditLogs(exchange);
            } else {
                JsonUtil.sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            System.err.println("[AdminHandler] Error processing " + path + ": " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleStats(HttpExchange exchange) throws Exception {
        Map<String, Object> stats = generalRepo.getAdminStats();
        JsonUtil.sendJson(exchange, 200, stats);
    }

    private void handleApplications(HttpExchange exchange) throws Exception {
        Map<String, String> query = JsonUtil.parseQueryParams(exchange.getRequestURI());
        int limit = 50;
        if (query.containsKey("limit")) {
            try { limit = Integer.parseInt(query.get("limit")); } catch (NumberFormatException ignored) {}
        }
        List<LeaveApplication> apps = leaveRepo.findAll(limit);
        JsonUtil.sendJson(exchange, 200, apps);
    }

    private void handleGetUsers(HttpExchange exchange) throws Exception {
        Map<String, String> query = JsonUtil.parseQueryParams(exchange.getRequestURI());
        String role = query.get("role");
        List<User> users = userRepo.findAll(role);
        JsonUtil.sendJson(exchange, 200, users);
    }

    private void handleGetUserById(HttpExchange exchange) throws Exception {
        int id = extractIdFromPath(exchange.getRequestURI().getPath());
        User user = userRepo.findById(id);
        if (user == null) {
            JsonUtil.sendError(exchange, 404, "User not found");
            return;
        }
        JsonUtil.sendJson(exchange, 200, user);
    }

    private void handleCreateUser(HttpExchange exchange, User admin) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String email = JsonUtil.getString(body, "email", "").trim();
        String name = JsonUtil.getString(body, "name", "").trim();
        String designation = JsonUtil.getString(body, "designation", "").trim();
        String role = JsonUtil.getString(body, "role", "FACULTY").trim();
        int deptId = JsonUtil.getInt(body, "department_id", 0);
        int approverId = JsonUtil.getInt(body, "approver_id", 0);
        boolean isApproved = body.has("is_approved") ? body.get("is_approved").getAsBoolean() : true;

        if (email.isEmpty() || name.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Name and Email are required");
            return;
        }

        User existing = userRepo.findByEmail(email);
        if (existing != null) {
            JsonUtil.sendError(exchange, 400, "A user with this email already exists");
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setDepartmentId(deptId);
        user.setDesignation(designation);
        user.setRole(role);
        user.setApproverId(approverId);
        user.setActive(true);
        user.setApproved(isApproved);

        User created = userRepo.create(user);

        if (designation != null && !designation.isEmpty()) {
            generalRepo.initializeBalances(created.getId(), designation, DateUtil.currentAcademicYear());
        }

        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "CREATE_USER", "USER",
                created.getId(), "Admin created user: " + email + " (" + role + ")", getClientIP(exchange));

        JsonUtil.sendJson(exchange, 201, created);
    }

    private void handleUpdateUser(HttpExchange exchange, User admin) throws Exception {
        int id = extractIdFromPath(exchange.getRequestURI().getPath());
        User existing = userRepo.findById(id);
        if (existing == null) {
            JsonUtil.sendError(exchange, 404, "User not found");
            return;
        }

        JsonObject body = JsonUtil.readJsonBody(exchange);
        if (body.has("name")) existing.setName(body.get("name").getAsString().trim());
        if (body.has("department_id") && !body.get("department_id").isJsonNull()) {
            existing.setDepartmentId(body.get("department_id").getAsInt());
        }
        if (body.has("designation") && !body.get("designation").isJsonNull()) {
            existing.setDesignation(body.get("designation").getAsString().trim());
        }
        if (body.has("role")) existing.setRole(body.get("role").getAsString().trim());
        if (body.has("approver_id")) {
            if (body.get("approver_id").isJsonNull()) existing.setApproverId(0);
            else existing.setApproverId(body.get("approver_id").getAsInt());
        }
        if (body.has("is_approved")) existing.setApproved(body.get("is_approved").getAsBoolean());
        if (body.has("is_active")) existing.setActive(body.get("is_active").getAsBoolean());

        userRepo.update(existing);

        if (existing.getDesignation() != null && !existing.getDesignation().isEmpty()) {
            generalRepo.initializeBalances(existing.getId(), existing.getDesignation(), DateUtil.currentAcademicYear());
        }

        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "UPDATE_USER", "USER",
                existing.getId(), "Admin updated user: " + existing.getEmail(), getClientIP(exchange));

        JsonUtil.sendJson(exchange, 200, existing);
    }

    private void handleCreateDepartment(HttpExchange exchange, User admin) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String name = JsonUtil.getString(body, "name", "").trim();
        String code = JsonUtil.getString(body, "code", "").trim();
        if (name.isEmpty() || code.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Department name and code are required");
            return;
        }
        generalRepo.createDepartment(name, code);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "CREATE_DEPARTMENT", "DEPARTMENT",
                null, "Created department: " + name + " (" + code + ")", getClientIP(exchange));
        JsonUtil.sendJson(exchange, 201, Map.of("message", "Department created successfully"));
    }

    private void handleUpdateDepartment(HttpExchange exchange, User admin) throws Exception {
        int id = extractIdFromPath(exchange.getRequestURI().getPath());
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String name = JsonUtil.getString(body, "name", "").trim();
        String code = JsonUtil.getString(body, "code", "").trim();
        if (name.isEmpty() || code.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Department name and code are required");
            return;
        }
        generalRepo.updateDepartment(id, name, code);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "UPDATE_DEPARTMENT", "DEPARTMENT",
                id, "Updated department: " + name + " (" + code + ")", getClientIP(exchange));
        JsonUtil.sendJson(exchange, 200, Map.of("message", "Department updated successfully"));
    }

    private void handleCreateLeaveType(HttpExchange exchange, User admin) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String name = JsonUtil.getString(body, "name", "").trim();
        String code = JsonUtil.getString(body, "code", "").trim();
        String desc = JsonUtil.getString(body, "description", "").trim();
        boolean reqDoc = body.has("requires_document") && body.get("requires_document").getAsBoolean();

        if (name.isEmpty() || code.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Name and code are required");
            return;
        }
        generalRepo.createLeaveType(name, code, desc, reqDoc);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "CREATE_LEAVE_TYPE", "LEAVE_TYPE",
                null, "Created leave type: " + name + " (" + code + ")", getClientIP(exchange));
        JsonUtil.sendJson(exchange, 201, Map.of("message", "Leave type created successfully"));
    }

    private void handleUpdateLeaveType(HttpExchange exchange, User admin) throws Exception {
        int id = extractIdFromPath(exchange.getRequestURI().getPath());
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String name = JsonUtil.getString(body, "name", "").trim();
        String code = JsonUtil.getString(body, "code", "").trim();
        String desc = JsonUtil.getString(body, "description", "").trim();
        boolean reqDoc = body.has("requires_document") && body.get("requires_document").getAsBoolean();

        if (name.isEmpty() || code.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Name and code are required");
            return;
        }
        generalRepo.updateLeaveType(id, name, code, desc, reqDoc);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "UPDATE_LEAVE_TYPE", "LEAVE_TYPE",
                id, "Updated leave type: " + name + " (" + code + ")", getClientIP(exchange));
        JsonUtil.sendJson(exchange, 200, Map.of("message", "Leave type updated successfully"));
    }

    private void handleGetPolicies(HttpExchange exchange) throws Exception {
        List<Map<String, Object>> policies = generalRepo.findAllPolicies();
        JsonUtil.sendJson(exchange, 200, policies);
    }

    private void handleCreatePolicy(HttpExchange exchange, User admin) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);
        int leaveTypeId = JsonUtil.getInt(body, "leave_type_id", 0);
        String designation = JsonUtil.getString(body, "designation", "").trim();
        double days = JsonUtil.getDouble(body, "days_entitled", 0);
        boolean carry = body.has("carry_forward") && body.get("carry_forward").getAsBoolean();
        String year = JsonUtil.getString(body, "academic_year", DateUtil.currentAcademicYear()).trim();

        if (leaveTypeId <= 0 || designation.isEmpty() || days <= 0) {
            JsonUtil.sendError(exchange, 400, "Valid leave type, designation, and days entitled are required");
            return;
        }
        generalRepo.createPolicy(leaveTypeId, designation, days, carry, year);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "CREATE_POLICY", "POLICY",
                null, "Created leave policy for: " + designation + " (" + days + " days)", getClientIP(exchange));
        JsonUtil.sendJson(exchange, 201, Map.of("message", "Leave policy created successfully"));
    }

    private void handleUpdatePolicy(HttpExchange exchange, User admin) throws Exception {
        int id = extractIdFromPath(exchange.getRequestURI().getPath());
        JsonObject body = JsonUtil.readJsonBody(exchange);
        int leaveTypeId = JsonUtil.getInt(body, "leave_type_id", 0);
        String designation = JsonUtil.getString(body, "designation", "").trim();
        double days = JsonUtil.getDouble(body, "days_entitled", 0);
        boolean carry = body.has("carry_forward") && body.get("carry_forward").getAsBoolean();
        String year = JsonUtil.getString(body, "academic_year", DateUtil.currentAcademicYear()).trim();

        if (leaveTypeId <= 0 || designation.isEmpty() || days <= 0) {
            JsonUtil.sendError(exchange, 400, "Valid leave type, designation, and days entitled are required");
            return;
        }
        generalRepo.updatePolicy(id, leaveTypeId, designation, days, carry, year);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "UPDATE_POLICY", "POLICY",
                id, "Updated leave policy #" + id, getClientIP(exchange));
        JsonUtil.sendJson(exchange, 200, Map.of("message", "Leave policy updated successfully"));
    }

    private void handleCreateHoliday(HttpExchange exchange, User admin) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);
        String name = JsonUtil.getString(body, "name", "").trim();
        String date = JsonUtil.getString(body, "holiday_date", "").trim();
        String type = JsonUtil.getString(body, "holiday_type", "NATIONAL").trim();

        if (name.isEmpty() || date.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Holiday name and date are required");
            return;
        }
        generalRepo.createHoliday(name, date, type);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "CREATE_HOLIDAY", "HOLIDAY",
                null, "Created holiday: " + name + " (" + date + ")", getClientIP(exchange));
        JsonUtil.sendJson(exchange, 201, Map.of("message", "Holiday created successfully"));
    }

    private void handleDeleteHoliday(HttpExchange exchange, User admin) throws Exception {
        int id = extractIdFromPath(exchange.getRequestURI().getPath());
        generalRepo.deleteHoliday(id);
        generalRepo.addAuditLog(admin != null ? admin.getId() : null, "DELETE_HOLIDAY", "HOLIDAY",
                id, "Deleted holiday #" + id, getClientIP(exchange));
        JsonUtil.sendJson(exchange, 200, Map.of("message", "Holiday removed successfully"));
    }

    private void handleReports(HttpExchange exchange) throws Exception {
        Map<String, String> query = JsonUtil.parseQueryParams(exchange.getRequestURI());
        String type = query.getOrDefault("type", "summary");
        Integer deptId = null;
        if (query.containsKey("department_id") && !query.get("department_id").isEmpty()) {
            try { deptId = Integer.parseInt(query.get("department_id")); } catch (NumberFormatException ignored) {}
        }
        List<Map<String, Object>> report = generalRepo.generateReport(type, deptId);
        JsonUtil.sendJson(exchange, 200, report);
    }

    private void handleAuditLogs(HttpExchange exchange) throws Exception {
        Map<String, String> query = JsonUtil.parseQueryParams(exchange.getRequestURI());
        int limit = 50;
        if (query.containsKey("limit")) {
            try { limit = Integer.parseInt(query.get("limit")); } catch (NumberFormatException ignored) {}
        }
        List<Map<String, Object>> logs = generalRepo.getAuditLogs(limit);
        JsonUtil.sendJson(exchange, 200, logs);
    }

    private int extractIdFromPath(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String getClientIP(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null) return forwarded.split(",")[0].trim();
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }
}

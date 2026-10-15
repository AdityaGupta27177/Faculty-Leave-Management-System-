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
 * LeaveHandler — handles leave application CRUD, balances, calendar, and summaries.
 * Demonstrates: classes, objects, conditionals, loops, functions (Sem III topics).
 */
public class LeaveHandler implements HttpHandler {

    private final LeaveRepository leaveRepo = new LeaveRepository();
    private final GeneralRepository generalRepo = new GeneralRepository();
    private final UserRepository userRepo = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        User user = (User) exchange.getAttribute("user");

        try {
            // Route to appropriate handler method
            if (path.equals("/api/leave/apply") && "POST".equals(method)) {
                handleApply(exchange, user);
            } else if (path.equals("/api/leave/my") && "GET".equals(method)) {
                handleGetMyLeaves(exchange, user);
            } else if (path.equals("/api/leave/balances") && "GET".equals(method)) {
                handleGetBalances(exchange, user);
            } else if (path.equals("/api/leave/summary") && "GET".equals(method)) {
                handleGetSummary(exchange, user);
            } else if (path.equals("/api/leave/calendar") && "GET".equals(method)) {
                handleGetCalendar(exchange, user);
            } else if (path.matches("/api/leave/\\d+") && "GET".equals(method)) {
                handleGetById(exchange, user);
            } else if (path.matches("/api/leave/\\d+") && "PUT".equals(method)) {
                handleUpdate(exchange, user);
            } else if (path.matches("/api/leave/\\d+/cancel") && "POST".equals(method)) {
                handleCancel(exchange, user);
            } else {
                JsonUtil.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            System.err.println("[Leave] Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    /** Submit a new leave application */
    private void handleApply(HttpExchange exchange, User user) throws Exception {
        JsonObject body = JsonUtil.readJsonBody(exchange);

        // Server-side validation
        int leaveTypeId = JsonUtil.getInt(body, "leave_type_id", 0);
        String startDate = JsonUtil.getString(body, "start_date", "");
        String endDate = JsonUtil.getString(body, "end_date", "");
        String reason = JsonUtil.getString(body, "reason", "");
        String status = JsonUtil.getString(body, "status", "DRAFT");
        boolean halfDayStart = JsonUtil.getBoolean(body, "half_day_start", false);
        boolean halfDayEnd = JsonUtil.getBoolean(body, "half_day_end", false);
        double totalDays = JsonUtil.getDouble(body, "total_days", 0);

        // Validate
        if (leaveTypeId <= 0) { JsonUtil.sendError(exchange, 400, "Leave type is required."); return; }
        if (!DateUtil.isValidDate(startDate)) { JsonUtil.sendError(exchange, 400, "Invalid start date."); return; }
        if (!DateUtil.isValidDate(endDate)) { JsonUtil.sendError(exchange, 400, "Invalid end date."); return; }

        if ("SUBMITTED".equals(status)) {
            if (reason.trim().length() < 10) {
                JsonUtil.sendError(exchange, 400, "Reason must be at least 10 characters.");
                return;
            }
            if (totalDays <= 0) {
                JsonUtil.sendError(exchange, 400, "Leave duration must be positive.");
                return;
            }
        }

        // Recalculate total days server-side
        Set<String> holidays = generalRepo.getHolidayDates();
        double serverDays = DateUtil.calculateWorkingDays(startDate, endDate, halfDayStart, halfDayEnd, holidays);
        if (serverDays <= 0 && "SUBMITTED".equals(status)) {
            JsonUtil.sendError(exchange, 400, "No working days in the selected range.");
            return;
        }

        // Build application
        LeaveApplication app = new LeaveApplication();
        app.setApplicationNo(leaveRepo.generateApplicationNo());
        app.setUserId(user.getId());
        app.setLeaveTypeId(leaveTypeId);
        app.setStartDate(startDate);
        app.setEndDate(endDate);
        app.setHalfDayStart(halfDayStart);
        app.setHalfDayEnd(halfDayEnd);
        app.setTotalDays(serverDays);
        app.setReason(reason);
        app.setStatus(status);

        // Set approver from user's configured approver
        if (user.getApproverId() > 0) {
            app.setApproverId(user.getApproverId());
        }

        // If submitting, set status to SUBMITTED and mark pending
        if ("SUBMITTED".equals(status)) {
            app.setStatus("SUBMITTED");
            app.setSubmittedAt(DateUtil.now());
        }

        app = leaveRepo.create(app);

        // If submitted, add to pending balance and create approval history
        if ("SUBMITTED".equals(app.getStatus())) {
            generalRepo.addPending(user.getId(), leaveTypeId, DateUtil.currentAcademicYear(), serverDays);
            leaveRepo.addApprovalHistory(app.getId(), "SUBMITTED", user.getId(), "Application submitted.");

            // Notify approver
            if (user.getApproverId() > 0) {
                generalRepo.createNotification(user.getApproverId(),
                        "New Leave Application",
                        user.getName() + " has submitted a leave application (" + app.getApplicationNo() + ").",
                        "LEAVE_SUBMITTED", app.getId());
            }

            // Audit
            generalRepo.addAuditLog(user.getId(), "LEAVE_SUBMITTED", "LEAVE_APPLICATION",
                    app.getId(), "Submitted " + serverDays + " days of leave", null);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", app.getId());
        response.put("application_no", app.getApplicationNo());
        response.put("status", app.getStatus());
        JsonUtil.sendJson(exchange, 201, response);
    }

    /** Get current user's leave applications */
    private void handleGetMyLeaves(HttpExchange exchange, User user) throws Exception {
        Map<String, String> params = JsonUtil.getQueryParams(exchange);
        int limit = 0;
        try { limit = Integer.parseInt(params.getOrDefault("limit", "0")); } catch (NumberFormatException e) {}

        List<LeaveApplication> apps = leaveRepo.findByUserId(user.getId(), limit);
        JsonUtil.sendJson(exchange, 200, apps);
    }

    /** Get leave balances for current user */
    private void handleGetBalances(HttpExchange exchange, User user) throws Exception {
        // Initialize balances if user has a designation
        if (user.getDesignation() != null && !user.getDesignation().isEmpty()) {
            generalRepo.initializeBalances(user.getId(), user.getDesignation(), DateUtil.currentAcademicYear());
        }

        List<LeaveBalance> balances = generalRepo.getBalancesForUser(user.getId(), DateUtil.currentAcademicYear());
        JsonUtil.sendJson(exchange, 200, balances);
    }

    /** Get application status summary */
    private void handleGetSummary(HttpExchange exchange, User user) throws Exception {
        Map<String, Integer> counts = leaveRepo.countByStatusForUser(user.getId());

        Map<String, Object> summary = new HashMap<>();
        int total = 0;
        for (int v : counts.values()) total += v;
        summary.put("total", total);
        summary.put("pending", counts.getOrDefault("SUBMITTED", 0) + counts.getOrDefault("PENDING", 0));
        summary.put("approved", counts.getOrDefault("APPROVED", 0));
        summary.put("rejected", counts.getOrDefault("REJECTED", 0));
        summary.put("draft", counts.getOrDefault("DRAFT", 0));

        JsonUtil.sendJson(exchange, 200, summary);
    }

    /** Get leave calendar */
    private void handleGetCalendar(HttpExchange exchange, User user) throws Exception {
        Map<String, String> params = JsonUtil.getQueryParams(exchange);
        Integer deptId = null;
        if ("true".equals(params.get("dept")) && user.getDepartmentId() > 0) {
            deptId = user.getDepartmentId();
        }
        List<LeaveApplication> calendar = leaveRepo.findCalendar(deptId);
        JsonUtil.sendJson(exchange, 200, calendar);
    }

    /** Get a single application by ID */
    private void handleGetById(HttpExchange exchange, User user) throws Exception {
        int id = Integer.parseInt(JsonUtil.getPathSegment(exchange, 3));
        LeaveApplication app = leaveRepo.findById(id);

        if (app == null) {
            JsonUtil.sendError(exchange, 404, "Application not found.");
            return;
        }

        // Authorization: user can view own apps, approver can view assigned apps, admin can view all
        if (app.getUserId() != user.getId() && app.getApproverId() != user.getId() && !user.hasRole("ADMIN")) {
            JsonUtil.sendError(exchange, 403, "Not authorized to view this application.");
            return;
        }

        // Attach approval history
        List<Map<String, Object>> history = leaveRepo.getApprovalHistory(id);

        Map<String, Object> response = new HashMap<>();
        response.put("id", app.getId());
        response.put("application_no", app.getApplicationNo());
        response.put("user_id", app.getUserId());
        response.put("faculty_name", app.getFacultyName());
        response.put("department_name", app.getDepartmentName());
        response.put("leave_type_id", app.getLeaveTypeId());
        response.put("leave_type_name", app.getLeaveTypeName());
        response.put("start_date", app.getStartDate());
        response.put("end_date", app.getEndDate());
        response.put("half_day_start", app.isHalfDayStart());
        response.put("half_day_end", app.isHalfDayEnd());
        response.put("total_days", app.getTotalDays());
        response.put("reason", app.getReason());
        response.put("status", app.getStatus());
        response.put("approver_name", app.getApproverName());
        response.put("submitted_at", app.getSubmittedAt());
        response.put("decided_at", app.getDecidedAt());
        response.put("approval_history", history);

        JsonUtil.sendJson(exchange, 200, response);
    }

    /** Update a draft/returned application */
    private void handleUpdate(HttpExchange exchange, User user) throws Exception {
        int id = Integer.parseInt(JsonUtil.getPathSegment(exchange, 3));
        LeaveApplication existing = leaveRepo.findById(id);

        if (existing == null) { JsonUtil.sendError(exchange, 404, "Not found."); return; }
        if (existing.getUserId() != user.getId()) { JsonUtil.sendError(exchange, 403, "Not authorized."); return; }
        if (!existing.isEditable()) { JsonUtil.sendError(exchange, 400, "Application cannot be edited."); return; }

        JsonObject body = JsonUtil.readJsonBody(exchange);
        String status = JsonUtil.getString(body, "status", existing.getStatus());

        // Update fields
        existing.setLeaveTypeId(JsonUtil.getInt(body, "leave_type_id", existing.getLeaveTypeId()));
        existing.setStartDate(JsonUtil.getString(body, "start_date", existing.getStartDate()));
        existing.setEndDate(JsonUtil.getString(body, "end_date", existing.getEndDate()));
        existing.setHalfDayStart(JsonUtil.getBoolean(body, "half_day_start", existing.isHalfDayStart()));
        existing.setHalfDayEnd(JsonUtil.getBoolean(body, "half_day_end", existing.isHalfDayEnd()));
        existing.setReason(JsonUtil.getString(body, "reason", existing.getReason()));

        // Recalculate days
        Set<String> holidays = generalRepo.getHolidayDates();
        double days = DateUtil.calculateWorkingDays(existing.getStartDate(), existing.getEndDate(),
                existing.isHalfDayStart(), existing.isHalfDayEnd(), holidays);
        existing.setTotalDays(days);
        existing.setStatus(status);

        if ("SUBMITTED".equals(status)) {
            existing.setSubmittedAt(DateUtil.now());
            if (user.getApproverId() > 0) existing.setApproverId(user.getApproverId());
        }

        leaveRepo.update(existing);

        if ("SUBMITTED".equals(status)) {
            generalRepo.addPending(user.getId(), existing.getLeaveTypeId(), DateUtil.currentAcademicYear(), days);
            leaveRepo.addApprovalHistory(id, "SUBMITTED", user.getId(), "Application submitted.");

            if (user.getApproverId() > 0) {
                generalRepo.createNotification(user.getApproverId(),
                        "Leave Application Resubmitted",
                        user.getName() + " has resubmitted application " + existing.getApplicationNo(),
                        "LEAVE_SUBMITTED", id);
            }
        }

        JsonUtil.sendJson(exchange, 200, Map.of("id", id, "status", status));
    }

    /** Cancel an application */
    private void handleCancel(HttpExchange exchange, User user) throws Exception {
        int id = Integer.parseInt(JsonUtil.getPathSegment(exchange, 3));
        LeaveApplication app = leaveRepo.findById(id);

        if (app == null) { JsonUtil.sendError(exchange, 404, "Not found."); return; }
        if (app.getUserId() != user.getId()) { JsonUtil.sendError(exchange, 403, "Not authorized."); return; }
        if (!app.isCancellable()) { JsonUtil.sendError(exchange, 400, "Application cannot be cancelled."); return; }

        String previousStatus = app.getStatus();
        app.setStatus("CANCELLED");
        app.setDecidedAt(DateUtil.now());
        leaveRepo.update(app);

        // Restore pending balance
        generalRepo.restorePending(user.getId(), app.getLeaveTypeId(), DateUtil.currentAcademicYear(), app.getTotalDays());

        leaveRepo.addApprovalHistory(id, "CANCELLED", user.getId(), "Cancelled by applicant.");

        generalRepo.addAuditLog(user.getId(), "LEAVE_CANCELLED", "LEAVE_APPLICATION", id,
                "Application " + app.getApplicationNo() + " cancelled", null);

        JsonUtil.sendJson(exchange, 200, Map.of("status", "cancelled"));
    }
}

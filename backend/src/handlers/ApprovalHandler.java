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
 * ApprovalHandler — HOD approval/rejection of leave applications.
 * Enforces that only the assigned approver can decide on an application.
 */
public class ApprovalHandler implements HttpHandler {

    private final LeaveRepository leaveRepo = new LeaveRepository();
    private final GeneralRepository generalRepo = new GeneralRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        User user = (User) exchange.getAttribute("user");

        try {
            if (path.equals("/api/approval/pending") && "GET".equals(method)) {
                handleGetPending(exchange, user);
            } else if (path.equals("/api/approval/history") && "GET".equals(method)) {
                handleGetHistory(exchange, user);
            } else if (path.equals("/api/approval/stats") && "GET".equals(method)) {
                handleGetStats(exchange, user);
            } else if (path.equals("/api/approval/overlaps") && "GET".equals(method)) {
                handleGetOverlaps(exchange, user);
            } else if (path.matches("/api/approval/\\d+/decide") && "POST".equals(method)) {
                handleDecide(exchange, user);
            } else {
                JsonUtil.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            System.err.println("[Approval] Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    /** Get pending applications for this approver */
    private void handleGetPending(HttpExchange exchange, User user) throws Exception {
        List<LeaveApplication> apps = leaveRepo.findPendingForApprover(user.getId());
        JsonUtil.sendJson(exchange, 200, apps);
    }

    /** Get decided applications for this approver */
    private void handleGetHistory(HttpExchange exchange, User user) throws Exception {
        List<LeaveApplication> apps = leaveRepo.findDecidedByApprover(user.getId());
        JsonUtil.sendJson(exchange, 200, apps);
    }

    /** Get approval statistics */
    private void handleGetStats(HttpExchange exchange, User user) throws Exception {
        Map<String, Integer> counts = leaveRepo.countByStatusForApprover(user.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", counts.getOrDefault("SUBMITTED", 0) + counts.getOrDefault("PENDING", 0));
        stats.put("approved", counts.getOrDefault("APPROVED", 0));
        stats.put("rejected", counts.getOrDefault("REJECTED", 0));

        // On leave today
        int onLeave = 0;
        if (user.getDepartmentId() > 0) {
            onLeave = leaveRepo.countOnLeaveToday(user.getDepartmentId());
        }
        stats.put("on_leave_today", onLeave);

        JsonUtil.sendJson(exchange, 200, stats);
    }

    /** Get overlapping leave in the department */
    private void handleGetOverlaps(HttpExchange exchange, User user) throws Exception {
        Map<String, String> params = JsonUtil.getQueryParams(exchange);
        String startDate = params.getOrDefault("start_date", "");
        String endDate = params.getOrDefault("end_date", "");
        int excludeId = 0;
        try { excludeId = Integer.parseInt(params.getOrDefault("exclude_id", "0")); } catch (Exception e) {}

        if (user.getDepartmentId() <= 0 || startDate.isEmpty() || endDate.isEmpty()) {
            JsonUtil.sendJson(exchange, 200, Collections.emptyList());
            return;
        }

        List<LeaveApplication> overlaps = leaveRepo.findOverlaps(user.getDepartmentId(), startDate, endDate, excludeId);
        JsonUtil.sendJson(exchange, 200, overlaps);
    }

    /** Approve, reject, or return an application */
    private void handleDecide(HttpExchange exchange, User user) throws Exception {
        String pathStr = exchange.getRequestURI().getPath();
        // Extract ID from /api/approval/123/decide
        String[] parts = pathStr.split("/");
        int appId = Integer.parseInt(parts[3]);

        LeaveApplication app = leaveRepo.findById(appId);
        if (app == null) {
            JsonUtil.sendError(exchange, 404, "Application not found.");
            return;
        }

        // Authorization: only the assigned approver or admin can decide
        if (app.getApproverId() != user.getId() && !user.hasRole("ADMIN")) {
            JsonUtil.sendError(exchange, 403, "You are not the assigned approver for this application.");
            return;
        }

        // Check current status — can only decide on SUBMITTED or PENDING
        if (!"SUBMITTED".equals(app.getStatus()) && !"PENDING".equals(app.getStatus())) {
            JsonUtil.sendError(exchange, 400, "Application is not in a reviewable state (current: " + app.getStatus() + ").");
            return;
        }

        JsonObject body = JsonUtil.readJsonBody(exchange);
        String action = JsonUtil.getString(body, "action", "");
        String comments = JsonUtil.getString(body, "comments", "");

        // Validate action
        if (!action.equals("APPROVED") && !action.equals("REJECTED") && !action.equals("RETURNED")) {
            JsonUtil.sendError(exchange, 400, "Invalid action. Must be APPROVED, REJECTED, or RETURNED.");
            return;
        }

        // Require comments for rejection
        if ("REJECTED".equals(action) && (comments == null || comments.trim().length() < 5)) {
            JsonUtil.sendError(exchange, 400, "Please provide a reason for rejection.");
            return;
        }

        // Update application status
        app.setStatus(action);
        app.setDecidedAt(DateUtil.now());
        leaveRepo.update(app);

        // Record approval history
        leaveRepo.addApprovalHistory(appId, action, user.getId(), comments);

        // Update leave balances
        String academicYear = DateUtil.currentAcademicYear();
        switch (action) {
            case "APPROVED":
                // Move from pending to used
                generalRepo.approvePending(app.getUserId(), app.getLeaveTypeId(), academicYear, app.getTotalDays());
                break;
            case "REJECTED":
            case "RETURNED":
                // Restore pending
                generalRepo.restorePending(app.getUserId(), app.getLeaveTypeId(), academicYear, app.getTotalDays());
                break;
        }

        // Notify the applicant
        String notifType = "LEAVE_" + action;
        String notifTitle = action.equals("APPROVED") ? "Leave Approved" :
                            action.equals("REJECTED") ? "Leave Rejected" : "Changes Requested";
        String notifMsg = "Your leave application " + app.getApplicationNo() + " has been " +
                         action.toLowerCase() + " by " + user.getName() + ".";
        if (comments != null && !comments.isEmpty()) {
            notifMsg += " Comment: " + comments;
        }
        generalRepo.createNotification(app.getUserId(), notifTitle, notifMsg, notifType, appId);

        // Audit log
        generalRepo.addAuditLog(user.getId(), "LEAVE_" + action, "LEAVE_APPLICATION", appId,
                "Application " + app.getApplicationNo() + " " + action.toLowerCase() +
                (comments.isEmpty() ? "" : ". Comment: " + comments), null);

        JsonUtil.sendJson(exchange, 200, Map.of("status", action.toLowerCase(), "application_id", appId));
    }
}

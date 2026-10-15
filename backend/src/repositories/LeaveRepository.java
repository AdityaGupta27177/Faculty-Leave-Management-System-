package repositories;

import models.LeaveApplication;
import java.sql.*;
import java.util.*;

/**
 * LeaveRepository — database operations for leave applications.
 * All queries use prepared statements (parameterized) to prevent SQL injection.
 */
public class LeaveRepository {

    /** Generate a unique application number */
    public String generateApplicationNo() throws SQLException {
        String sql = "SELECT nextval('app_number_seq')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "LV-" + rs.getLong(1);
            }
        }
        return "LV-" + System.currentTimeMillis();
    }

    /** Create a new leave application */
    public LeaveApplication create(LeaveApplication app) throws SQLException {
        String sql = "INSERT INTO leave_applications (application_no, user_id, leave_type_id, " +
                     "start_date, end_date, half_day_start, half_day_end, total_days, reason, " +
                     "supporting_doc, status, approver_id, submitted_at) " +
                     "VALUES (?, ?, ?, ?::date, ?::date, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "RETURNING id, created_at";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app.getApplicationNo());
            ps.setInt(2, app.getUserId());
            ps.setInt(3, app.getLeaveTypeId());
            ps.setString(4, app.getStartDate());
            ps.setString(5, app.getEndDate());
            ps.setBoolean(6, app.isHalfDayStart());
            ps.setBoolean(7, app.isHalfDayEnd());
            ps.setDouble(8, app.getTotalDays());
            ps.setString(9, app.getReason());
            ps.setString(10, app.getSupportingDoc());
            ps.setString(11, app.getStatus());
            if (app.getApproverId() > 0) ps.setInt(12, app.getApproverId());
            else ps.setNull(12, Types.INTEGER);
            if ("SUBMITTED".equals(app.getStatus()) || "PENDING".equals(app.getStatus())) {
                ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                app.setId(rs.getInt("id"));
                app.setCreatedAt(rs.getString("created_at"));
            }
        }
        return app;
    }

    /** Update a leave application */
    public void update(LeaveApplication app) throws SQLException {
        String sql = "UPDATE leave_applications SET leave_type_id = ?, start_date = ?::date, " +
                     "end_date = ?::date, half_day_start = ?, half_day_end = ?, total_days = ?, " +
                     "reason = ?, supporting_doc = ?, status = ?, approver_id = ?, " +
                     "submitted_at = ?, decided_at = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, app.getLeaveTypeId());
            ps.setString(2, app.getStartDate());
            ps.setString(3, app.getEndDate());
            ps.setBoolean(4, app.isHalfDayStart());
            ps.setBoolean(5, app.isHalfDayEnd());
            ps.setDouble(6, app.getTotalDays());
            ps.setString(7, app.getReason());
            ps.setString(8, app.getSupportingDoc());
            ps.setString(9, app.getStatus());
            if (app.getApproverId() > 0) ps.setInt(10, app.getApproverId());
            else ps.setNull(10, Types.INTEGER);
            if (app.getSubmittedAt() != null) ps.setString(11, app.getSubmittedAt());
            else ps.setNull(11, Types.TIMESTAMP);
            if (app.getDecidedAt() != null) ps.setString(12, app.getDecidedAt());
            else ps.setNull(12, Types.TIMESTAMP);
            ps.setInt(13, app.getId());
            ps.executeUpdate();
        }
    }

    /** Find application by ID with joined info */
    public LeaveApplication findById(int id) throws SQLException {
        String sql = "SELECT la.*, lt.name AS leave_type_name, u.name AS faculty_name, " +
                     "d.name AS department_name, ap.name AS approver_name " +
                     "FROM leave_applications la " +
                     "JOIN leave_types lt ON la.leave_type_id = lt.id " +
                     "JOIN users u ON la.user_id = u.id " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "LEFT JOIN users ap ON la.approver_id = ap.id " +
                     "WHERE la.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapApplication(rs);
            return null;
        }
    }

    /** Get applications for a specific user */
    public List<LeaveApplication> findByUserId(int userId, int limit) throws SQLException {
        String sql = "SELECT la.*, lt.name AS leave_type_name, u.name AS faculty_name, " +
                     "d.name AS department_name, ap.name AS approver_name " +
                     "FROM leave_applications la " +
                     "JOIN leave_types lt ON la.leave_type_id = lt.id " +
                     "JOIN users u ON la.user_id = u.id " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "LEFT JOIN users ap ON la.approver_id = ap.id " +
                     "WHERE la.user_id = ? ORDER BY la.created_at DESC";
        if (limit > 0) sql += " LIMIT " + limit;

        List<LeaveApplication> apps = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) apps.add(mapApplication(rs));
        }
        return apps;
    }

    /** Get applications pending approval for a specific approver */
    public List<LeaveApplication> findPendingForApprover(int approverId) throws SQLException {
        String sql = "SELECT la.*, lt.name AS leave_type_name, u.name AS faculty_name, " +
                     "d.name AS department_name, ap.name AS approver_name " +
                     "FROM leave_applications la " +
                     "JOIN leave_types lt ON la.leave_type_id = lt.id " +
                     "JOIN users u ON la.user_id = u.id " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "LEFT JOIN users ap ON la.approver_id = ap.id " +
                     "WHERE la.approver_id = ? AND la.status IN ('SUBMITTED', 'PENDING') " +
                     "ORDER BY la.submitted_at ASC";
        List<LeaveApplication> apps = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, approverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) apps.add(mapApplication(rs));
        }
        return apps;
    }

    /** Get decided applications for a specific approver */
    public List<LeaveApplication> findDecidedByApprover(int approverId) throws SQLException {
        String sql = "SELECT la.*, lt.name AS leave_type_name, u.name AS faculty_name, " +
                     "d.name AS department_name, ap.name AS approver_name, " +
                     "(SELECT ah.comments FROM approval_history ah WHERE ah.application_id = la.id " +
                     " ORDER BY ah.created_at DESC LIMIT 1) AS approval_comments " +
                     "FROM leave_applications la " +
                     "JOIN leave_types lt ON la.leave_type_id = lt.id " +
                     "JOIN users u ON la.user_id = u.id " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "LEFT JOIN users ap ON la.approver_id = ap.id " +
                     "WHERE la.approver_id = ? AND la.status IN ('APPROVED', 'REJECTED', 'RETURNED') " +
                     "ORDER BY la.decided_at DESC";
        List<LeaveApplication> apps = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, approverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) apps.add(mapApplication(rs));
        }
        return apps;
    }

    /** Get all applications (for admin) */
    public List<LeaveApplication> findAll(int limit) throws SQLException {
        String sql = "SELECT la.*, lt.name AS leave_type_name, u.name AS faculty_name, " +
                     "d.name AS department_name " +
                     "FROM leave_applications la " +
                     "JOIN leave_types lt ON la.leave_type_id = lt.id " +
                     "JOIN users u ON la.user_id = u.id " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "ORDER BY la.created_at DESC";
        if (limit > 0) sql += " LIMIT " + limit;

        List<LeaveApplication> apps = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) apps.add(mapApplication(rs));
        }
        return apps;
    }

    /** Get leave calendar (approved/pending applications for the department) */
    public List<LeaveApplication> findCalendar(Integer departmentId) throws SQLException {
        String sql = "SELECT la.*, lt.name AS leave_type_name, u.name AS faculty_name, " +
                     "d.name AS department_name " +
                     "FROM leave_applications la " +
                     "JOIN leave_types lt ON la.leave_type_id = lt.id " +
                     "JOIN users u ON la.user_id = u.id " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE la.status IN ('APPROVED', 'PENDING', 'SUBMITTED') " +
                     "AND la.end_date >= CURRENT_DATE ";
        if (departmentId != null && departmentId > 0) {
            sql += "AND u.department_id = ? ";
        }
        sql += "ORDER BY la.start_date ASC";

        List<LeaveApplication> apps = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (departmentId != null && departmentId > 0) {
                ps.setInt(1, departmentId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) apps.add(mapApplication(rs));
        }
        return apps;
    }

    /** Find overlapping leave applications for a department */
    public List<LeaveApplication> findOverlaps(int departmentId, String startDate, String endDate, int excludeId) throws SQLException {
        String sql = "SELECT la.*, lt.name AS leave_type_name, u.name AS faculty_name " +
                     "FROM leave_applications la " +
                     "JOIN users u ON la.user_id = u.id " +
                     "JOIN leave_types lt ON la.leave_type_id = lt.id " +
                     "WHERE u.department_id = ? " +
                     "AND la.status IN ('APPROVED', 'PENDING', 'SUBMITTED') " +
                     "AND la.start_date <= ?::date AND la.end_date >= ?::date " +
                     "AND la.id != ?";
        List<LeaveApplication> apps = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            ps.setString(2, endDate);
            ps.setString(3, startDate);
            ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) apps.add(mapApplication(rs));
        }
        return apps;
    }

    /** Count applications by status for a user */
    public Map<String, Integer> countByStatusForUser(int userId) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS cnt FROM leave_applications WHERE user_id = ? GROUP BY status";
        Map<String, Integer> counts = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                counts.put(rs.getString("status"), rs.getInt("cnt"));
            }
        }
        return counts;
    }

    /** Count applications by status for an approver */
    public Map<String, Integer> countByStatusForApprover(int approverId) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS cnt FROM leave_applications WHERE approver_id = ? GROUP BY status";
        Map<String, Integer> counts = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, approverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                counts.put(rs.getString("status"), rs.getInt("cnt"));
            }
        }
        return counts;
    }

    /** Count people on leave today for a department */
    public int countOnLeaveToday(int departmentId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT la.user_id) FROM leave_applications la " +
                     "JOIN users u ON la.user_id = u.id " +
                     "WHERE u.department_id = ? AND la.status = 'APPROVED' " +
                     "AND CURRENT_DATE BETWEEN la.start_date AND la.end_date";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    /** Add an approval history record */
    public void addApprovalHistory(int applicationId, String action, int actorId, String comments) throws SQLException {
        String sql = "INSERT INTO approval_history (application_id, action, actor_id, comments) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setString(2, action);
            ps.setInt(3, actorId);
            ps.setString(4, comments);
            ps.executeUpdate();
        }
    }

    /** Get approval history for an application */
    public List<Map<String, Object>> getApprovalHistory(int applicationId) throws SQLException {
        String sql = "SELECT ah.*, u.name AS actor_name FROM approval_history ah " +
                     "JOIN users u ON ah.actor_id = u.id " +
                     "WHERE ah.application_id = ? ORDER BY ah.created_at ASC";
        List<Map<String, Object>> history = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> h = new HashMap<>();
                h.put("id", rs.getInt("id"));
                h.put("action", rs.getString("action"));
                h.put("actor_name", rs.getString("actor_name"));
                h.put("comments", rs.getString("comments"));
                h.put("created_at", rs.getString("created_at"));
                history.add(h);
            }
        }
        return history;
    }

    /** Map a ResultSet row to a LeaveApplication */
    private LeaveApplication mapApplication(ResultSet rs) throws SQLException {
        LeaveApplication a = new LeaveApplication();
        a.setId(rs.getInt("id"));
        a.setApplicationNo(rs.getString("application_no"));
        a.setUserId(rs.getInt("user_id"));
        a.setLeaveTypeId(rs.getInt("leave_type_id"));
        a.setStartDate(rs.getString("start_date"));
        a.setEndDate(rs.getString("end_date"));
        a.setHalfDayStart(rs.getBoolean("half_day_start"));
        a.setHalfDayEnd(rs.getBoolean("half_day_end"));
        a.setTotalDays(rs.getDouble("total_days"));
        a.setReason(rs.getString("reason"));
        a.setSupportingDoc(rs.getString("supporting_doc"));
        a.setStatus(rs.getString("status"));
        a.setApproverId(rs.getInt("approver_id"));
        a.setSubmittedAt(rs.getString("submitted_at"));
        a.setDecidedAt(rs.getString("decided_at"));
        a.setCreatedAt(rs.getString("created_at"));
        a.setUpdatedAt(rs.getString("updated_at"));
        // Joined fields
        try { a.setFacultyName(rs.getString("faculty_name")); } catch (SQLException ignored) {}
        try { a.setLeaveTypeName(rs.getString("leave_type_name")); } catch (SQLException ignored) {}
        try { a.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        try { a.setApproverName(rs.getString("approver_name")); } catch (SQLException ignored) {}
        try { a.setApprovalComments(rs.getString("approval_comments")); } catch (SQLException ignored) {}
        return a;
    }
}

package repositories;

import models.*;
import java.sql.*;
import java.util.*;

/**
 * GeneralRepository — handles departments, leave types, policies, holidays,
 * balances, notifications, and audit logs.
 */
public class GeneralRepository {

    // ========== Departments ==========
    public List<Department> findAllDepartments() throws SQLException {
        String sql = "SELECT * FROM departments WHERE is_active = true ORDER BY name";
        List<Department> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Department d = new Department();
                d.setId(rs.getInt("id"));
                d.setName(rs.getString("name"));
                d.setCode(rs.getString("code"));
                d.setActive(rs.getBoolean("is_active"));
                list.add(d);
            }
        }
        return list;
    }

    public void createDepartment(String name, String code) throws SQLException {
        String sql = "INSERT INTO departments (name, code) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, code);
            ps.executeUpdate();
        }
    }

    public void updateDepartment(int id, String name, String code) throws SQLException {
        String sql = "UPDATE departments SET name = ?, code = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, code);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    // ========== Leave Types ==========
    public List<LeaveType> findAllLeaveTypes() throws SQLException {
        String sql = "SELECT * FROM leave_types WHERE is_active = true ORDER BY name";
        List<LeaveType> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LeaveType lt = new LeaveType();
                lt.setId(rs.getInt("id"));
                lt.setName(rs.getString("name"));
                lt.setCode(rs.getString("code"));
                lt.setDescription(rs.getString("description"));
                lt.setRequiresDocument(rs.getBoolean("requires_document"));
                lt.setActive(rs.getBoolean("is_active"));
                list.add(lt);
            }
        }
        return list;
    }

    public void createLeaveType(String name, String code, String desc, boolean reqDoc) throws SQLException {
        String sql = "INSERT INTO leave_types (name, code, description, requires_document) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, code);
            ps.setString(3, desc); ps.setBoolean(4, reqDoc);
            ps.executeUpdate();
        }
    }

    public void updateLeaveType(int id, String name, String code, String desc, boolean reqDoc) throws SQLException {
        String sql = "UPDATE leave_types SET name=?, code=?, description=?, requires_document=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, code);
            ps.setString(3, desc); ps.setBoolean(4, reqDoc);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    // ========== Leave Policies ==========
    public List<Map<String, Object>> findAllPolicies() throws SQLException {
        String sql = "SELECT lp.*, lt.name AS leave_type_name FROM leave_policies lp " +
                     "JOIN leave_types lt ON lp.leave_type_id = lt.id ORDER BY lt.name, lp.designation";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("id", rs.getInt("id"));
                p.put("leave_type_id", rs.getInt("leave_type_id"));
                p.put("leave_type_name", rs.getString("leave_type_name"));
                p.put("designation", rs.getString("designation"));
                p.put("days_entitled", rs.getDouble("days_entitled"));
                p.put("carry_forward", rs.getBoolean("carry_forward"));
                p.put("academic_year", rs.getString("academic_year"));
                list.add(p);
            }
        }
        return list;
    }

    public void createPolicy(int leaveTypeId, String designation, double days, boolean carry, String year) throws SQLException {
        String sql = "INSERT INTO leave_policies (leave_type_id, designation, days_entitled, carry_forward, academic_year) " +
                     "VALUES (?, ?, ?, ?, ?) ON CONFLICT (leave_type_id, designation, academic_year) DO UPDATE SET days_entitled = ?, carry_forward = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveTypeId); ps.setString(2, designation);
            ps.setDouble(3, days); ps.setBoolean(4, carry); ps.setString(5, year);
            ps.setDouble(6, days); ps.setBoolean(7, carry);
            ps.executeUpdate();
        }
    }

    public void updatePolicy(int id, int leaveTypeId, String designation, double days, boolean carry, String year) throws SQLException {
        String sql = "UPDATE leave_policies SET leave_type_id=?, designation=?, days_entitled=?, carry_forward=?, academic_year=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveTypeId); ps.setString(2, designation);
            ps.setDouble(3, days); ps.setBoolean(4, carry);
            ps.setString(5, year); ps.setInt(6, id);
            ps.executeUpdate();
        }
    }

    // ========== Holidays ==========
    public List<Map<String, Object>> findAllHolidays() throws SQLException {
        String sql = "SELECT * FROM holidays WHERE is_active = true ORDER BY holiday_date";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> h = new HashMap<>();
                h.put("id", rs.getInt("id"));
                h.put("name", rs.getString("name"));
                h.put("holiday_date", rs.getString("holiday_date"));
                h.put("holiday_type", rs.getString("holiday_type"));
                list.add(h);
            }
        }
        return list;
    }

    public Set<String> getHolidayDates() throws SQLException {
        Set<String> dates = new HashSet<>();
        String sql = "SELECT holiday_date FROM holidays WHERE is_active = true";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) dates.add(rs.getString("holiday_date"));
        }
        return dates;
    }

    public void createHoliday(String name, String date, String type) throws SQLException {
        String sql = "INSERT INTO holidays (name, holiday_date, holiday_type) VALUES (?, ?::date, ?) ON CONFLICT (holiday_date, name) DO NOTHING";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, date); ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    public void deleteHoliday(int id) throws SQLException {
        String sql = "UPDATE holidays SET is_active = false WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ========== Leave Balances ==========
    public List<LeaveBalance> getBalancesForUser(int userId, String academicYear) throws SQLException {
        String sql = "SELECT lb.*, lt.name AS leave_type_name, lt.code FROM leave_balances lb " +
                     "JOIN leave_types lt ON lb.leave_type_id = lt.id " +
                     "WHERE lb.user_id = ? AND lb.academic_year = ? ORDER BY lt.name";
        List<LeaveBalance> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, academicYear);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LeaveBalance b = new LeaveBalance();
                b.setId(rs.getInt("id"));
                b.setUserId(rs.getInt("user_id"));
                b.setLeaveTypeId(rs.getInt("leave_type_id"));
                b.setLeaveTypeName(rs.getString("leave_type_name"));
                b.setCode(rs.getString("code"));
                b.setAcademicYear(rs.getString("academic_year"));
                b.setTotalEntitled(rs.getDouble("total_entitled"));
                b.setUsed(rs.getDouble("used"));
                b.setPending(rs.getDouble("pending"));
                list.add(b);
            }
        }
        return list;
    }

    /** Initialize balances for a user based on their designation and leave policies */
    public void initializeBalances(int userId, String designation, String academicYear) throws SQLException {
        String sql = "INSERT INTO leave_balances (user_id, leave_type_id, academic_year, total_entitled) " +
                     "SELECT ?, lp.leave_type_id, ?, lp.days_entitled " +
                     "FROM leave_policies lp WHERE lp.designation = ? AND lp.academic_year = ? " +
                     "ON CONFLICT (user_id, leave_type_id, academic_year) DO NOTHING";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, academicYear);
            ps.setString(3, designation); ps.setString(4, academicYear);
            ps.executeUpdate();
        }
    }

    /** Update balance: add to pending */
    public void addPending(int userId, int leaveTypeId, String academicYear, double days) throws SQLException {
        String sql = "UPDATE leave_balances SET pending = pending + ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE user_id = ? AND leave_type_id = ? AND academic_year = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, days); ps.setInt(2, userId);
            ps.setInt(3, leaveTypeId); ps.setString(4, academicYear);
            ps.executeUpdate();
        }
    }

    /** Move from pending to used (on approval) */
    public void approvePending(int userId, int leaveTypeId, String academicYear, double days) throws SQLException {
        String sql = "UPDATE leave_balances SET pending = pending - ?, used = used + ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE user_id = ? AND leave_type_id = ? AND academic_year = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, days); ps.setDouble(2, days);
            ps.setInt(3, userId); ps.setInt(4, leaveTypeId); ps.setString(5, academicYear);
            ps.executeUpdate();
        }
    }

    /** Restore pending (on rejection/cancellation) */
    public void restorePending(int userId, int leaveTypeId, String academicYear, double days) throws SQLException {
        String sql = "UPDATE leave_balances SET pending = GREATEST(0, pending - ?), updated_at = CURRENT_TIMESTAMP " +
                     "WHERE user_id = ? AND leave_type_id = ? AND academic_year = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, days); ps.setInt(2, userId);
            ps.setInt(3, leaveTypeId); ps.setString(4, academicYear);
            ps.executeUpdate();
        }
    }

    /** Restore used (on cancellation of approved leave) */
    public void restoreUsed(int userId, int leaveTypeId, String academicYear, double days) throws SQLException {
        String sql = "UPDATE leave_balances SET used = GREATEST(0, used - ?), updated_at = CURRENT_TIMESTAMP " +
                     "WHERE user_id = ? AND leave_type_id = ? AND academic_year = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, days); ps.setInt(2, userId);
            ps.setInt(3, leaveTypeId); ps.setString(4, academicYear);
            ps.executeUpdate();
        }
    }

    /** Record a balance transaction */
    public void recordTransaction(int balanceId, Integer applicationId, String type, double days, String reason) throws SQLException {
        String sql = "INSERT INTO balance_transactions (balance_id, application_id, transaction_type, days, reason) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, balanceId);
            if (applicationId != null) ps.setInt(2, applicationId);
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, type); ps.setDouble(4, days); ps.setString(5, reason);
            ps.executeUpdate();
        }
    }

    // ========== Notifications ==========
    public void createNotification(int userId, String title, String message, String type, Integer refId) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, title, message, notif_type, reference_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, title);
            ps.setString(3, message); ps.setString(4, type);
            if (refId != null) ps.setInt(5, refId);
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getNotifications(int userId) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> n = new HashMap<>();
                n.put("id", rs.getInt("id"));
                n.put("title", rs.getString("title"));
                n.put("message", rs.getString("message"));
                n.put("notif_type", rs.getString("notif_type"));
                n.put("is_read", rs.getBoolean("is_read"));
                n.put("created_at", rs.getString("created_at"));
                list.add(n);
            }
        }
        return list;
    }

    public void markNotificationRead(int notifId, int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = true WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notifId); ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ========== Audit Logs ==========
    public void addAuditLog(Integer userId, String action, String entityType, Integer entityId, String details, String ip) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, action); ps.setString(3, entityType);
            if (entityId != null) ps.setInt(4, entityId);
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, details); ps.setString(6, ip);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getAuditLogs(int limit) throws SQLException {
        String sql = "SELECT al.*, u.name AS user_name FROM audit_logs al " +
                     "LEFT JOIN users u ON al.user_id = u.id ORDER BY al.created_at DESC LIMIT ?";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> l = new HashMap<>();
                l.put("id", rs.getInt("id"));
                l.put("user_name", rs.getString("user_name"));
                l.put("action", rs.getString("action"));
                l.put("entity_type", rs.getString("entity_type"));
                l.put("entity_id", rs.getInt("entity_id"));
                l.put("details", rs.getString("details"));
                l.put("created_at", rs.getString("created_at"));
                list.add(l);
            }
        }
        return list;
    }

    // ========== Reports ==========
    public List<Map<String, Object>> generateReport(String type, Integer departmentId) throws SQLException {
        String sql;
        switch (type) {
            case "department":
                sql = "SELECT d.name AS department, lt.name AS leave_type, " +
                      "COUNT(la.id) AS applications, SUM(CASE WHEN la.status='APPROVED' THEN la.total_days ELSE 0 END) AS approved_days " +
                      "FROM leave_applications la JOIN users u ON la.user_id = u.id " +
                      "JOIN departments d ON u.department_id = d.id JOIN leave_types lt ON la.leave_type_id = lt.id " +
                      (departmentId != null && departmentId > 0 ? "WHERE u.department_id = " + departmentId + " " : "") +
                      "GROUP BY d.name, lt.name ORDER BY d.name, lt.name";
                break;
            case "utilization":
                sql = "SELECT u.name AS faculty, d.name AS department, lt.name AS leave_type, " +
                      "lb.total_entitled, lb.used, lb.pending, (lb.total_entitled - lb.used - lb.pending) AS remaining " +
                      "FROM leave_balances lb JOIN users u ON lb.user_id = u.id " +
                      "JOIN departments d ON u.department_id = d.id JOIN leave_types lt ON lb.leave_type_id = lt.id " +
                      (departmentId != null && departmentId > 0 ? "WHERE u.department_id = " + departmentId + " " : "") +
                      "ORDER BY u.name, lt.name";
                break;
            default: // summary
                sql = "SELECT u.name AS faculty, d.name AS department, " +
                      "COUNT(la.id) AS total_applications, " +
                      "SUM(CASE WHEN la.status='APPROVED' THEN 1 ELSE 0 END) AS approved, " +
                      "SUM(CASE WHEN la.status='REJECTED' THEN 1 ELSE 0 END) AS rejected, " +
                      "SUM(CASE WHEN la.status IN ('SUBMITTED','PENDING') THEN 1 ELSE 0 END) AS pending " +
                      "FROM leave_applications la JOIN users u ON la.user_id = u.id " +
                      "LEFT JOIN departments d ON u.department_id = d.id " +
                      (departmentId != null && departmentId > 0 ? "WHERE u.department_id = " + departmentId + " " : "") +
                      "GROUP BY u.name, d.name ORDER BY u.name";
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }
        }
        return results;
    }

    /** Admin stats */
    public Map<String, Object> getAdminStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            // Total users
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE is_active = true")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("total_users", rs.getInt(1));
            }
            // Total departments
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM departments WHERE is_active = true")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("total_departments", rs.getInt(1));
            }
            // Pending approvals
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM leave_applications WHERE status IN ('SUBMITTED','PENDING')")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("pending_approvals", rs.getInt(1));
            }
            // Approved this month
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM leave_applications WHERE status = 'APPROVED' AND decided_at >= date_trunc('month', CURRENT_DATE)")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("approved_this_month", rs.getInt(1));
            }
        }
        return stats;
    }
}

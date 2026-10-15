package repositories;

import models.User;
import java.sql.*;
import java.util.*;

/**
 * UserRepository — database operations for the users table.
 * Uses JDBC prepared statements to prevent SQL injection.
 */
public class UserRepository {

    /** Find a user by their database ID */
    public User findById(int id) throws SQLException {
        String sql = "SELECT u.*, d.name AS department_name, a.name AS approver_name " +
                     "FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "LEFT JOIN users a ON u.approver_id = a.id " +
                     "WHERE u.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    /** Find a user by their email */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT u.*, d.name AS department_name FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE u.email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    /** Find a user by their Google ID */
    public User findByGoogleId(String googleId) throws SQLException {
        String sql = "SELECT u.*, d.name AS department_name FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE u.google_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, googleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    /** Get all users with optional role filter */
    public List<User> findAll(String roleFilter) throws SQLException {
        String sql = "SELECT u.*, d.name AS department_name FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id ";
        if (roleFilter != null && !roleFilter.isEmpty()) {
            sql += "WHERE u.role = ? ";
        }
        sql += "ORDER BY u.name ASC";

        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (roleFilter != null && !roleFilter.isEmpty()) {
                ps.setString(1, roleFilter);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    /** Create a new user (from Google sign-in) */
    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (google_id, email, name, profile_picture, department_id, " +
                     "designation, role, approver_id, is_active, is_approved) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getGoogleId());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getName());
            ps.setString(4, user.getProfilePicture());
            if (user.getDepartmentId() > 0) ps.setInt(5, user.getDepartmentId());
            else ps.setNull(5, Types.INTEGER);
            ps.setString(6, user.getDesignation());
            ps.setString(7, user.getRole() != null ? user.getRole() : "FACULTY");
            if (user.getApproverId() > 0) ps.setInt(8, user.getApproverId());
            else ps.setNull(8, Types.INTEGER);
            ps.setBoolean(9, user.isActive());
            ps.setBoolean(10, user.isApproved());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user.setId(rs.getInt("id"));
                user.setCreatedAt(rs.getString("created_at"));
            }
        }
        return user;
    }

    /** Update an existing user */
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, department_id = ?, designation = ?, " +
                     "role = ?, approver_id = ?, is_active = ?, is_approved = ?, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            if (user.getDepartmentId() > 0) ps.setInt(2, user.getDepartmentId());
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, user.getDesignation());
            ps.setString(4, user.getRole());
            if (user.getApproverId() > 0) ps.setInt(5, user.getApproverId());
            else ps.setNull(5, Types.INTEGER);
            ps.setBoolean(6, user.isActive());
            ps.setBoolean(7, user.isApproved());
            ps.setInt(8, user.getId());
            ps.executeUpdate();
        }
    }

    /** Update Google profile info (on sign-in) */
    public void updateGoogleProfile(int userId, String name, String picture, String googleId) throws SQLException {
        String sql = "UPDATE users SET name = ?, profile_picture = ?, google_id = ?, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, picture);
            ps.setString(3, googleId);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    /** Count users by approval status */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = true";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    /** Map a ResultSet row to a User object */
    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setGoogleId(rs.getString("google_id"));
        u.setEmail(rs.getString("email"));
        u.setName(rs.getString("name"));
        u.setProfilePicture(rs.getString("profile_picture"));
        u.setDepartmentId(rs.getInt("department_id"));
        u.setDesignation(rs.getString("designation"));
        u.setRole(rs.getString("role"));
        u.setApproverId(rs.getInt("approver_id"));
        u.setActive(rs.getBoolean("is_active"));
        u.setApproved(rs.getBoolean("is_approved"));
        u.setCreatedAt(rs.getString("created_at"));
        u.setUpdatedAt(rs.getString("updated_at"));
        // Joined fields
        try { u.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        try { u.setApproverName(rs.getString("approver_name")); } catch (SQLException ignored) {}
        return u;
    }
}

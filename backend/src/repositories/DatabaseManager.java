package repositories;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/**
 * DatabaseManager — manages database connectivity and auto-migration.
 * Compatible with Supabase cloud PostgreSQL as well as local PostgreSQL.
 *
 * Automatically parses Supabase connection strings, enforces SSL,
 * and initializes schema and seed data on startup.
 */
public class DatabaseManager {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    /**
     * Initialize connection settings from db.properties or environment variables.
     */
    public static void initialize() {
        Properties props = loadProperties();

        // 1. Check for Supabase URL first
        String supabaseUrl = getVal(props, "SUPABASE_URL");
        if (supabaseUrl == null || supabaseUrl.trim().isEmpty()) {
            supabaseUrl = getVal(props, "DATABASE_URL");
        }

        if (supabaseUrl != null && !supabaseUrl.trim().isEmpty()) {
            parseAndConfigureSupabase(supabaseUrl.trim());
        } else {
            // 2. Standard configuration
            dbUrl = getVal(props, "DB_URL");
            dbUser = getVal(props, "DB_USER");
            dbPassword = getVal(props, "DB_PASSWORD");

            if (dbUrl == null || dbUrl.isEmpty()) {
                dbUrl = "jdbc:postgresql://localhost:5432/leave_management";
            }
            if (dbUser == null || dbUser.isEmpty()) {
                dbUser = "postgres";
            }
            if (dbPassword == null || dbPassword.isEmpty()) {
                dbPassword = "postgres";
            }
        }

        // Load PostgreSQL JDBC driver
        try {
            Class.forName("org.postgresql.Driver");
            initialized = true;
            System.out.println("[DB] PostgreSQL driver loaded.");
            System.out.println("[DB] Connecting to: " + maskUrl(dbUrl));
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] ERROR: PostgreSQL JDBC driver not found in classpath!");
            throw new RuntimeException("PostgreSQL driver not found", e);
        }
    }

    /**
     * Parses a Supabase connection string (either URI format or JDBC format).
     * e.g., postgresql://postgres.[REF]:[PASS]@[HOST]:[PORT]/postgres
     * or jdbc:postgresql://[HOST]:[PORT]/postgres?user=...&password=...&sslmode=require
     */
    private static void parseAndConfigureSupabase(String raw) {
        try {
            if (raw.startsWith("postgresql://") || raw.startsWith("postgres://")) {
                String clean = raw.replaceFirst("^postgres(ql)?://", "");
                int atIdx = clean.lastIndexOf('@');
                if (atIdx > 0) {
                    String userPass = clean.substring(0, atIdx);
                    String hostPortPath = clean.substring(atIdx + 1);

                    int colonIdx = userPass.indexOf(':');
                    if (colonIdx > 0) {
                        dbUser = userPass.substring(0, colonIdx);
                        dbPassword = userPass.substring(colonIdx + 1);
                        // Auto-strip brackets if accidentally left in
                        if (dbPassword.startsWith("[") && dbPassword.endsWith("]")) {
                            dbPassword = dbPassword.substring(1, dbPassword.length() - 1);
                        }
                    } else {
                        dbUser = userPass;
                    }

                    String pathQuery = "/postgres";
                    String hostPort = hostPortPath;
                    int slashIdx = hostPortPath.indexOf('/');
                    if (slashIdx >= 0) {
                        hostPort = hostPortPath.substring(0, slashIdx);
                        pathQuery = hostPortPath.substring(slashIdx);
                    }

                    String host = hostPort;
                    int port = 5432;
                    int portColon = hostPort.lastIndexOf(':');
                    if (portColon > 0) {
                        host = hostPort.substring(0, portColon);
                        try {
                            port = Integer.parseInt(hostPort.substring(portColon + 1));
                        } catch (NumberFormatException ignored) {}
                    }

                    String path = pathQuery.contains("?") ? pathQuery.substring(0, pathQuery.indexOf('?')) : pathQuery;
                    if (path.isEmpty()) path = "/postgres";

                    dbUrl = "jdbc:postgresql://" + host + ":" + port + path + "?sslmode=require";
                    System.out.println("[DB] Configured from Supabase URI for host: " + host + " (port " + port + ")");
                }
            } else if (raw.startsWith("jdbc:postgresql://")) {
                dbUrl = raw;
                if (!dbUrl.contains("sslmode=")) {
                    dbUrl += (dbUrl.contains("?") ? "&" : "?") + "sslmode=require";
                }
                System.out.println("[DB] Configured from Supabase JDBC URL.");
            } else {
                dbUrl = raw;
            }
        } catch (Exception e) {
            System.err.println("[DB] Notice: Fallback parsing Supabase URL: " + e.getMessage());
            dbUrl = raw;
        }
    }

    /**
     * Get a database connection.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database not initialized. Call DatabaseManager.initialize() first.");
        }
        if (dbUser != null && !dbUser.isEmpty() && dbPassword != null) {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Test connection and auto-migrate tables and seed data.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                autoMigrateAndSeed(conn);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("=================================================================");
            System.err.println("[DB] Database connection error: " + e.getMessage());
            System.err.println("[DB] If you are using Supabase:");
            System.err.println("     1. Open 'db.properties' in your project root.");
            System.err.println("     2. Paste your Supabase connection URI under SUPABASE_URL=...");
            System.err.println("     3. Verify your database password is correct.");
            System.err.println("=================================================================");
            return false;
        }
    }

    /**
     * Automatically creates all tables and initial seed data if not present.
     * Keeps database setup zero-effort for the student.
     */
    public static void autoMigrateAndSeed(Connection conn) {
        try (Statement st = conn.createStatement()) {
            System.out.println("[DB] Checking and auto-migrating database schema...");

            // 1. Sequence for application numbers
            st.execute("CREATE SEQUENCE IF NOT EXISTS app_number_seq START WITH 1001;");

            // 2. Departments
            st.execute("CREATE TABLE IF NOT EXISTS departments (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL UNIQUE, " +
                    "code VARCHAR(20) NOT NULL UNIQUE, " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 3. Users
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "google_id VARCHAR(255) UNIQUE, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "name VARCHAR(150) NOT NULL, " +
                    "profile_picture TEXT, " +
                    "department_id INTEGER REFERENCES departments(id), " +
                    "designation VARCHAR(100), " +
                    "role VARCHAR(20) NOT NULL DEFAULT 'FACULTY' CHECK (role IN ('FACULTY', 'HOD', 'ADMIN')), " +
                    "approver_id INTEGER REFERENCES users(id), " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "is_approved BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 4. Sessions
            st.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                    "id VARCHAR(128) PRIMARY KEY, " +
                    "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at TIMESTAMP NOT NULL" +
                    ");");

            // 5. Leave Types
            st.execute("CREATE TABLE IF NOT EXISTS leave_types (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(80) NOT NULL UNIQUE, " +
                    "code VARCHAR(20) NOT NULL UNIQUE, " +
                    "description TEXT, " +
                    "requires_document BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 6. Leave Policies
            st.execute("CREATE TABLE IF NOT EXISTS leave_policies (" +
                    "id SERIAL PRIMARY KEY, " +
                    "leave_type_id INTEGER NOT NULL REFERENCES leave_types(id) ON DELETE CASCADE, " +
                    "designation VARCHAR(100) NOT NULL, " +
                    "days_entitled NUMERIC(5,1) NOT NULL DEFAULT 0, " +
                    "carry_forward BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "academic_year VARCHAR(9) NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(leave_type_id, designation, academic_year)" +
                    ");");

            // 7. Leave Applications
            st.execute("CREATE TABLE IF NOT EXISTS leave_applications (" +
                    "id SERIAL PRIMARY KEY, " +
                    "application_no VARCHAR(30) NOT NULL UNIQUE, " +
                    "user_id INTEGER NOT NULL REFERENCES users(id), " +
                    "leave_type_id INTEGER NOT NULL REFERENCES leave_types(id), " +
                    "start_date DATE NOT NULL, " +
                    "end_date DATE NOT NULL, " +
                    "half_day_start BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "half_day_end BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "total_days NUMERIC(5,1) NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "supporting_doc TEXT, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', " +
                    "approver_id INTEGER REFERENCES users(id), " +
                    "submitted_at TIMESTAMP, " +
                    "decided_at TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 8. Approval History
            st.execute("CREATE TABLE IF NOT EXISTS approval_history (" +
                    "id SERIAL PRIMARY KEY, " +
                    "application_id INTEGER NOT NULL REFERENCES leave_applications(id) ON DELETE CASCADE, " +
                    "action VARCHAR(20) NOT NULL, " +
                    "actor_id INTEGER NOT NULL REFERENCES users(id), " +
                    "comments TEXT, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 9. Leave Balances
            st.execute("CREATE TABLE IF NOT EXISTS leave_balances (" +
                    "id SERIAL PRIMARY KEY, " +
                    "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "leave_type_id INTEGER NOT NULL REFERENCES leave_types(id) ON DELETE CASCADE, " +
                    "academic_year VARCHAR(9) NOT NULL, " +
                    "total_entitled NUMERIC(5,1) NOT NULL DEFAULT 0, " +
                    "used NUMERIC(5,1) NOT NULL DEFAULT 0, " +
                    "pending NUMERIC(5,1) NOT NULL DEFAULT 0, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(user_id, leave_type_id, academic_year)" +
                    ");");

            // 10. Balance Transactions
            st.execute("CREATE TABLE IF NOT EXISTS balance_transactions (" +
                    "id SERIAL PRIMARY KEY, " +
                    "balance_id INTEGER NOT NULL REFERENCES leave_balances(id) ON DELETE CASCADE, " +
                    "application_id INTEGER REFERENCES leave_applications(id), " +
                    "transaction_type VARCHAR(20) NOT NULL, " +
                    "days NUMERIC(5,1) NOT NULL, " +
                    "reason TEXT, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 11. Holidays
            st.execute("CREATE TABLE IF NOT EXISTS holidays (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(150) NOT NULL, " +
                    "holiday_date DATE NOT NULL, " +
                    "holiday_type VARCHAR(20) NOT NULL DEFAULT 'INSTITUTIONAL', " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(holiday_date, name)" +
                    ");");

            // 12. Notifications
            st.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "id SERIAL PRIMARY KEY, " +
                    "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "title VARCHAR(200) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "notif_type VARCHAR(30) NOT NULL DEFAULT 'INFO', " +
                    "reference_id INTEGER, " +
                    "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 13. Audit Logs
            st.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id SERIAL PRIMARY KEY, " +
                    "user_id INTEGER REFERENCES users(id), " +
                    "action VARCHAR(100) NOT NULL, " +
                    "entity_type VARCHAR(50), " +
                    "entity_id INTEGER, " +
                    "details TEXT, " +
                    "ip_address VARCHAR(45), " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // Seed default data if empty
            seedInitialDataIfEmpty(conn);

            System.out.println("[DB] Database schema and initial data are fully prepared!");

        } catch (SQLException e) {
            System.err.println("[DB] Auto-migration notice: " + e.getMessage());
        }
    }

    private static void seedInitialDataIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            // Check if departments seeded
            ResultSet rsDept = st.executeQuery("SELECT COUNT(*) FROM departments");
            if (rsDept.next() && rsDept.getInt(1) == 0) {
                st.execute("INSERT INTO departments (name, code) VALUES " +
                        "('Computer Science', 'CS'), " +
                        "('Information Technology', 'IT'), " +
                        "('Electronics', 'ECE'), " +
                        "('Mechanical Engineering', 'ME'), " +
                        "('Civil Engineering', 'CE') ON CONFLICT DO NOTHING;");
            }

            // Check if leave types seeded
            ResultSet rsTypes = st.executeQuery("SELECT COUNT(*) FROM leave_types");
            if (rsTypes.next() && rsTypes.getInt(1) == 0) {
                st.execute("INSERT INTO leave_types (name, code, description, requires_document) VALUES " +
                        "('Casual Leave', 'CL', 'Short-duration leave for personal reasons', FALSE), " +
                        "('Sick Leave', 'SL', 'Leave due to illness or medical reasons', TRUE), " +
                        "('Earned Leave', 'EL', 'Pre-planned leave earned through service', FALSE), " +
                        "('Academic Duty Leave', 'ADL', 'Leave for conferences, workshops, or seminars', FALSE), " +
                        "('Maternity Leave', 'ML', 'Leave for maternity purposes', TRUE), " +
                        "('Unpaid Leave', 'UPL', 'Leave without pay when balances are exhausted', FALSE) " +
                        "ON CONFLICT DO NOTHING;");
            }

            // Check if policies seeded
            ResultSet rsPol = st.executeQuery("SELECT COUNT(*) FROM leave_policies");
            if (rsPol.next() && rsPol.getInt(1) == 0) {
                st.execute("INSERT INTO leave_policies (leave_type_id, designation, days_entitled, carry_forward, academic_year) " +
                        "SELECT id, 'Professor', CASE code WHEN 'CL' THEN 12 WHEN 'SL' THEN 10 WHEN 'EL' THEN 15 WHEN 'ADL' THEN 10 WHEN 'ML' THEN 180 WHEN 'UPL' THEN 30 END, (code='EL'), '2026-2027' FROM leave_types " +
                        "UNION ALL " +
                        "SELECT id, 'Associate Professor', CASE code WHEN 'CL' THEN 12 WHEN 'SL' THEN 10 WHEN 'EL' THEN 12 WHEN 'ADL' THEN 8 WHEN 'ML' THEN 180 WHEN 'UPL' THEN 20 END, (code='EL'), '2026-2027' FROM leave_types " +
                        "UNION ALL " +
                        "SELECT id, 'Assistant Professor', CASE code WHEN 'CL' THEN 10 WHEN 'SL' THEN 8 WHEN 'EL' THEN 10 WHEN 'ADL' THEN 5 WHEN 'ML' THEN 180 WHEN 'UPL' THEN 15 END, (code='EL'), '2026-2027' FROM leave_types " +
                        "ON CONFLICT DO NOTHING;");
            }

            // Check if holidays seeded
            ResultSet rsHol = st.executeQuery("SELECT COUNT(*) FROM holidays");
            if (rsHol.next() && rsHol.getInt(1) == 0) {
                st.execute("INSERT INTO holidays (name, holiday_date, holiday_type) VALUES " +
                        "('Republic Day', '2027-01-26', 'NATIONAL'), " +
                        "('Independence Day', '2026-08-15', 'NATIONAL'), " +
                        "('Gandhi Jayanti', '2026-10-02', 'NATIONAL'), " +
                        "('Diwali', '2026-11-01', 'NATIONAL'), " +
                        "('Christmas', '2026-12-25', 'NATIONAL'), " +
                        "('New Year', '2027-01-01', 'INSTITUTIONAL') " +
                        "ON CONFLICT DO NOTHING;");
            }

            // Check if demo users seeded
            ResultSet rsUsers = st.executeQuery("SELECT COUNT(*) FROM users");
            if (rsUsers.next() && rsUsers.getInt(1) == 0) {
                st.execute("INSERT INTO users (id, email, name, department_id, designation, role, is_active, is_approved) VALUES " +
                        "(1, 'admin@institution.edu', 'Prof. Albus Dumbledore', 1, 'Professor', 'ADMIN', TRUE, TRUE), " +
                        "(2, 'hod.cs@institution.edu', 'Dr. Alan Turing', 1, 'Professor', 'HOD', TRUE, TRUE), " +
                        "(3, 'faculty.cs@institution.edu', 'Dr. Ada Lovelace', 1, 'Associate Professor', 'FACULTY', TRUE, TRUE), " +
                        "(4, 'faculty.it@institution.edu', 'Prof. Grace Hopper', 2, 'Assistant Professor', 'FACULTY', TRUE, TRUE) " +
                        "ON CONFLICT (id) DO UPDATE SET is_active=TRUE, is_approved=TRUE;");

                st.execute("UPDATE users SET approver_id = 2 WHERE id IN (3, 4);");
                st.execute("SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));");

                // Initialize balances for the demo users
                st.execute("INSERT INTO leave_balances (user_id, leave_type_id, academic_year, total_entitled, used, pending) " +
                        "SELECT u.id, lp.leave_type_id, '2026-2027', lp.days_entitled, 0, 0 " +
                        "FROM users u " +
                        "JOIN leave_policies lp ON u.designation = lp.designation AND lp.academic_year = '2026-2027' " +
                        "WHERE u.id IN (1, 2, 3, 4) " +
                        "ON CONFLICT DO NOTHING;");
            }
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        String[] candidatePaths = {"db.properties", "../db.properties", "config.properties", "../config.properties"};
        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    props.load(fis);
                    System.out.println("[DB] Loaded configuration from: " + f.getAbsolutePath());
                    return props;
                } catch (IOException ignored) {}
            }
        }
        return props;
    }

    private static String getVal(Properties props, String key) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) return val.trim();
        val = props.getProperty(key);
        if (val != null && !val.trim().isEmpty()) return val.trim();
        return null;
    }

    private static String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^/@:]+@", ":****@");
    }

    public static void close(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    public static void close(PreparedStatement ps) {
        if (ps != null) {
            try { ps.close(); } catch (SQLException ignored) {}
        }
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
    }
}

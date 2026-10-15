import repositories.DatabaseManager;
import server.AppServer;

import java.io.File;

/**
 * Main — Entry point for the Faculty Leave Management System backend.
 * Semester III Industry Practice-I Project.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("--------------------------------------------------");
        System.out.println("Starting Faculty Leave Management System Backend...");
        System.out.println("Semester III Java Full Stack Development Project");
        System.out.println("--------------------------------------------------");

        // 1. Initialize Database
        try {
            DatabaseManager.initialize();
            boolean dbConnected = DatabaseManager.testConnection();
            if (dbConnected) {
                System.out.println("[DB] PostgreSQL Database connection established successfully!");
            } else {
                System.err.println("[DB] WARNING: Could not establish active connection to PostgreSQL.");
                System.err.println("[DB] Check that PostgreSQL service is running and DB_URL/DB_USER/DB_PASSWORD are correct.");
            }
        } catch (Exception e) {
            System.err.println("[DB] Database initialization notice: " + e.getMessage());
        }

        // 2. Resolve Frontend Directory
        String frontendDir = "frontend";
        File dirCheck = new File(frontendDir);
        if (!dirCheck.exists()) {
            dirCheck = new File("../frontend");
            if (dirCheck.exists()) {
                frontendDir = "../frontend";
            }
        }

        // 3. Resolve Port
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                port = Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {}
        }

        // 4. Start Server
        try {
            AppServer server = new AppServer(port, frontendDir);
            server.start();

            // Open browser automatically once server is running
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:" + port));
                } catch (Exception ignored) {}
            }

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[Server] Shutting down gracefully...");
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("[Server] FATAL: Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

package server;

import com.sun.net.httpserver.HttpServer;
import handlers.*;
import middleware.AuthMiddleware;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * AppServer — Main HTTP Server configuration and routing.
 * Configures endpoints with role-based access control and static file serving.
 */
public class AppServer {

    private final int port;
    private final String staticDir;
    private HttpServer server;

    public AppServer(int port, String staticDir) {
        this.port = port;
        this.staticDir = staticDir;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Handlers
        AuthHandler authHandler = new AuthHandler();
        CommonHandler commonHandler = new CommonHandler();
        LeaveHandler leaveHandler = new LeaveHandler();
        ApprovalHandler approvalHandler = new ApprovalHandler();
        AdminHandler adminHandler = new AdminHandler();
        NotificationHandler notificationHandler = new NotificationHandler();

        // 1. Auth routes (public / session-creating)
        server.createContext("/api/auth", authHandler);
        server.createContext("/api/config", authHandler);

        // 2. Reference data routes (departments, leave types, holidays)
        server.createContext("/api/departments", commonHandler);
        server.createContext("/api/leave-types", commonHandler);
        server.createContext("/api/holidays", commonHandler);

        // 3. Faculty Leave routes (requires authentication)
        server.createContext("/api/leave", AuthMiddleware.requireAuth(leaveHandler));

        // 4. Approval routes (requires HOD or ADMIN role)
        server.createContext("/api/approval", AuthMiddleware.requireRole(approvalHandler, "HOD", "ADMIN"));

        // 5. Admin routes (requires ADMIN role)
        server.createContext("/api/admin", AuthMiddleware.requireRole(adminHandler, "ADMIN"));

        // 6. Notification routes (requires authentication)
        server.createContext("/api/notifications", AuthMiddleware.requireAuth(notificationHandler));

        // 7. Static file serving (frontend HTML/CSS/JS/assets)
        server.createContext("/", new StaticFileHandler(staticDir));

        // Multi-threaded request execution
        server.setExecutor(Executors.newFixedThreadPool(12));
        server.start();

        System.out.println("=================================================");
        System.out.println("🎓 Faculty Leave Management System Server Running");
        System.out.println("🔗 URL: http://localhost:" + port);
        System.out.println("=================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[Server] Stopped.");
        }
    }
}

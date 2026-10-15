package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * StaticFileHandler — serves frontend HTML, CSS, JavaScript, and asset files.
 * Protects against directory traversal and correctly sets MIME content types.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path rootDir;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("csv", "text/csv; charset=UTF-8");
    }

    public StaticFileHandler(String staticDir) {
        Path path = Paths.get(staticDir).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            // Try fallback relative path
            path = Paths.get("../frontend").toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                path = Paths.get("frontend").toAbsolutePath().normalize();
            }
        }
        this.rootDir = path;
        System.out.println("[Static] Serving frontend static files from: " + this.rootDir);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath == null || requestPath.equals("/") || requestPath.isEmpty()) {
            requestPath = "/index.html";
        }

        // Clean path to prevent path traversal
        requestPath = requestPath.replace('\\', '/');
        if (requestPath.startsWith("/")) {
            requestPath = requestPath.substring(1);
        }

        Path target = rootDir.resolve(requestPath).normalize();

        // Security check: ensure target path is within rootDir
        if (!target.startsWith(rootDir)) {
            sendError(exchange, 403, "Access Denied");
            return;
        }

        if (Files.isDirectory(target)) {
            target = target.resolve("index.html");
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            sendError(exchange, 404, "404 Not Found");
            return;
        }

        String ext = "";
        String filename = target.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            ext = filename.substring(dot + 1).toLowerCase();
        }

        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        exchange.getResponseHeaders().set("Content-Type", contentType);

        byte[] bytes = Files.readAllBytes(target);
        exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(method) ? -1 : bytes.length);

        if (!"HEAD".equalsIgnoreCase(method)) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

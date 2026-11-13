package com.civicsphere;

import com.civicsphere.dao.CategoryDAO;
import com.civicsphere.model.Category;
import com.civicsphere.model.Complaint;
import com.civicsphere.model.ComplaintHistory;
import com.civicsphere.model.Department;
import com.civicsphere.model.RecurrenceRecord;
import com.civicsphere.model.User;
import com.civicsphere.service.AdminService;
import com.civicsphere.service.AuthService;
import com.civicsphere.service.ComplaintService;
import com.civicsphere.service.DepartmentService;
import com.civicsphere.service.RecurrenceService;
import com.civicsphere.util.JsonUtil;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Standalone Embedded Server for CivicSphere.
 * Allows running the complete web application with a single command without needing
 * to configure an external Apache Tomcat server.
 */
public class CivicSphereServer {

    private static final Logger LOGGER = Logger.getLogger(CivicSphereServer.class.getName());
    private static final int DEFAULT_PORT = 8080;
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    private static final AuthService authService = new AuthService();
    private static final ComplaintService complaintService = new ComplaintService();
    private static final RecurrenceService recurrenceService = new RecurrenceService();
    private static final AdminService adminService = new AdminService();
    private static final DepartmentService departmentService = new DepartmentService();
    private static final CategoryDAO categoryDAO = new CategoryDAO();

    public static class SessionData {
        public int userId;
        public String role;
        public String fullName;
        public String email;
        public long lastAccessed = System.currentTimeMillis();
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        String envPort = System.getenv("PORT");
        if (envPort != null) {
            try { port = Integer.parseInt(envPort); } catch (NumberFormatException ignored) {}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        // API Handler
        server.createContext("/api", new ApiHandler());

        // Static Files Handler
        server.createContext("/", new StaticFileHandler());

        server.start();

        System.out.println("=====================================================================");
        System.out.println("  🏛️  CIVICSPHERE APPLICATION RUNNING SUCCESSFULLY");
        System.out.println("=====================================================================");
        System.out.println("  Access URL:         http://localhost:" + port + "/");
        System.out.println("  Citizen Portal:     http://localhost:" + port + "/citizen/dashboard.html");
        System.out.println("  Admin Portal:       http://localhost:" + port + "/admin/dashboard.html");
        System.out.println("---------------------------------------------------------------------");
        System.out.println("  Demo Credentials:");
        System.out.println("    Admin:   admin@civicsphere.gov   / Admin@123");
        System.out.println("    Citizen: citizen@example.com     / Citizen@123");
        System.out.println("=====================================================================");
        System.out.println("  Press Ctrl+C to stop the server.");
    }

    private static SessionData getSession(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        List<String> cookies = headers.get("Cookie");
        if (cookies != null) {
            for (String cookieHeader : cookies) {
                String[] parts = cookieHeader.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("JSESSIONID=")) {
                        String sessionId = part.substring("JSESSIONID=".length());
                        return sessions.get(sessionId);
                    }
                }
            }
        }
        return null;
    }

    private static String createSession(HttpExchange exchange, User user) {
        String sessionId = UUID.randomUUID().toString();
        SessionData data = new SessionData();
        data.userId = user.getUserId();
        data.role = user.getRole();
        data.fullName = user.getFullName();
        data.email = user.getEmail();
        sessions.put(sessionId, data);

        exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
        return sessionId;
    }

    private static void invalidateSession(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        List<String> cookies = headers.get("Cookie");
        if (cookies != null) {
            for (String cookieHeader : cookies) {
                for (String part : cookieHeader.split(";")) {
                    part = part.trim();
                    if (part.startsWith("JSESSIONID=")) {
                        String sessionId = part.substring("JSESSIONID=".length());
                        sessions.remove(sessionId);
                    }
                }
            }
        }
        exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=deleted; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            SessionData session = getSession(exchange);

            try {
                // Auth Routes
                if (path.equals("/api/auth/login") && "POST".equals(method)) {
                    JsonObject json = parseJson(exchange);
                    User user = authService.authenticate(json.get("email").getAsString(), json.get("password").getAsString());
                    createSession(exchange, user);
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", user.getUserId());
                    map.put("fullName", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    sendJson(exchange, 200, true, "Login successful", map);
                    return;
                }

                if (path.equals("/api/auth/register") && "POST".equals(method)) {
                    JsonObject json = parseJson(exchange);
                    User user = authService.registerCitizen(
                            json.get("fullName").getAsString(),
                            json.get("email").getAsString(),
                            json.get("phone").getAsString(),
                            json.get("password").getAsString(),
                            json.get("confirmPassword").getAsString(),
                            json.has("address") ? json.get("address").getAsString() : ""
                    );
                    createSession(exchange, user);
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", user.getUserId());
                    map.put("fullName", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    sendJson(exchange, 201, true, "Registration successful", map);
                    return;
                }

                if (path.equals("/api/auth/logout") && "POST".equals(method)) {
                    invalidateSession(exchange);
                    sendJson(exchange, 200, true, "Logged out", null);
                    return;
                }

                if (path.equals("/api/auth/me") && "GET".equals(method)) {
                    if (session == null) {
                        sendError(exchange, 401, "Not logged in");
                        return;
                    }
                    User user = authService.getUserById(session.userId);
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", user.getUserId());
                    map.put("fullName", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("phone", user.getPhone());
                    map.put("role", user.getRole());
                    map.put("address", user.getAddress());
                    sendJson(exchange, 200, true, "User profile", map);
                    return;
                }

                // Categories
                if (path.equals("/api/categories") && "GET".equals(method)) {
                    List<Category> list = categoryDAO.findAll();
                    sendJson(exchange, 200, true, "Categories list", list);
                    return;
                }

                // Departments
                if (path.equals("/api/departments") && "GET".equals(method)) {
                    List<Department> list = departmentService.getAllDepartments();
                    sendJson(exchange, 200, true, "Departments list", list);
                    return;
                }
                if (path.equals("/api/departments") && "POST".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) {
                        sendError(exchange, 403, "Admin required"); return;
                    }
                    JsonObject json = parseJson(exchange);
                    Department d = departmentService.createDepartment(
                            json.get("departmentName").getAsString(),
                            json.has("description") ? json.get("description").getAsString() : "",
                            json.has("contactEmail") ? json.get("contactEmail").getAsString() : ""
                    );
                    sendJson(exchange, 201, true, "Department created", d);
                    return;
                }
                if (path.matches("/api/departments/\\d+") && "PUT".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) {
                        sendError(exchange, 403, "Admin required"); return;
                    }
                    int id = Integer.parseInt(path.substring("/api/departments/".length()));
                    JsonObject json = parseJson(exchange);
                    departmentService.updateDepartment(id,
                            json.get("departmentName").getAsString(),
                            json.has("description") ? json.get("description").getAsString() : "",
                            json.has("contactEmail") ? json.get("contactEmail").getAsString() : "");
                    sendJson(exchange, 200, true, "Department updated", null);
                    return;
                }
                if (path.matches("/api/departments/\\d+") && "DELETE".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) {
                        sendError(exchange, 403, "Admin required"); return;
                    }
                    int id = Integer.parseInt(path.substring("/api/departments/".length()));
                    departmentService.deleteDepartment(id);
                    sendJson(exchange, 200, true, "Department deleted", null);
                    return;
                }

                // Complaints
                if (path.equals("/api/complaints/my") && "GET".equals(method)) {
                    if (session == null) { sendError(exchange, 401, "Login required"); return; }
                    List<Complaint> list = complaintService.getComplaintsByUser(session.userId);
                    sendJson(exchange, 200, true, "My complaints", list);
                    return;
                }

                if (path.equals("/api/complaints") && "GET".equals(method)) {
                    Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
                    Integer catId = query.containsKey("categoryId") && !query.get("categoryId").isEmpty() ? Integer.parseInt(query.get("categoryId")) : null;
                    List<Complaint> list = complaintService.getAllComplaints(query.get("status"), catId, query.get("search"), query.get("city"), query.get("area"));
                    sendJson(exchange, 200, true, "Complaints list", list);
                    return;
                }

                if (path.matches("/api/complaints/\\d+") && "GET".equals(method)) {
                    int id = Integer.parseInt(path.substring("/api/complaints/".length()));
                    Complaint c = complaintService.getComplaintById(id);
                    if (c == null) { sendError(exchange, 404, "Complaint not found"); return; }
                    List<ComplaintHistory> history = complaintService.getComplaintHistory(id);
                    RecurrenceRecord rec = recurrenceService.getAllRecurrenceRecords(null).stream()
                            .filter(r -> r.getRecurringComplaintId() == id || r.getOriginalComplaintId() == id)
                            .findFirst().orElse(null);
                    Map<String, Object> resMap = new HashMap<>();
                    resMap.put("complaint", c);
                    resMap.put("history", history);
                    resMap.put("recurrence", rec);
                    sendJson(exchange, 200, true, "Complaint details", resMap);
                    return;
                }

                if (path.equals("/api/complaints") && "POST".equals(method)) {
                    if (session == null) { sendError(exchange, 401, "Login required"); return; }
                    Complaint c = parseComplaintBody(exchange);
                    Complaint saved = complaintService.submitComplaint(c, session.userId);
                    sendJson(exchange, 201, true, "Complaint submitted", saved);
                    return;
                }

                if (path.matches("/api/complaints/\\d+/status") && "PUT".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) { sendError(exchange, 403, "Admin required"); return; }
                    int id = Integer.parseInt(path.split("/")[3]);
                    JsonObject json = parseJson(exchange);
                    complaintService.updateStatus(id, json.get("status").getAsString(),
                            json.has("departmentId") && !json.get("departmentId").isJsonNull() ? json.get("departmentId").getAsInt() : null,
                            json.has("remarks") ? json.get("remarks").getAsString() : "", session.userId);
                    sendJson(exchange, 200, true, "Status updated", null);
                    return;
                }

                if (path.matches("/api/complaints/\\d+/resolve") && "PUT".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) { sendError(exchange, 403, "Admin required"); return; }
                    int id = Integer.parseInt(path.split("/")[3]);
                    JsonObject json = parseJson(exchange);
                    complaintService.resolveComplaint(id, json.get("remarks").getAsString(), null, session.userId);
                    sendJson(exchange, 200, true, "Complaint resolved", null);
                    return;
                }

                // Admin
                if (path.equals("/api/admin/dashboard") && "GET".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) { sendError(exchange, 403, "Admin required"); return; }
                    Map<String, Object> stats = adminService.getAdminDashboardStats();
                    sendJson(exchange, 200, true, "Admin dashboard stats", stats);
                    return;
                }

                if (path.matches("/api/admin/complaints/\\d+/assign") && "PUT".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) { sendError(exchange, 403, "Admin required"); return; }
                    int id = Integer.parseInt(path.split("/")[4]);
                    JsonObject json = parseJson(exchange);
                    complaintService.assignDepartment(id, json.get("departmentId").getAsInt(),
                            json.has("remarks") ? json.get("remarks").getAsString() : "", session.userId);
                    sendJson(exchange, 200, true, "Department assigned", null);
                    return;
                }

                // Recurrence
                if (path.equals("/api/recurrence") && "GET".equals(method)) {
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
                    List<RecurrenceRecord> list = recurrenceService.getAllRecurrenceRecords(q.get("status"));
                    sendJson(exchange, 200, true, "Recurrence records", list);
                    return;
                }
                if (path.equals("/api/recurrence/stats") && "GET".equals(method)) {
                    Map<String, Integer> stats = recurrenceService.getRecurrenceStatistics();
                    sendJson(exchange, 200, true, "Recurrence stats", stats);
                    return;
                }
                if (path.matches("/api/recurrence/\\d+/verify") && "PUT".equals(method)) {
                    if (session == null || !"ADMIN".equalsIgnoreCase(session.role)) { sendError(exchange, 403, "Admin required"); return; }
                    int id = Integer.parseInt(path.split("/")[3]);
                    JsonObject json = parseJson(exchange);
                    recurrenceService.verifyRecurrence(id, json.get("verificationStatus").getAsString(), session.userId);
                    sendJson(exchange, 200, true, "Recurrence verified", null);
                    return;
                }

                sendError(exchange, 404, "Endpoint not found: " + path);

            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                LOGGER.warning("API Handler error: " + e.getMessage());
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) path = "/index.html";

            // Map to webapp or uploads directory
            Path filePath = null;
            if (path.startsWith("/uploads/")) {
                filePath = Paths.get("uploads", path.substring("/uploads/".length()));
            } else {
                String sub = path.startsWith("/") ? path.substring(1) : path;
                // Check possible webapp roots
                Path[] candidates = new Path[]{
                    Paths.get("src", "main", "webapp", sub),
                    Paths.get("target", "war-staging", sub),
                    Paths.get(sub)
                };
                for (Path candidate : candidates) {
                    if (Files.exists(candidate) && !Files.isDirectory(candidate)) {
                        filePath = candidate;
                        break;
                    }
                }
            }

            if (filePath == null || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                String notFound = "<h1>404 Not Found</h1><p>Resource not found: " + path + "</p>";
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, notFound.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(notFound.getBytes(StandardCharsets.UTF_8)); }
                return;
            }

            String mime = getMimeType(filePath.toString());
            exchange.getResponseHeaders().set("Content-Type", mime);

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        return "application/octet-stream";
    }

    private static JsonObject parseJson(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return JsonUtil.fromJson(reader, JsonObject.class);
        }
    }

    private static Complaint parseComplaintBody(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        Complaint c = new Complaint();

        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            // Read body
            byte[] body = exchange.getRequestBody().readAllBytes();
            String boundary = contentType.substring(contentType.indexOf("boundary=") + 9).trim();
            if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                boundary = boundary.substring(1, boundary.length() - 1);
            }

            String content = new String(body, StandardCharsets.ISO_8859_1);
            String[] parts = content.split("--" + boundary);

            for (String part : parts) {
                if (part.contains("name=\"title\"")) c.setTitle(extractField(part));
                else if (part.contains("name=\"description\"")) c.setDescription(extractField(part));
                else if (part.contains("name=\"area\"")) c.setArea(extractField(part));
                else if (part.contains("name=\"city\"")) c.setCity(extractField(part));
                else if (part.contains("name=\"state\"")) c.setState(extractField(part));
                else if (part.contains("name=\"landmark\"")) c.setLandmark(extractField(part));
                else if (part.contains("name=\"categoryId\"")) {
                    String v = extractField(part);
                    if (!v.isEmpty()) c.setCategoryId(Integer.parseInt(v));
                } else if (part.contains("name=\"latitude\"")) {
                    String v = extractField(part);
                    if (!v.isEmpty()) c.setLatitude(new BigDecimal(v));
                } else if (part.contains("name=\"longitude\"")) {
                    String v = extractField(part);
                    if (!v.isEmpty()) c.setLongitude(new BigDecimal(v));
                } else if (part.contains("name=\"image\"") && part.contains("filename=\"")) {
                    // Extract image file
                    String fn = "civic_" + System.currentTimeMillis() + ".jpg";
                    int headerEnd = part.indexOf("\r\n\r\n");
                    if (headerEnd > 0) {
                        int fileStart = headerEnd + 4;
                        int fileEnd = part.lastIndexOf("\r\n");
                        if (fileEnd > fileStart) {
                            byte[] fileBytes = part.substring(fileStart, fileEnd).getBytes(StandardCharsets.ISO_8859_1);
                            if (fileBytes.length > 50) {
                                Files.createDirectories(Paths.get("uploads"));
                                Files.write(Paths.get("uploads", fn), fileBytes);
                                c.setImagePath(fn);
                            }
                        }
                    }
                }
            }
        } else {
            JsonObject json = parseJson(exchange);
            if (json != null) {
                c.setTitle(json.has("title") ? json.get("title").getAsString() : "");
                c.setDescription(json.has("description") ? json.get("description").getAsString() : "");
                c.setArea(json.has("area") ? json.get("area").getAsString() : "");
                c.setCity(json.has("city") ? json.get("city").getAsString() : "");
                c.setState(json.has("state") ? json.get("state").getAsString() : "");
                c.setLandmark(json.has("landmark") ? json.get("landmark").getAsString() : "");
                if (json.has("categoryId")) c.setCategoryId(json.get("categoryId").getAsInt());
                if (json.has("latitude") && !json.get("latitude").isJsonNull()) c.setLatitude(new BigDecimal(json.get("latitude").getAsString()));
                if (json.has("longitude") && !json.get("longitude").isJsonNull()) c.setLongitude(new BigDecimal(json.get("longitude").getAsString()));
            }
        }

        return c;
    }

    private static String extractField(String part) {
        int idx = part.indexOf("\r\n\r\n");
        if (idx == -1) return "";
        String val = part.substring(idx + 4);
        int end = val.indexOf("\r\n");
        if (end != -1) val = val.substring(0, end);
        try {
            return new String(val.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return val.trim();
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                try {
                    map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()),
                            URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static void sendJson(HttpExchange exchange, int status, boolean success, String msg, Object data) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("message", msg);
        if (data != null) resp.put("data", data);

        String json = JsonUtil.toJson(resp);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int status, String error) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("error", error);

        String json = JsonUtil.toJson(resp);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

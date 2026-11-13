package com.civicsphere.controller;

import com.civicsphere.config.AppConfig;
import com.civicsphere.model.Complaint;
import com.civicsphere.model.ComplaintHistory;
import com.civicsphere.model.RecurrenceRecord;
import com.civicsphere.service.ComplaintService;
import com.civicsphere.service.RecurrenceService;
import com.civicsphere.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller managing citizen complaint lifecycle, submission, photo uploads,
 * timeline tracking, and resolution verification.
 */
@WebServlet(name = "ComplaintServlet", urlPatterns = {"/api/complaints/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 10 * 1024 * 1024,        // 10 MB
        maxRequestSize = 20 * 1024 * 1024      // 20 MB
)
public class ComplaintServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ComplaintServlet.class.getName());

    private ComplaintService complaintService;
    private RecurrenceService recurrenceService;

    @Override
    public void init() throws ServletException {
        this.complaintService = new ComplaintService();
        this.recurrenceService = new RecurrenceService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        HttpSession session = req.getSession(false);
        Integer currentUserId = (session != null) ? (Integer) session.getAttribute("USER_ID") : null;

        if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
            // GET /api/complaints
            handleGetAll(req, resp);
        } else if ("/my".equals(pathInfo)) {
            // GET /api/complaints/my
            if (currentUserId == null) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "Login required");
                return;
            }
            List<Complaint> list = complaintService.getComplaintsByUser(currentUserId);
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Complaints fetched", list);
        } else {
            // GET /api/complaints/{id}
            try {
                int complaintId = Integer.parseInt(pathInfo.substring(1));
                Complaint complaint = complaintService.getComplaintById(complaintId);
                if (complaint == null) {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Complaint not found");
                    return;
                }

                List<ComplaintHistory> history = complaintService.getComplaintHistory(complaintId);
                RecurrenceRecord recurrence = recurrenceService.getAllRecurrenceRecords(null).stream()
                        .filter(r -> r.getRecurringComplaintId() == complaintId || r.getOriginalComplaintId() == complaintId)
                        .findFirst().orElse(null);

                Map<String, Object> data = new HashMap<>();
                data.put("complaint", complaint);
                data.put("history", history);
                data.put("recurrence", recurrence);

                JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Complaint details retrieved", data);
            } catch (NumberFormatException e) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid complaint ID format");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        HttpSession session = req.getSession(false);
        Integer currentUserId = (session != null) ? (Integer) session.getAttribute("USER_ID") : null;

        if (currentUserId == null) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to submit a complaint.");
            return;
        }

        if (pathInfo != null && pathInfo.matches("/\\d+/recurrence")) {
            // POST /api/complaints/{id}/recurrence
            handleReportRecurrence(req, resp, currentUserId, pathInfo);
        } else if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
            // POST /api/complaints
            handleSubmitComplaint(req, resp, currentUserId);
        } else {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        HttpSession session = req.getSession(false);
        Integer currentUserId = (session != null) ? (Integer) session.getAttribute("USER_ID") : null;
        String role = (session != null) ? (String) session.getAttribute("USER_ROLE") : null;

        if (currentUserId == null || !"ADMIN".equalsIgnoreCase(role)) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "Administrator access required.");
            return;
        }

        if (pathInfo != null && pathInfo.matches("/\\d+/status")) {
            // PUT /api/complaints/{id}/status
            handleUpdateStatus(req, resp, currentUserId, pathInfo);
        } else if (pathInfo != null && pathInfo.matches("/\\d+/resolve")) {
            // PUT /api/complaints/{id}/resolve
            handleResolve(req, resp, currentUserId, pathInfo);
        } else {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleGetAll(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String status = req.getParameter("status");
        String categoryIdStr = req.getParameter("categoryId");
        String search = req.getParameter("search");
        String city = req.getParameter("city");
        String area = req.getParameter("area");

        Integer categoryId = null;
        if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
            try {
                categoryId = Integer.parseInt(categoryIdStr);
            } catch (NumberFormatException ignored) {}
        }

        List<Complaint> list = complaintService.getAllComplaints(status, categoryId, search, city, area);
        JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Complaints list", list);
    }

    private void handleSubmitComplaint(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        try {
            boolean isMultipart = req.getContentType() != null && req.getContentType().toLowerCase().startsWith("multipart/");

            Complaint c = new Complaint();
            String savedImagePath = null;

            if (isMultipart) {
                c.setTitle(req.getParameter("title"));
                c.setDescription(req.getParameter("description"));
                c.setArea(req.getParameter("area"));
                c.setCity(req.getParameter("city"));
                c.setState(req.getParameter("state"));
                c.setLandmark(req.getParameter("landmark"));

                String catIdStr = req.getParameter("categoryId");
                if (catIdStr != null && !catIdStr.isEmpty()) {
                    c.setCategoryId(Integer.parseInt(catIdStr));
                }

                String latStr = req.getParameter("latitude");
                if (latStr != null && !latStr.isEmpty()) {
                    c.setLatitude(new BigDecimal(latStr));
                }

                String lonStr = req.getParameter("longitude");
                if (lonStr != null && !lonStr.isEmpty()) {
                    c.setLongitude(new BigDecimal(lonStr));
                }

                Part imagePart = req.getPart("image");
                if (imagePart != null && imagePart.getSize() > 0) {
                    savedImagePath = processUploadedFile(imagePart, req.getServletContext().getRealPath(""));
                    c.setImagePath(savedImagePath);
                }
            } else {
                JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
                if (json == null) {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request payload");
                    return;
                }
                c.setTitle(json.has("title") ? json.get("title").getAsString() : null);
                c.setDescription(json.has("description") ? json.get("description").getAsString() : null);
                c.setArea(json.has("area") ? json.get("area").getAsString() : null);
                c.setCity(json.has("city") ? json.get("city").getAsString() : null);
                c.setState(json.has("state") ? json.get("state").getAsString() : null);
                c.setLandmark(json.has("landmark") ? json.get("landmark").getAsString() : null);
                if (json.has("categoryId")) c.setCategoryId(json.get("categoryId").getAsInt());
                if (json.has("latitude") && !json.get("latitude").isJsonNull()) c.setLatitude(new BigDecimal(json.get("latitude").getAsString()));
                if (json.has("longitude") && !json.get("longitude").isJsonNull()) c.setLongitude(new BigDecimal(json.get("longitude").getAsString()));
                if (json.has("imagePath") && !json.get("imagePath").isJsonNull()) c.setImagePath(json.get("imagePath").getAsString());
            }

            Complaint saved = complaintService.submitComplaint(c, userId);
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_CREATED, "Complaint submitted successfully!", saved);

        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error submitting complaint", e);
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Submission failed: " + e.getMessage());
        }
    }

    private void handleReportRecurrence(HttpServletRequest req, HttpServletResponse resp, int userId, String pathInfo) throws IOException {
        try {
            int originalComplaintId = Integer.parseInt(pathInfo.split("/")[1]);

            boolean isMultipart = req.getContentType() != null && req.getContentType().toLowerCase().startsWith("multipart/");
            Complaint newComplaint = new Complaint();
            newComplaint.setUserId(userId);

            if (isMultipart) {
                newComplaint.setTitle(req.getParameter("title"));
                newComplaint.setDescription(req.getParameter("description"));
                newComplaint.setArea(req.getParameter("area"));
                newComplaint.setCity(req.getParameter("city"));
                newComplaint.setState(req.getParameter("state"));
                newComplaint.setLandmark(req.getParameter("landmark"));

                String catStr = req.getParameter("categoryId");
                if (catStr != null && !catStr.isEmpty()) newComplaint.setCategoryId(Integer.parseInt(catStr));

                Part imagePart = req.getPart("image");
                if (imagePart != null && imagePart.getSize() > 0) {
                    newComplaint.setImagePath(processUploadedFile(imagePart, req.getServletContext().getRealPath("")));
                }
            } else {
                JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
                if (json != null) {
                    newComplaint.setTitle(json.has("title") ? json.get("title").getAsString() : null);
                    newComplaint.setDescription(json.has("description") ? json.get("description").getAsString() : null);
                    newComplaint.setArea(json.has("area") ? json.get("area").getAsString() : null);
                    newComplaint.setCity(json.has("city") ? json.get("city").getAsString() : null);
                    newComplaint.setState(json.has("state") ? json.get("state").getAsString() : null);
                    newComplaint.setLandmark(json.has("landmark") ? json.get("landmark").getAsString() : null);
                    if (json.has("categoryId")) newComplaint.setCategoryId(json.get("categoryId").getAsInt());
                }
            }

            RecurrenceRecord record = recurrenceService.reportCitizenRecurrence(originalComplaintId, newComplaint);
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_CREATED, "Recurring issue reported and linked successfully!", record);

        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reporting recurrence", e);
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Recurrence submission failed: " + e.getMessage());
        }
    }

    private void handleUpdateStatus(HttpServletRequest req, HttpServletResponse resp, int adminId, String pathInfo) throws IOException {
        try {
            int complaintId = Integer.parseInt(pathInfo.split("/")[1]);
            JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
            if (json == null || !json.has("status")) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing status parameter");
                return;
            }

            String newStatus = json.get("status").getAsString();
            Integer deptId = json.has("departmentId") && !json.get("departmentId").isJsonNull() ? json.get("departmentId").getAsInt() : null;
            String remarks = json.has("remarks") && !json.get("remarks").isJsonNull() ? json.get("remarks").getAsString() : "";

            boolean success = complaintService.updateStatus(complaintId, newStatus, deptId, remarks, adminId);
            if (success) {
                JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Status updated successfully", null);
            } else {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update status");
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating status", e);
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void handleResolve(HttpServletRequest req, HttpServletResponse resp, int adminId, String pathInfo) throws IOException {
        try {
            int complaintId = Integer.parseInt(pathInfo.split("/")[1]);
            JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
            if (json == null || !json.has("remarks")) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Resolution remarks are required");
                return;
            }

            String remarks = json.get("remarks").getAsString();
            String resolutionImage = json.has("resolutionImagePath") && !json.get("resolutionImagePath").isJsonNull()
                    ? json.get("resolutionImagePath").getAsString() : null;

            boolean success = complaintService.resolveComplaint(complaintId, remarks, resolutionImage, adminId);
            if (success) {
                JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Complaint marked as resolved!", null);
            } else {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to resolve complaint");
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resolving complaint", e);
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String processUploadedFile(Part part, String appRealPath) throws IOException {
        String submittedFilename = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        String ext = "";
        int i = submittedFilename.lastIndexOf('.');
        if (i > 0) {
            ext = submittedFilename.substring(i).toLowerCase();
        }

        // Validate image extensions
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
            throw new IllegalArgumentException("Only JPG, PNG, and WebP images are allowed.");
        }

        String safeFileName = "civic_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        // Save in application's uploads directory or configured upload dir
        String uploadDirConfig = AppConfig.getUploadDir();
        Path uploadPath;
        if (Paths.get(uploadDirConfig).isAbsolute()) {
            uploadPath = Paths.get(uploadDirConfig);
        } else {
            uploadPath = Paths.get(appRealPath != null ? appRealPath : ".", uploadDirConfig);
        }

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path target = uploadPath.resolve(safeFileName);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return safeFileName;
    }
}


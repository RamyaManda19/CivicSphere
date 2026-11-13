package com.civicsphere.controller;

import com.civicsphere.model.Complaint;
import com.civicsphere.service.AdminService;
import com.civicsphere.service.ComplaintService;
import com.civicsphere.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller providing administrative actions and analytics.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/api/admin/*"})
public class AdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminServlet.class.getName());

    private AdminService adminService;
    private ComplaintService complaintService;

    @Override
    public void init() throws ServletException {
        this.adminService = new AdminService();
        this.complaintService = new ComplaintService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        HttpSession session = req.getSession(false);
        Integer adminId = (session != null) ? (Integer) session.getAttribute("USER_ID") : null;
        String role = (session != null) ? (String) session.getAttribute("USER_ROLE") : null;

        if (adminId == null || !"ADMIN".equalsIgnoreCase(role)) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required.");
            return;
        }

        if ("/dashboard".equals(pathInfo)) {
            Map<String, Object> stats = adminService.getAdminDashboardStats();
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Dashboard statistics", stats);
        } else if ("/complaints".equals(pathInfo) || pathInfo == null || "/".equals(pathInfo)) {
            String status = req.getParameter("status");
            String catStr = req.getParameter("categoryId");
            String search = req.getParameter("search");
            String city = req.getParameter("city");
            String area = req.getParameter("area");

            Integer categoryId = null;
            if (catStr != null && !catStr.isEmpty()) {
                try { categoryId = Integer.parseInt(catStr); } catch (NumberFormatException ignored) {}
            }

            List<Complaint> list = complaintService.getAllComplaints(status, categoryId, search, city, area);
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Admin complaints fetched", list);
        } else {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        HttpSession session = req.getSession(false);
        Integer adminId = (session != null) ? (Integer) session.getAttribute("USER_ID") : null;
        String role = (session != null) ? (String) session.getAttribute("USER_ROLE") : null;

        if (adminId == null || !"ADMIN".equalsIgnoreCase(role)) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required.");
            return;
        }

        if (pathInfo != null && pathInfo.matches("/complaints/\\d+/assign")) {
            try {
                String[] parts = pathInfo.split("/");
                int complaintId = Integer.parseInt(parts[2]);

                JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
                if (json == null || !json.has("departmentId")) {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "departmentId is required");
                    return;
                }

                int departmentId = json.get("departmentId").getAsInt();
                String remarks = json.has("remarks") && !json.get("remarks").isJsonNull() ? json.get("remarks").getAsString() : "";

                boolean assigned = complaintService.assignDepartment(complaintId, departmentId, remarks, adminId);
                if (assigned) {
                    JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Department assigned successfully", null);
                } else {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to assign department");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error assigning department", e);
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        } else {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }
}


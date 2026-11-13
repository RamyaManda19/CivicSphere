package com.civicsphere.controller;

import com.civicsphere.model.Department;
import com.civicsphere.service.DepartmentService;
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

/**
 * Controller managing department records and configurations.
 */
@WebServlet(name = "DepartmentServlet", urlPatterns = {"/api/departments/*"})
public class DepartmentServlet extends HttpServlet {

    private DepartmentService departmentService;

    @Override
    public void init() throws ServletException {
        this.departmentService = new DepartmentService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
            List<Department> list = departmentService.getAllDepartments();
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Departments fetched", list);
        } else {
            try {
                int id = Integer.parseInt(pathInfo.substring(1));
                Department dept = departmentService.getDepartmentById(id);
                if (dept == null) {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Department not found");
                } else {
                    JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Department found", dept);
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID format");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAdmin(req)) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required.");
            return;
        }

        try {
            JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
            if (json == null || !json.has("departmentName")) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "departmentName is required");
                return;
            }

            String name = json.get("departmentName").getAsString();
            String desc = json.has("description") && !json.get("description").isJsonNull() ? json.get("description").getAsString() : "";
            String email = json.has("contactEmail") && !json.get("contactEmail").isJsonNull() ? json.get("contactEmail").getAsString() : "";

            Department created = departmentService.createDepartment(name, desc, email);
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_CREATED, "Department created successfully", created);

        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAdmin(req)) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing department ID");
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
            if (json == null || !json.has("departmentName")) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "departmentName is required");
                return;
            }

            String name = json.get("departmentName").getAsString();
            String desc = json.has("description") && !json.get("description").isJsonNull() ? json.get("description").getAsString() : "";
            String email = json.has("contactEmail") && !json.get("contactEmail").isJsonNull() ? json.get("contactEmail").getAsString() : "";

            boolean updated = departmentService.updateDepartment(id, name, desc, email);
            if (updated) {
                JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Department updated successfully", null);
            } else {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Department not found or update failed");
            }
        } catch (Exception e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAdmin(req)) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing department ID");
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            boolean deleted = departmentService.deleteDepartment(id);
            if (deleted) {
                JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Department deleted successfully", null);
            } else {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Failed to delete department (it may be linked to active complaints).");
            }
        } catch (Exception e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && "ADMIN".equalsIgnoreCase((String) session.getAttribute("USER_ROLE"));
    }
}


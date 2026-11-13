package com.civicsphere.controller;

import com.civicsphere.model.RecurrenceRecord;
import com.civicsphere.service.RecurrenceService;
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
 * Controller managing recurring civic issue records, similarity evaluations,
 * and administrator confirmation/rejection workflows.
 */
@WebServlet(name = "RecurrenceServlet", urlPatterns = {"/api/recurrence/*"})
public class RecurrenceServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RecurrenceServlet.class.getName());

    private RecurrenceService recurrenceService;

    @Override
    public void init() throws ServletException {
        this.recurrenceService = new RecurrenceService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
            // GET /api/recurrence?status=...
            String status = req.getParameter("status");
            List<RecurrenceRecord> list = recurrenceService.getAllRecurrenceRecords(status);
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Recurrence records fetched", list);
        } else if ("/stats".equals(pathInfo)) {
            // GET /api/recurrence/stats
            Map<String, Integer> stats = recurrenceService.getRecurrenceStatistics();
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Recurrence statistics", stats);
        } else {
            // GET /api/recurrence/{id}
            try {
                int id = Integer.parseInt(pathInfo.substring(1));
                RecurrenceRecord record = recurrenceService.getRecurrenceById(id);
                if (record == null) {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Recurrence record not found");
                } else {
                    JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Recurrence details", record);
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID format");
            }
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

        if (pathInfo != null && pathInfo.matches("/\\d+/verify")) {
            // PUT /api/recurrence/{id}/verify
            try {
                int recurrenceId = Integer.parseInt(pathInfo.split("/")[1]);
                JsonObject json = JsonUtil.fromJson(req.getReader(), JsonObject.class);
                if (json == null || !json.has("verificationStatus")) {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "verificationStatus is required");
                    return;
                }

                String newStatus = json.get("verificationStatus").getAsString();
                boolean verified = recurrenceService.verifyRecurrence(recurrenceId, newStatus, adminId);
                if (verified) {
                    JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Recurrence status updated to " + newStatus, null);
                } else {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update recurrence status");
                }
            } catch (IllegalArgumentException e) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error verifying recurrence", e);
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }
}


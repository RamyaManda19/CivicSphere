package com.civicsphere.service;

import com.civicsphere.dao.ComplaintDAO;
import com.civicsphere.dao.ComplaintHistoryDAO;
import com.civicsphere.model.Complaint;
import com.civicsphere.model.ComplaintHistory;
import com.civicsphere.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing complaint submissions, status transitions, department assignment,
 * and resolution workflows with transactional history tracking.
 */
public class ComplaintService {

    private static final Logger LOGGER = Logger.getLogger(ComplaintService.class.getName());

    private final ComplaintDAO complaintDAO;
    private final ComplaintHistoryDAO historyDAO;
    private final RecurrenceService recurrenceService;

    public ComplaintService() {
        this.complaintDAO = new ComplaintDAO();
        this.historyDAO = new ComplaintHistoryDAO();
        this.recurrenceService = new RecurrenceService(complaintDAO, new com.civicsphere.dao.RecurrenceDAO());
    }

    public ComplaintService(ComplaintDAO complaintDAO, ComplaintHistoryDAO historyDAO, RecurrenceService recurrenceService) {
        this.complaintDAO = complaintDAO;
        this.historyDAO = historyDAO;
        this.recurrenceService = recurrenceService;
    }

    /**
     * Submits a new citizen complaint and triggers automatic recurrence evaluation.
     */
    public Complaint submitComplaint(Complaint c, int userId) throws Exception {
        if (c.getTitle() == null || c.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Complaint title is required.");
        }
        if (c.getDescription() == null || c.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Complaint description is required.");
        }
        if (c.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Valid category is required.");
        }
        if (c.getArea() == null || c.getArea().trim().isEmpty()) {
            throw new IllegalArgumentException("Area is required.");
        }
        if (c.getCity() == null || c.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required.");
        }
        if (c.getState() == null || c.getState().trim().isEmpty()) {
            throw new IllegalArgumentException("State is required.");
        }

        c.setUserId(userId);
        c.setStatus("SUBMITTED");
        c.setSubmittedAt(new Timestamp(System.currentTimeMillis()));

        int complaintId = complaintDAO.createComplaint(c);
        if (complaintId <= 0) {
            throw new RuntimeException("Failed to save complaint to database.");
        }
        c.setComplaintId(complaintId);

        // Record initial status in history
        ComplaintHistory initialHistory = new ComplaintHistory();
        initialHistory.setComplaintId(complaintId);
        initialHistory.setPreviousStatus(null);
        initialHistory.setNewStatus("SUBMITTED");
        initialHistory.setRemarks("Complaint submitted by citizen.");
        initialHistory.setUpdatedBy(userId);
        initialHistory.setUpdatedAt(c.getSubmittedAt());
        historyDAO.addHistory(initialHistory, null);

        // Run Recurrence Detection Engine
        try {
            recurrenceService.evaluateRecurrence(c);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error running recurrence engine for complaint #" + complaintId, e);
        }

        return c;
    }

    public Complaint getComplaintById(int complaintId) {
        return complaintDAO.findById(complaintId);
    }

    public List<Complaint> getComplaintsByUser(int userId) {
        return complaintDAO.findByUserId(userId);
    }

    public List<Complaint> getAllComplaints(String status, Integer categoryId, String search, String city, String area) {
        return complaintDAO.findAll(status, categoryId, search, city, area);
    }

    public List<ComplaintHistory> getComplaintHistory(int complaintId) {
        return historyDAO.getHistoryByComplaintId(complaintId);
    }

    /**
     * Updates complaint status with an audit remark inside a database transaction.
     */
    public boolean updateStatus(int complaintId, String newStatus, Integer departmentId, String remarks, int updatedBy) throws Exception {
        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null) {
            throw new IllegalArgumentException("Complaint #" + complaintId + " not found.");
        }

        String previousStatus = complaint.getStatus();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            boolean updated = complaintDAO.updateStatus(complaintId, newStatus, departmentId, conn);
            if (!updated) {
                conn.rollback();
                return false;
            }

            ComplaintHistory h = new ComplaintHistory();
            h.setComplaintId(complaintId);
            h.setPreviousStatus(previousStatus);
            h.setNewStatus(newStatus);
            h.setRemarks(remarks != null && !remarks.trim().isEmpty() ? remarks.trim() : "Status updated to " + newStatus);
            h.setUpdatedBy(updatedBy);
            h.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            historyDAO.addHistory(h, conn);

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            LOGGER.log(Level.SEVERE, "Failed to update complaint status", e);
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Assigns a department to a complaint with audit history.
     */
    public boolean assignDepartment(int complaintId, int departmentId, String remarks, int updatedBy) throws Exception {
        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null) {
            throw new IllegalArgumentException("Complaint #" + complaintId + " not found.");
        }

        String previousStatus = complaint.getStatus();
        String newStatus = "SUBMITTED".equals(previousStatus) ? "ASSIGNED" : previousStatus;

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            boolean assigned = complaintDAO.assignDepartment(complaintId, departmentId, conn);
            if (!assigned) {
                conn.rollback();
                return false;
            }

            ComplaintHistory h = new ComplaintHistory();
            h.setComplaintId(complaintId);
            h.setPreviousStatus(previousStatus);
            h.setNewStatus(newStatus);
            h.setRemarks(remarks != null && !remarks.trim().isEmpty() ? remarks.trim() : "Assigned to department #" + departmentId);
            h.setUpdatedBy(updatedBy);
            h.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            historyDAO.addHistory(h, conn);

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            LOGGER.log(Level.SEVERE, "Failed to assign department", e);
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Marks a complaint as resolved with remarks, evidence image, and resolution date.
     */
    public boolean resolveComplaint(int complaintId, String remarks, String resolutionImagePath, int updatedBy) throws Exception {
        if (remarks == null || remarks.trim().isEmpty()) {
            throw new IllegalArgumentException("Resolution remarks are required.");
        }

        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null) {
            throw new IllegalArgumentException("Complaint #" + complaintId + " not found.");
        }

        String previousStatus = complaint.getStatus();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            boolean resolved = complaintDAO.resolveComplaint(complaintId, remarks.trim(), resolutionImagePath, conn);
            if (!resolved) {
                conn.rollback();
                return false;
            }

            ComplaintHistory h = new ComplaintHistory();
            h.setComplaintId(complaintId);
            h.setPreviousStatus(previousStatus);
            h.setNewStatus("RESOLVED");
            h.setRemarks("Resolution verified: " + remarks.trim());
            h.setUpdatedBy(updatedBy);
            h.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            historyDAO.addHistory(h, conn);

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            LOGGER.log(Level.SEVERE, "Failed to resolve complaint", e);
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}


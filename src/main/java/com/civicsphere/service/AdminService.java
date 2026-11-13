package com.civicsphere.service;

import com.civicsphere.dao.ComplaintDAO;
import com.civicsphere.dao.RecurrenceDAO;
import com.civicsphere.model.Complaint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service providing administrative analytics, dashboard aggregation, and metrics.
 */
public class AdminService {

    private final ComplaintDAO complaintDAO;
    private final RecurrenceDAO recurrenceDAO;

    public AdminService() {
        this.complaintDAO = new ComplaintDAO();
        this.recurrenceDAO = new RecurrenceDAO();
    }

    public AdminService(ComplaintDAO complaintDAO, RecurrenceDAO recurrenceDAO) {
        this.complaintDAO = complaintDAO;
        this.recurrenceDAO = recurrenceDAO;
    }

    /**
     * Gathers all statistics and metrics needed for the Admin Dashboard.
     */
    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        Map<String, Integer> statusCounts = complaintDAO.getStatusCounts(null);
        Map<String, Integer> recurrenceStats = recurrenceDAO.getRecurrenceStats();
        Map<String, Integer> categoryCounts = complaintDAO.getCountsByCategory();
        Map<String, Integer> departmentCounts = complaintDAO.getCountsByDepartment();
        List<Complaint> recentActivity = complaintDAO.getRecentComplaints(null, 8);

        stats.put("statusCounts", statusCounts);
        stats.put("recurrenceStats", recurrenceStats);
        stats.put("categoryCounts", categoryCounts);
        stats.put("departmentCounts", departmentCounts);
        stats.put("recentActivity", recentActivity);

        return stats;
    }

    /**
     * Gathers statistics for the Citizen Dashboard.
     */
    public Map<String, Object> getCitizenDashboardStats(int userId) {
        Map<String, Object> stats = new HashMap<>();

        Map<String, Integer> statusCounts = complaintDAO.getStatusCounts(userId);
        List<Complaint> recentComplaints = complaintDAO.getRecentComplaints(userId, 5);

        stats.put("statusCounts", statusCounts);
        stats.put("recentComplaints", recentComplaints);

        return stats;
    }
}


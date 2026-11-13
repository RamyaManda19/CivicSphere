package com.civicsphere.dao;

import com.civicsphere.model.ComplaintHistory;
import com.civicsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for complaint_history table.
 */
public class ComplaintHistoryDAO {

    private static final Logger LOGGER = Logger.getLogger(ComplaintHistoryDAO.class.getName());

    public boolean addHistory(ComplaintHistory history, Connection conn) throws SQLException {
        String sql = "INSERT INTO complaint_history (complaint_id, previous_status, new_status, remarks, updated_by, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        boolean closeConn = false;
        if (conn == null) {
            conn = DBConnection.getConnection();
            closeConn = true;
        }

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, history.getComplaintId());
            ps.setString(2, history.getPreviousStatus());
            ps.setString(3, history.getNewStatus());
            ps.setString(4, history.getRemarks());
            ps.setInt(5, history.getUpdatedBy());
            Timestamp now = history.getUpdatedAt() != null ? history.getUpdatedAt() : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(6, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        history.setHistoryId(rs.getInt(1));
                    }
                }
                return true;
            }
        } finally {
            if (closeConn) {
                DBConnection.close(conn);
            }
        }
        return false;
    }

    public List<ComplaintHistory> getHistoryByComplaintId(int complaintId) {
        List<ComplaintHistory> list = new ArrayList<>();
        String sql = "SELECT h.*, u.full_name AS updated_by_name, u.role AS updated_by_role " +
                     "FROM complaint_history h " +
                     "LEFT JOIN users u ON h.updated_by = u.user_id " +
                     "WHERE h.complaint_id = ? " +
                     "ORDER BY h.updated_at ASC, h.history_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComplaintHistory h = new ComplaintHistory();
                    h.setHistoryId(rs.getInt("history_id"));
                    h.setComplaintId(rs.getInt("complaint_id"));
                    h.setPreviousStatus(rs.getString("previous_status"));
                    h.setNewStatus(rs.getString("new_status"));
                    h.setRemarks(rs.getString("remarks"));
                    h.setUpdatedBy(rs.getInt("updated_by"));
                    h.setUpdatedByName(rs.getString("updated_by_name"));
                    h.setUpdatedByRole(rs.getString("updated_by_role"));
                    h.setUpdatedAt(rs.getTimestamp("updated_at"));
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching history for complaint: " + complaintId, e);
        }
        return list;
    }
}


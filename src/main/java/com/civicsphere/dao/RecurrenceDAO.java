package com.civicsphere.dao;

import com.civicsphere.model.Complaint;
import com.civicsphere.model.RecurrenceRecord;
import com.civicsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for recurrence_records table.
 */
public class RecurrenceDAO {

    private static final Logger LOGGER = Logger.getLogger(RecurrenceDAO.class.getName());

    public boolean createRecurrenceRecord(RecurrenceRecord r) {
        String sql = "INSERT INTO recurrence_records " +
                     "(original_complaint_id, recurring_complaint_id, matching_score, matching_reason, verification_status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, r.getOriginalComplaintId());
            ps.setInt(2, r.getRecurringComplaintId());
            ps.setBigDecimal(3, r.getMatchingScore());
            ps.setString(4, r.getMatchingReason());
            ps.setString(5, r.getVerificationStatus() != null ? r.getVerificationStatus() : "PENDING");
            ps.setTimestamp(6, r.getCreatedAt() != null ? r.getCreatedAt() : new Timestamp(System.currentTimeMillis()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        r.setRecurrenceId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            LOGGER.info("Recurrence pair already exists: " + r.getOriginalComplaintId() + " -> " + r.getRecurringComplaintId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating recurrence record", e);
        }
        return false;
    }

    public boolean exists(int originalComplaintId, int recurringComplaintId) {
        String sql = "SELECT COUNT(*) FROM recurrence_records WHERE original_complaint_id = ? AND recurring_complaint_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, originalComplaintId);
            ps.setInt(2, recurringComplaintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking recurrence existence", e);
        }
        return false;
    }

    public List<RecurrenceRecord> findAll(String verificationStatus) {
        List<RecurrenceRecord> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.*, u.full_name AS verified_by_name, " +
                "oc.title AS orig_title, oc.area AS orig_area, oc.city AS orig_city, oc.status AS orig_status, oc.resolved_at AS orig_resolved_at, oc.category_id AS orig_cat_id, " +
                "ocat.category_name AS orig_cat_name, " +
                "rc.title AS rec_title, rc.area AS rec_area, rc.city AS rec_city, rc.status AS rec_status, rc.submitted_at AS rec_submitted_at, rc.category_id AS rec_cat_id, " +
                "rcat.category_name AS rec_cat_name, " +
                "ru.full_name AS rec_user_name " +
                "FROM recurrence_records r " +
                "LEFT JOIN users u ON r.verified_by = u.user_id " +
                "LEFT JOIN complaints oc ON r.original_complaint_id = oc.complaint_id " +
                "LEFT JOIN categories ocat ON oc.category_id = ocat.category_id " +
                "LEFT JOIN complaints rc ON r.recurring_complaint_id = rc.complaint_id " +
                "LEFT JOIN categories rcat ON rc.category_id = rcat.category_id " +
                "LEFT JOIN users ru ON rc.user_id = ru.user_id " +
                "WHERE 1=1 "
        );

        if (verificationStatus != null && !verificationStatus.trim().isEmpty() && !verificationStatus.equalsIgnoreCase("ALL")) {
            sql.append(" AND r.verification_status = ?");
        }
        sql.append(" ORDER BY r.created_at DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (verificationStatus != null && !verificationStatus.trim().isEmpty() && !verificationStatus.equalsIgnoreCase("ALL")) {
                ps.setString(1, verificationStatus.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithComplaints(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching recurrence records", e);
        }
        return list;
    }

    public RecurrenceRecord findById(int recurrenceId) {
        String sql = "SELECT r.*, u.full_name AS verified_by_name, " +
                "oc.title AS orig_title, oc.area AS orig_area, oc.city AS orig_city, oc.status AS orig_status, oc.resolved_at AS orig_resolved_at, oc.category_id AS orig_cat_id, " +
                "ocat.category_name AS orig_cat_name, " +
                "rc.title AS rec_title, rc.area AS rec_area, rc.city AS rec_city, rc.status AS rec_status, rc.submitted_at AS rec_submitted_at, rc.category_id AS rec_cat_id, " +
                "rcat.category_name AS rec_cat_name, " +
                "ru.full_name AS rec_user_name " +
                "FROM recurrence_records r " +
                "LEFT JOIN users u ON r.verified_by = u.user_id " +
                "LEFT JOIN complaints oc ON r.original_complaint_id = oc.complaint_id " +
                "LEFT JOIN categories ocat ON oc.category_id = ocat.category_id " +
                "LEFT JOIN complaints rc ON r.recurring_complaint_id = rc.complaint_id " +
                "LEFT JOIN categories rcat ON rc.category_id = rcat.category_id " +
                "LEFT JOIN users ru ON rc.user_id = ru.user_id " +
                "WHERE r.recurrence_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recurrenceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowWithComplaints(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding recurrence record by id: " + recurrenceId, e);
        }
        return null;
    }

    public RecurrenceRecord findByRecurringComplaintId(int recurringComplaintId) {
        String sql = "SELECT r.*, u.full_name AS verified_by_name, " +
                "oc.title AS orig_title, oc.area AS orig_area, oc.city AS orig_city, oc.status AS orig_status, oc.resolved_at AS orig_resolved_at, oc.category_id AS orig_cat_id, " +
                "ocat.category_name AS orig_cat_name, " +
                "rc.title AS rec_title, rc.area AS rec_area, rc.city AS rec_city, rc.status AS rec_status, rc.submitted_at AS rec_submitted_at, rc.category_id AS rec_cat_id, " +
                "rcat.category_name AS rec_cat_name, " +
                "ru.full_name AS rec_user_name " +
                "FROM recurrence_records r " +
                "LEFT JOIN users u ON r.verified_by = u.user_id " +
                "LEFT JOIN complaints oc ON r.original_complaint_id = oc.complaint_id " +
                "LEFT JOIN categories ocat ON oc.category_id = ocat.category_id " +
                "LEFT JOIN complaints rc ON r.recurring_complaint_id = rc.complaint_id " +
                "LEFT JOIN categories rcat ON rc.category_id = rcat.category_id " +
                "LEFT JOIN users ru ON rc.user_id = ru.user_id " +
                "WHERE r.recurring_complaint_id = ? " +
                "ORDER BY r.matching_score DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recurringComplaintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowWithComplaints(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding recurrence by complaint id: " + recurringComplaintId, e);
        }
        return null;
    }

    public boolean verifyRecurrence(int recurrenceId, String newStatus, int verifiedBy) {
        String sql = "UPDATE recurrence_records SET verification_status = ?, verified_by = ?, verified_at = NOW() WHERE recurrence_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, verifiedBy);
            ps.setInt(3, recurrenceId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verifying recurrence record: " + recurrenceId, e);
        }
        return false;
    }

    public Map<String, Integer> getRecurrenceStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("TOTAL", 0);
        stats.put("PENDING", 0);
        stats.put("CONFIRMED", 0);
        stats.put("REJECTED", 0);

        String sql = "SELECT verification_status, COUNT(*) AS cnt FROM recurrence_records GROUP BY verification_status";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int total = 0;
            while (rs.next()) {
                String st = rs.getString("verification_status");
                int cnt = rs.getInt("cnt");
                total += cnt;
                if (st != null) {
                    stats.put(st.toUpperCase(), cnt);
                }
            }
            stats.put("TOTAL", total);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching recurrence statistics", e);
        }
        return stats;
    }

    private RecurrenceRecord mapRowWithComplaints(ResultSet rs) throws SQLException {
        RecurrenceRecord r = new RecurrenceRecord();
        r.setRecurrenceId(rs.getInt("recurrence_id"));
        r.setOriginalComplaintId(rs.getInt("original_complaint_id"));
        r.setRecurringComplaintId(rs.getInt("recurring_complaint_id"));
        r.setMatchingScore(rs.getBigDecimal("matching_score"));
        r.setMatchingReason(rs.getString("matching_reason"));
        r.setVerificationStatus(rs.getString("verification_status"));

        int vBy = rs.getInt("verified_by");
        if (!rs.wasNull()) r.setVerifiedBy(vBy);

        r.setVerifiedByName(rs.getString("verified_by_name"));
        r.setVerifiedAt(rs.getTimestamp("verified_at"));
        r.setCreatedAt(rs.getTimestamp("created_at"));

        // Build brief original complaint
        Complaint orig = new Complaint();
        orig.setComplaintId(r.getOriginalComplaintId());
        orig.setTitle(rs.getString("orig_title"));
        orig.setArea(rs.getString("orig_area"));
        orig.setCity(rs.getString("orig_city"));
        orig.setStatus(rs.getString("orig_status"));
        orig.setResolvedAt(rs.getTimestamp("orig_resolved_at"));
        orig.setCategoryId(rs.getInt("orig_cat_id"));
        orig.setCategoryName(rs.getString("orig_cat_name"));
        r.setOriginalComplaint(orig);

        // Build brief recurring complaint
        Complaint rec = new Complaint();
        rec.setComplaintId(r.getRecurringComplaintId());
        rec.setTitle(rs.getString("rec_title"));
        rec.setArea(rs.getString("rec_area"));
        rec.setCity(rs.getString("rec_city"));
        rec.setStatus(rs.getString("rec_status"));
        rec.setSubmittedAt(rs.getTimestamp("rec_submitted_at"));
        rec.setCategoryId(rs.getInt("rec_cat_id"));
        rec.setCategoryName(rs.getString("rec_cat_name"));
        rec.setUserName(rs.getString("rec_user_name"));
        r.setRecurringComplaint(rec);

        return r;
    }
}


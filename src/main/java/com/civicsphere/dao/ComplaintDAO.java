package com.civicsphere.dao;

import com.civicsphere.model.Complaint;
import com.civicsphere.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for complaints table.
 */
public class ComplaintDAO {

    private static final Logger LOGGER = Logger.getLogger(ComplaintDAO.class.getName());

    public int createComplaint(Complaint c) {
        String sql = "INSERT INTO complaints (" +
                "user_id, category_id, department_id, parent_complaint_id, " +
                "title, description, area, city, state, landmark, " +
                "latitude, longitude, image_path, status, submitted_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, c.getUserId());
            ps.setInt(2, c.getCategoryId());
            if (c.getDepartmentId() != null && c.getDepartmentId() > 0) {
                ps.setInt(3, c.getDepartmentId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            if (c.getParentComplaintId() != null && c.getParentComplaintId() > 0) {
                ps.setInt(4, c.getParentComplaintId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, c.getTitle());
            ps.setString(6, c.getDescription());
            ps.setString(7, c.getArea());
            ps.setString(8, c.getCity());
            ps.setString(9, c.getState());
            ps.setString(10, c.getLandmark());

            if (c.getLatitude() != null) {
                ps.setBigDecimal(11, c.getLatitude());
            } else {
                ps.setNull(11, Types.DECIMAL);
            }
            if (c.getLongitude() != null) {
                ps.setBigDecimal(12, c.getLongitude());
            } else {
                ps.setNull(12, Types.DECIMAL);
            }

            ps.setString(13, c.getImagePath());
            ps.setString(14, c.getStatus() != null ? c.getStatus() : "SUBMITTED");
            Timestamp now = c.getSubmittedAt() != null ? c.getSubmittedAt() : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(15, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        c.setComplaintId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting complaint: " + c.getTitle(), e);
        }
        return -1;
    }

    public Complaint findById(int complaintId) {
        String sql = "SELECT c.*, " +
                "u.full_name AS user_name, u.email AS user_email, u.phone AS user_phone, " +
                "cat.category_name, " +
                "d.department_name, " +
                "p.title AS parent_title, " +
                "r.matching_score, " +
                "(c.parent_complaint_id IS NOT NULL OR EXISTS(SELECT 1 FROM recurrence_records rr WHERE rr.recurring_complaint_id = c.complaint_id)) AS is_rec " +
                "FROM complaints c " +
                "LEFT JOIN users u ON c.user_id = u.user_id " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "LEFT JOIN departments d ON c.department_id = d.department_id " +
                "LEFT JOIN complaints p ON c.parent_complaint_id = p.complaint_id " +
                "LEFT JOIN recurrence_records r ON r.recurring_complaint_id = c.complaint_id " +
                "WHERE c.complaint_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowWithJoins(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding complaint by id: " + complaintId, e);
        }
        return null;
    }

    public List<Complaint> findByUserId(int userId) {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT c.*, " +
                "u.full_name AS user_name, u.email AS user_email, u.phone AS user_phone, " +
                "cat.category_name, " +
                "d.department_name, " +
                "p.title AS parent_title, " +
                "r.matching_score, " +
                "(c.parent_complaint_id IS NOT NULL OR EXISTS(SELECT 1 FROM recurrence_records rr WHERE rr.recurring_complaint_id = c.complaint_id)) AS is_rec " +
                "FROM complaints c " +
                "LEFT JOIN users u ON c.user_id = u.user_id " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "LEFT JOIN departments d ON c.department_id = d.department_id " +
                "LEFT JOIN complaints p ON c.parent_complaint_id = p.complaint_id " +
                "LEFT JOIN recurrence_records r ON r.recurring_complaint_id = c.complaint_id " +
                "WHERE c.user_id = ? " +
                "ORDER BY c.submitted_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithJoins(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding complaints for user: " + userId, e);
        }
        return list;
    }

    public List<Complaint> findAll(String status, Integer categoryId, String search, String city, String area) {
        List<Complaint> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, " +
                "u.full_name AS user_name, u.email AS user_email, u.phone AS user_phone, " +
                "cat.category_name, " +
                "d.department_name, " +
                "p.title AS parent_title, " +
                "r.matching_score, " +
                "(c.parent_complaint_id IS NOT NULL OR EXISTS(SELECT 1 FROM recurrence_records rr WHERE rr.recurring_complaint_id = c.complaint_id)) AS is_rec " +
                "FROM complaints c " +
                "LEFT JOIN users u ON c.user_id = u.user_id " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "LEFT JOIN departments d ON c.department_id = d.department_id " +
                "LEFT JOIN complaints p ON c.parent_complaint_id = p.complaint_id " +
                "LEFT JOIN recurrence_records r ON r.recurring_complaint_id = c.complaint_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            sql.append(" AND c.status = ?");
            params.add(status.trim());
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND c.category_id = ?");
            params.add(categoryId);
        }
        if (city != null && !city.trim().isEmpty()) {
            sql.append(" AND LOWER(c.city) LIKE LOWER(?)");
            params.add("%" + city.trim() + "%");
        }
        if (area != null && !area.trim().isEmpty()) {
            sql.append(" AND LOWER(c.area) LIKE LOWER(?)");
            params.add("%" + area.trim() + "%");
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (c.complaint_id = ? OR LOWER(c.title) LIKE LOWER(?) OR LOWER(c.description) LIKE LOWER(?) OR LOWER(u.full_name) LIKE LOWER(?))");
            int idSearch = -1;
            try {
                idSearch = Integer.parseInt(search.trim().replace("#", ""));
            } catch (NumberFormatException ignored) {}
            params.add(idSearch);
            params.add("%" + search.trim() + "%");
            params.add("%" + search.trim() + "%");
            params.add("%" + search.trim() + "%");
        }

        sql.append(" ORDER BY c.submitted_at DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithJoins(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying complaints", e);
        }
        return list;
    }

    /**
     * Finds historical complaints with status RESOLVED or CLOSED for recurrence engine evaluation.
     */
    public List<Complaint> findResolvedAndClosedComplaints(int categoryId) {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints " +
                "WHERE category_id = ? AND status IN ('RESOLVED', 'CLOSED') " +
                "ORDER BY resolved_at DESC, submitted_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSimpleRow(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding resolved complaints for category: " + categoryId, e);
        }
        return list;
    }

    public boolean updateStatus(int complaintId, String newStatus, Integer departmentId, Connection conn) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE complaints SET status = ?");
        if (departmentId != null && departmentId > 0) {
            sql.append(", department_id = ?");
        }
        sql.append(" WHERE complaint_id = ?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, newStatus);
            if (departmentId != null && departmentId > 0) {
                ps.setInt(2, departmentId);
                ps.setInt(3, complaintId);
            } else {
                ps.setInt(2, complaintId);
            }
            return ps.executeUpdate() > 0;
        }
    }

    public boolean resolveComplaint(int complaintId, String remarks, String resolutionImagePath, Connection conn) throws SQLException {
        String sql = "UPDATE complaints SET status = 'RESOLVED', resolved_at = NOW(), resolution_remarks = ?, resolution_image_path = ? WHERE complaint_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, remarks);
            ps.setString(2, resolutionImagePath);
            ps.setInt(3, complaintId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean assignDepartment(int complaintId, int departmentId, Connection conn) throws SQLException {
        String sql = "UPDATE complaints SET department_id = ?, status = CASE WHEN status = 'SUBMITTED' THEN 'ASSIGNED' ELSE status END WHERE complaint_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            ps.setInt(2, complaintId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateParentComplaintId(int complaintId, int parentComplaintId) {
        String sql = "UPDATE complaints SET parent_complaint_id = ? WHERE complaint_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parentComplaintId);
            ps.setInt(2, complaintId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error linking parent complaint", e);
        }
        return false;
    }

    public Map<String, Integer> getStatusCounts(Integer userId) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("TOTAL", 0);
        counts.put("SUBMITTED", 0);
        counts.put("UNDER_REVIEW", 0);
        counts.put("ASSIGNED", 0);
        counts.put("IN_PROGRESS", 0);
        counts.put("RESOLVED", 0);
        counts.put("CLOSED", 0);
        counts.put("RECURRING", 0);

        StringBuilder sql = new StringBuilder("SELECT status, COUNT(*) AS cnt FROM complaints");
        if (userId != null) {
            sql.append(" WHERE user_id = ?");
        }
        sql.append(" GROUP BY status");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (userId != null) {
                ps.setInt(1, userId);
            }

            int total = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    total += cnt;
                    if (status != null) {
                        counts.put(status.replace(" ", "_"), cnt);
                    }
                }
            }
            counts.put("TOTAL", total);

            // Recurring count
            StringBuilder recSql = new StringBuilder(
                    "SELECT COUNT(DISTINCT c.complaint_id) FROM complaints c " +
                    "WHERE (c.parent_complaint_id IS NOT NULL OR EXISTS(SELECT 1 FROM recurrence_records rr WHERE rr.recurring_complaint_id = c.complaint_id))"
            );
            if (userId != null) {
                recSql.append(" AND c.user_id = ?");
            }
            try (PreparedStatement psRec = conn.prepareStatement(recSql.toString())) {
                if (userId != null) {
                    psRec.setInt(1, userId);
                }
                try (ResultSet rsRec = psRec.executeQuery()) {
                    if (rsRec.next()) {
                        counts.put("RECURRING", rsRec.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching status counts", e);
        }
        return counts;
    }

    public Map<String, Integer> getCountsByCategory() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT cat.category_name, COUNT(c.complaint_id) AS cnt " +
                "FROM categories cat " +
                "LEFT JOIN complaints c ON cat.category_id = c.category_id " +
                "GROUP BY cat.category_id, cat.category_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                map.put(rs.getString("category_name"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching counts by category", e);
        }
        return map;
    }

    public Map<String, Integer> getCountsByDepartment() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT d.department_name, COUNT(c.complaint_id) AS cnt " +
                "FROM departments d " +
                "LEFT JOIN complaints c ON d.department_id = c.department_id " +
                "GROUP BY d.department_id, d.department_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                map.put(rs.getString("department_name"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching counts by department", e);
        }
        return map;
    }

    public List<Complaint> getRecentComplaints(Integer userId, int limit) {
        List<Complaint> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, u.full_name AS user_name, u.email AS user_email, u.phone AS user_phone, " +
                "cat.category_name, d.department_name, p.title AS parent_title, r.matching_score, " +
                "(c.parent_complaint_id IS NOT NULL OR EXISTS(SELECT 1 FROM recurrence_records rr WHERE rr.recurring_complaint_id = c.complaint_id)) AS is_rec " +
                "FROM complaints c " +
                "LEFT JOIN users u ON c.user_id = u.user_id " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "LEFT JOIN departments d ON c.department_id = d.department_id " +
                "LEFT JOIN complaints p ON c.parent_complaint_id = p.complaint_id " +
                "LEFT JOIN recurrence_records r ON r.recurring_complaint_id = c.complaint_id "
        );

        if (userId != null) {
            sql.append(" WHERE c.user_id = ?");
        }
        sql.append(" ORDER BY c.submitted_at DESC LIMIT ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (userId != null) {
                ps.setInt(idx++, userId);
            }
            ps.setInt(idx, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithJoins(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching recent complaints", e);
        }
        return list;
    }

    private Complaint mapSimpleRow(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setComplaintId(rs.getInt("complaint_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setCategoryId(rs.getInt("category_id"));

        int deptId = rs.getInt("department_id");
        if (!rs.wasNull()) c.setDepartmentId(deptId);

        int parentId = rs.getInt("parent_complaint_id");
        if (!rs.wasNull()) c.setParentComplaintId(parentId);

        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setArea(rs.getString("area"));
        c.setCity(rs.getString("city"));
        c.setState(rs.getString("state"));
        c.setLandmark(rs.getString("landmark"));

        BigDecimal lat = rs.getBigDecimal("latitude");
        if (lat != null) c.setLatitude(lat);
        BigDecimal lon = rs.getBigDecimal("longitude");
        if (lon != null) c.setLongitude(lon);

        c.setImagePath(rs.getString("image_path"));
        c.setStatus(rs.getString("status"));
        c.setSubmittedAt(rs.getTimestamp("submitted_at"));
        c.setResolvedAt(rs.getTimestamp("resolved_at"));
        c.setResolutionRemarks(rs.getString("resolution_remarks"));
        c.setResolutionImagePath(rs.getString("resolution_image_path"));

        return c;
    }

    private Complaint mapRowWithJoins(ResultSet rs) throws SQLException {
        Complaint c = mapSimpleRow(rs);
        c.setUserName(rs.getString("user_name"));
        c.setUserEmail(rs.getString("user_email"));
        c.setUserPhone(rs.getString("user_phone"));
        c.setCategoryName(rs.getString("category_name"));
        c.setDepartmentName(rs.getString("department_name"));
        c.setParentComplaintTitle(rs.getString("parent_title"));
        c.setRecurring(rs.getInt("is_rec") == 1);

        BigDecimal score = rs.getBigDecimal("matching_score");
        if (score != null) {
            c.setRecurrenceScore(score.doubleValue());
        }

        return c;
    }
}


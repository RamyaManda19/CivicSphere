package com.civicsphere.dao;

import com.civicsphere.model.Department;
import com.civicsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for departments table.
 */
public class DepartmentDAO {

    private static final Logger LOGGER = Logger.getLogger(DepartmentDAO.class.getName());

    public List<Department> findAll() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT department_id, department_name, description, contact_email, created_at FROM departments ORDER BY department_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching departments", e);
        }
        return list;
    }

    public Department findById(int departmentId) {
        String sql = "SELECT department_id, department_name, description, contact_email, created_at FROM departments WHERE department_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding department: " + departmentId, e);
        }
        return null;
    }

    public boolean createDepartment(Department department) {
        String sql = "INSERT INTO departments (department_name, description, contact_email) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, department.getDepartmentName());
            ps.setString(2, department.getDescription());
            ps.setString(3, department.getContactEmail());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        department.setDepartmentId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating department", e);
        }
        return false;
    }

    public boolean updateDepartment(Department department) {
        String sql = "UPDATE departments SET department_name = ?, description = ?, contact_email = ? WHERE department_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, department.getDepartmentName());
            ps.setString(2, department.getDescription());
            ps.setString(3, department.getContactEmail());
            ps.setInt(4, department.getDepartmentId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating department", e);
        }
        return false;
    }

    public boolean deleteDepartment(int departmentId) {
        String sql = "DELETE FROM departments WHERE department_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, departmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting department: " + departmentId, e);
        }
        return false;
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDepartmentId(rs.getInt("department_id"));
        d.setDepartmentName(rs.getString("department_name"));
        d.setDescription(rs.getString("description"));
        d.setContactEmail(rs.getString("contact_email"));
        d.setCreatedAt(rs.getTimestamp("created_at"));
        return d;
    }
}


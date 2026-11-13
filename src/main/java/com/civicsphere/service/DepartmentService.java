package com.civicsphere.service;

import com.civicsphere.dao.DepartmentDAO;
import com.civicsphere.model.Department;

import java.util.List;

/**
 * Service managing municipal department records.
 */
public class DepartmentService {

    private final DepartmentDAO departmentDAO;

    public DepartmentService() {
        this.departmentDAO = new DepartmentDAO();
    }

    public DepartmentService(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    public List<Department> getAllDepartments() {
        return departmentDAO.findAll();
    }

    public Department getDepartmentById(int id) {
        return departmentDAO.findById(id);
    }

    public Department createDepartment(String name, String description, String contactEmail) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name is required.");
        }
        Department dept = new Department();
        dept.setDepartmentName(name.trim());
        dept.setDescription(description != null ? description.trim() : "");
        dept.setContactEmail(contactEmail != null ? contactEmail.trim() : "");

        boolean created = departmentDAO.createDepartment(dept);
        if (!created) {
            throw new RuntimeException("Failed to create department.");
        }
        return dept;
    }

    public boolean updateDepartment(int id, String name, String description, String contactEmail) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name is required.");
        }
        Department dept = new Department();
        dept.setDepartmentId(id);
        dept.setDepartmentName(name.trim());
        dept.setDescription(description != null ? description.trim() : "");
        dept.setContactEmail(contactEmail != null ? contactEmail.trim() : "");

        return departmentDAO.updateDepartment(dept);
    }

    public boolean deleteDepartment(int id) {
        return departmentDAO.deleteDepartment(id);
    }
}


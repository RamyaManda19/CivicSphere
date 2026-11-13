package com.civicsphere.controller;

import com.civicsphere.dao.CategoryDAO;
import com.civicsphere.model.Category;
import com.civicsphere.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Controller providing civic problem category definitions.
 */
@WebServlet(name = "CategoryServlet", urlPatterns = {"/api/categories/*"})
public class CategoryServlet extends HttpServlet {

    private CategoryDAO categoryDAO;

    @Override
    public void init() throws ServletException {
        this.categoryDAO = new CategoryDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
            List<Category> list = categoryDAO.findAll();
            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Categories fetched", list);
        } else {
            try {
                int id = Integer.parseInt(pathInfo.substring(1));
                Category cat = categoryDAO.findById(id);
                if (cat == null) {
                    JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Category not found");
                } else {
                    JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Category found", cat);
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID format");
            }
        }
    }
}


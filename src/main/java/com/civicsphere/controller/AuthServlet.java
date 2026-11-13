package com.civicsphere.controller;

import com.civicsphere.model.User;
import com.civicsphere.service.AuthService;
import com.civicsphere.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller handling user registration, login, logout, and session checks.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/me".equals(pathInfo)) {
            handleMe(req, resp);
        } else {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/register".equals(pathInfo)) {
            handleRegister(req, resp);
        } else if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp);
        } else {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonObject body = JsonUtil.fromJson(req.getReader(), JsonObject.class);
            if (body == null) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON payload");
                return;
            }

            String fullName = body.has("fullName") ? body.get("fullName").getAsString() : null;
            String email = body.has("email") ? body.get("email").getAsString() : null;
            String phone = body.has("phone") ? body.get("phone").getAsString() : null;
            String password = body.has("password") ? body.get("password").getAsString() : null;
            String confirmPassword = body.has("confirmPassword") ? body.get("confirmPassword").getAsString() : null;
            String address = body.has("address") ? body.get("address").getAsString() : null;

            User user = authService.registerCitizen(fullName, email, phone, password, confirmPassword, address);

            // Establish session
            HttpSession session = req.getSession(true);
            session.setAttribute("USER_ID", user.getUserId());
            session.setAttribute("USER_ROLE", user.getRole());
            session.setAttribute("USER_NAME", user.getFullName());
            session.setAttribute("USER_EMAIL", user.getEmail());

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("fullName", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            userData.put("role", user.getRole());

            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_CREATED, "Registration successful!", userData);

        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed: " + e.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonObject body = JsonUtil.fromJson(req.getReader(), JsonObject.class);
            if (body == null) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials payload");
                return;
            }

            String email = body.has("email") ? body.get("email").getAsString() : null;
            String password = body.has("password") ? body.get("password").getAsString() : null;

            User user = authService.authenticate(email, password);

            // Create/renew session
            HttpSession session = req.getSession(true);
            session.setAttribute("USER_ID", user.getUserId());
            session.setAttribute("USER_ROLE", user.getRole());
            session.setAttribute("USER_NAME", user.getFullName());
            session.setAttribute("USER_EMAIL", user.getEmail());

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("fullName", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            userData.put("role", user.getRole());
            userData.put("address", user.getAddress());

            JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Login successful", userData);

        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "Logged out successfully", null);
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return;
        }

        int userId = (Integer) session.getAttribute("USER_ID");
        User user = authService.getUserById(userId);
        if (user == null) {
            JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUserId());
        userData.put("fullName", user.getFullName());
        userData.put("email", user.getEmail());
        userData.put("phone", user.getPhone());
        userData.put("role", user.getRole());
        userData.put("address", user.getAddress());

        JsonUtil.sendSuccessResponse(resp, HttpServletResponse.SC_OK, "User profile retrieved", userData);
    }
}

